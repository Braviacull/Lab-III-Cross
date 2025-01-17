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

    public static <K,V> void updateJson(String filename, Map<K, V> map, Gson gson) {
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(map, writer); // Save user map to JSON file
        } catch (IOException e) {
            System.err.println("Error updating map to " + filename + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
        }
    }

    public static <K,V> void loadMapFromJson(String filename, Map<K, V> map, Type type, Gson gson) {
        try (FileReader reader = new FileReader(filename)) {
            Map<K, V> mapTemp = gson.fromJson(reader, type); // Load user map from JSON file
            replaceMapContent(map, mapTemp);
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
                System.err.println("Error receiving order ID: " + e.getMessage());
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

    public synchronized static void sendNotification(IpPort ipPort, Notification notification, Gson gson) {
        try {
            if (ipPort != null) {
                InetAddress ipAddress = ipPort.getIpAddress();
                int port = ipPort.getPort();
        
                String json = gson.toJson(notification);
        
                DatagramSocket ds = new DatagramSocket();
        
                byte[] data = json.getBytes("UTF-8"); // Convert the JSON string to a byte array
        
                DatagramPacket dp = new DatagramPacket(data, data.length, ipAddress, port); // Create the datagram packet
        
                ds.send(dp); // Send the datagram packet
        
                ds.close(); // Close the DatagramSocket
            }
            else {
                System.out.println("User not reachable. Notification lost");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized int transaction(int size, int limit, String type, OrderBook orderBook, ConcurrentHashMap<String, IpPort> userIpPortMap, Gson gson) {
        if (!Costants.ASK.equals(type) && !Costants.BID.equals(type)) {
            throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }

        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = Costants.ASK.equals(type) ? orderBook.getBidMap() : orderBook.getAskMap();
        if (map.isEmpty() || size > orderBook.getSizeFromMap(map, limit, type)) {
            return size;
        }

        Integer price = Costants.ASK.equals(type) ? orderBook.getBidMarketPrice(orderBook.getBidMap()) : orderBook.getAskMarketPrice(orderBook.getAskMap()); // get market price
        while (size > 0 && price != null) {
            ConcurrentLinkedQueue<Order> queue = map.get(price);
            if (queue != null) {
                synchronized (queue) {
                    Iterator<Order> iterator = queue.iterator();
                    while (iterator.hasNext() && size > 0) {
                        Order order = iterator.next();
                        if (size >= order.getSize()) {
                            size -= order.getSize();
                            iterator.remove();
                            Trade trade = new Trade(order.getId(), order.getType(), Costants.LIMIT, order.getSize(), order.getPrice(), (int) Instant.now().getEpochSecond());
                            sendNotification(userIpPortMap.get(order.getUsername()), new Notification(trade), gson);
                        } else {
                            order.setSize(order.getSize() - size);
                            size = 0;
                        }
        
                        if (queue.isEmpty()) {
                            map.remove(price);
                        }
        
                        if (size == 0) {
                            break;
                        }
                        else if (size < 0) {
                            throw new IllegalArgumentException("Size cannot be negative: " + size);
                        }
                    }
                }
                price = Costants.ASK.equals(type) ? map.lowerKey(price) : map.higherKey(price);
            }
        }

        if (size != 0) {
            throw new IllegalStateException("size must be 0 at this point");
        }
        
        return size;
    }

    // public static void printMap (ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
    //     if (map.keySet().isEmpty()) {
    //         System.out.println("empty map");
    //     }
    //     for (int key : map.keySet()) {
    //         ConcurrentLinkedQueue<Order> queue = map.get(key);
    //         for (Order order : queue) {
    //             int orderId = order.getId();
    //             System.out.println("orderId = " + orderId);
    //             String type = order.getType();
    //             System.out.println("type = " + type);
    //             int size = order.getSize();
    //             System.out.println("size = " + size);
    //             int price = order.getPrice();
    //             System.out.println("price = " + price);
    //             String username = order.getUsername();
    //             System.out.println("username = " + username + "\n");
    //         }
    //     }
    // }

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

    public static Bools searchAndDeleteOrderById(int idToDelete, ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map, String username) {
        Bools res = new Bools(false, false);
        for (Map.Entry<Integer, ConcurrentLinkedQueue<Order>> entry : map.entrySet()) {
            ConcurrentLinkedQueue<Order> queue = entry.getValue();
            if (queue != null) {
                synchronized (queue) {
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
        }
        return res; // non trovato e non eliminato
    }
}
