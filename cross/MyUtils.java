package cross;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;

import com.google.gson.*;

public class MyUtils {

    public static <K,V> void replaceMapContent(Map<K, V> targetMap, Map<K, V> sourceMap) {
        targetMap.clear(); // Svuota la mappa target
        targetMap.putAll(sourceMap); // Copia il contenuto della mappa source nella mappa target
    }

    // metodo per persistere il contenuto di una mappa su un file json (viene fatto periodicamente o alla chiusura del server)
    // se non trova il file specificato, lo crea
    public static <K,V> void updateJson(String filename, Map<K, V> map, Gson gson) {
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(map, writer); // Save user map to JSON file
        } catch (IOException e) {
            System.err.println("Error updating map to " + filename + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
        }
    }

    // metodo per caricare una mappa dal file json, usato solo nell'inizializzazione ad inizio programma (SINGLE THREAD)
    public static <K,V> void loadMapFromJson(String filename, Map<K, V> map, Type type, Gson gson) {
        try (FileReader reader = new FileReader(filename)) {
            Map<K, V> mapTemp = gson.fromJson(reader, type); // Load user map from JSON file
            replaceMapContent(map, mapTemp); // Non posso assegnare direttamente il valore alla mappa perché Java passa una copia al riferimento per gli oggetti
        } catch (FileNotFoundException e) {
            System.out.println(filename + " not found, creating a new file.");
            updateJson(filename, map, gson); // Create new JSON file if not found
        } catch (IOException e) {
            System.err.println("Error loading map from " + filename + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendLine (String line, DataOutputStream out, AtomicBoolean isServerOnline) {
        try {
            out.writeUTF(line);
        } catch (IOException e) {
            if (isServerOnline.get()){
                System.err.println("Error sending line: " + e.getMessage());
                e.printStackTrace();
            } else {
            }
        }
    }

    public static void sendOrderId(int id, DataOutputStream out) {
        try {
            out.writeInt(id);
        } catch (IOException e) {
            System.err.println("Error sending Order ID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendJson(String json, DataOutputStream out) {
        try {
            out.writeUTF(json);
        } catch (IOException e) {
            System.err.println("Error sending String: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String receiveJson(DataInputStream in) {
        try {
            return in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
            return null; // indica un errore
        }
    }

    // Metodo sincronizzato per inviare una notifica a un indirizzo IP e porta specificati
    public synchronized static void sendNotification(IpPort ipPort, Notification notification, Gson gson) {
        try {
            if (ipPort != null) {
                // Ottiene l'indirizzo IP e la porta dall'oggetto IpPort
                InetAddress ipAddress = ipPort.getIpAddress();
                int port = ipPort.getPort();
        
                // Converte l'oggetto Notification in una stringa JSON
                String json = gson.toJson(notification);
        
                // Crea un DatagramSocket per inviare il pacchetto
                DatagramSocket ds = new DatagramSocket();
        
                // Converte la stringa JSON in un array di byte
                byte[] data = json.getBytes("UTF-8");
        
                // Crea il pacchetto datagram con i dati, la lunghezza, l'indirizzo IP e la porta
                DatagramPacket dp = new DatagramPacket(data, data.length, ipAddress, port);
        
                // Invia il pacchetto datagram
                ds.send(dp);
        
                // Chiude il DatagramSocket
                ds.close();
            } else {
                // Se l'oggetto IpPort è null, stampa un messaggio di errore
                System.out.println("User not reachable. Notification lost");
            }
        } catch (IOException e) {
            // Gestisce eventuali eccezioni di I/O
            e.printStackTrace();
        }
    }

    // metodo usato per limitTransaction (in base al limit price e al prezzo raggiunto, capisce se continuare a scorrere la mappa)
    public static boolean limitCondition (Order limitOrder, Integer price) {
        switch (limitOrder.getType()) {
            case Costants.ASK:
                return limitOrder.getPrice() <= price; // sto vendendo ad un prezzo maggiore o uguale di quello che chiedo 
            case Costants.BID:
                return limitOrder.getPrice() >= price; // sto comprando ad un prezzo minore o uguale di quello che chiedo
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
    }

    // metodo per eseguire una transazione (anche incompleta) quando si gestisce un l'arrivo di una richiesta per la gestione di un limit order
    public static int limitTransaction(Order limitOrder, OrderBook orderBook, ConcurrentHashMap<String, IpPort> userIpPortMap, Gson gson) {
        String type = limitOrder.getType();
        int limitOrderSize = limitOrder.getSize();
        if (!Costants.ASK.equals(type) && !Costants.BID.equals(type)) {
            throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }

        // Ottiene la mappa appropriata in base al tipo di ordine
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = Costants.ASK.equals(type) ? orderBook.getBidMap() : orderBook.getAskMap();
        if (map.isEmpty()) return limitOrderSize;

        synchronized (map) { // sincronizza l'accesso alla mappa
            // Ottiene il prezzo di mercato in base al tipo di ordine
            Integer price = Costants.ASK.equals(type) ? orderBook.getBidMarketPrice(orderBook.getBidMap()) : orderBook.getAskMarketPrice(orderBook.getAskMap());
            
            while (limitOrderSize > 0) {
                // Decide se continuare
                if (price == null) break;
                if (!limitCondition(limitOrder, price)) break;
    
                // Ottiene la coda di ordini al prezzo che ha rispettato la limitCondition
                ConcurrentLinkedQueue<Order> queue = map.get(price);
                Iterator<Order> iterator = queue.iterator();
                while (iterator.hasNext() && limitOrderSize > 0) { // controllo la coda
                    Order order = iterator.next();
                    if (limitOrderSize >= order.getSize()) {
                        // ordine analizzato consumato completamente
                        limitOrderSize -= order.getSize();
                        iterator.remove();
                        Trade trade = new Trade(order.getId(), order.getType(), Costants.LIMIT, order.getSize(), order.getPrice(), (int) Instant.now().getEpochSecond());
                        sendNotification(userIpPortMap.get(order.getUsername()), new Notification(trade), gson);
                    } else {
                        // Ordine analizzato più grande di quello che serve, il limitOrder non verrà aggiunto all'orderbook perché é già stato consumato
                        order.setSize(order.getSize() - limitOrderSize);
                        limitOrderSize = 0;
                    }
    
                    // Rimuove il prezzo dalla mappa se la coda è vuota
                    if (queue.isEmpty()) {
                        map.remove(price);
                    }
    
                    // decide se continuare a scorrere la coda
                    if (limitOrderSize == 0) {
                        break;
                    }
                    else if (limitOrderSize < 0) {
                        throw new IllegalArgumentException("Size cannot be negative: " + limitOrderSize);
                    }
                }
                // ottiene il prezzo successivo in base al tipo di ordine
                price = Costants.ASK.equals(type) ? map.lowerKey(price) : map.higherKey(price);
            }
        }
        return limitOrderSize; // Ritorna la dimensione rimanente del limit order
    }


    // Metodo per eseguire una transazione completamente o non iniziarla neanche
    public static int transaction(int size, int limit, String type, String orderType, OrderBook orderBook, ConcurrentHashMap<String, IpPort> userIpPortMap, Gson gson) {
        if (!Costants.ASK.equals(type) && !Costants.BID.equals(type)) {
            throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }

        // Ottiene la mappa appropriata in base al tipo di ordine
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = Costants.ASK.equals(type) ? orderBook.getBidMap() : orderBook.getAskMap();
        if (map.isEmpty()) return size;

        synchronized (map) { // una volta che ho deciso se la transazione andrà a termine con il prossimo if, nessuno deve toccare la mappa
            if (size > orderBook.getSizeFromMap(map)) return size; // non é possibile completare l'ordine
            // Ottieni il prezzo di mercato
            Integer price = Costants.ASK.equals(type) ? orderBook.getBidMarketPrice(orderBook.getBidMap()) : orderBook.getAskMarketPrice(orderBook.getAskMap());
            while (size > 0 && price != null) {
                // Ottiene la coda di ordini al prezzo corrente
                ConcurrentLinkedQueue<Order> queue = map.get(price);
                Iterator<Order> iterator = queue.iterator();
                while (iterator.hasNext() && size > 0) {
                    Order order = iterator.next();
                    if (size >= order.getSize()) {
                        // Se la dimensione dell'ordine è maggiore o uguale alla dimensione dell'ordine corrente
                        size -= order.getSize();
                        iterator.remove();
                        Trade trade = new Trade(order.getId(), order.getType(), Costants.LIMIT, order.getSize(), order.getPrice(), (int) Instant.now().getEpochSecond());
                        sendNotification(userIpPortMap.get(order.getUsername()), new Notification(trade), gson);
                    } else {
                        // Se la dimensione dell'ordine è minore della dimensione dell'ordine corrente
                        order.setSize(order.getSize() - size);
                        size = 0;
                    }
    
                    // Rimuove il prezzo dalla mappa se la coda è vuota
                    if (queue.isEmpty()) {
                        map.remove(price);
                    }
    
                    // Verifica se la dimensione dell'ordine è zero o negativa
                    if (size == 0) {
                        break;
                    }
                    else if (size < 0) {
                        throw new IllegalArgumentException("Size cannot be negative: " + size);
                    }
                }
                // Ottiene il prezzo successivo in base al tipo di ordine
                price = Costants.ASK.equals(type) ? map.lowerKey(price) : map.higherKey(price);
            }
        }

        if (size != 0 && !orderType.equals(Costants.LIMIT)) {
            throw new IllegalStateException("size must be 0 at this point"); // avevo calcolato che la size disponibile era sufficiente
        }
        return size;// Ritorna la dimensione rimanente dell'ordine
    }

    // Descrive lo stato di una cancellazione di un ordine
    public static class Bools {
        private boolean deleted;
        private boolean found;

        public Bools (boolean deleted, boolean found) {
            this.deleted = deleted;
            this.found = found;
        }
        
        public boolean isDeleted () {
            return deleted;
        }
        public boolean isFound () {
            return found;
        }
        public void setDeleted (boolean deleted) {
            this.deleted = deleted;
        }
        public void setFound (boolean found) {
            this.found = found;
        }
    }

    // cerca un ordine in una mappa e se lo trova e lo username corrisponde, allora lo elimina
    public static Bools searchAndDeleteOrderById(int idToDelete, ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map, String username) {
        Bools res = new Bools(false, false);
        synchronized (map) { // devo sincronizzare perché potrebbe eliminare un elemento della mappa
            for (Map.Entry<Integer, ConcurrentLinkedQueue<Order>> entry : map.entrySet()) {
                ConcurrentLinkedQueue<Order> queue = entry.getValue();
                Iterator<Order> iterator = queue.iterator();
                while (iterator.hasNext()) {
                    Order order = iterator.next();
                    int id = order.getId();
                    if (id == idToDelete) {
                        if (!order.getUsername().equals(username)) {
                            res.setFound(true); // trovato
                            res.setDeleted(false); // ma username non corretto
                            return res;
                        }
                        iterator.remove();
                        if (queue.isEmpty()) {
                            map.remove(entry.getKey());
                        }
                        res.setFound(true); // trovato
                        res.setDeleted(true); // ed eliminato
                        return res;
                    }
                }
            }
        }
        return res; // non trovato e non eliminato
    }
}
