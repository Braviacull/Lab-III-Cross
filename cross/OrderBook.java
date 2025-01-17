package cross;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

// La classe OrderBook gestisce le mappe degli ordini bid e ask, inclusi gli ordini stop
public class OrderBook {
    private Gson gson;
    private Type mapType; // Tipo della mappa per la deserializzazione JSON
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> bidMap = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> askMap = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> bidMapStop = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> askMapStop = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();

    public OrderBook(Gson gson) {
        this.gson = gson;

        // Crea il tipo della mappa per la deserializzazione JSON
        mapType = new TypeToken<ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>>(){}.getType();

        // Carica le mappe bid e ask dai file JSON
        loadMapFromJson(Costants.BID_MAP_FILE, bidMap);
        loadMapFromJson(Costants.ASK_MAP_FILE, askMap);
        loadMapFromJson(Costants.BID_MAP_STOP_FILE, bidMapStop);
        loadMapFromJson(Costants.ASK_MAP_STOP_FILE, askMapStop);
    }

    // Metodo per ottenere il nome del file JSON associato a una mappa
    public String getJsonFileNameFromMap (ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        if (map == askMap){
            return Costants.ASK_MAP_FILE;
        }
        else if (map == bidMap){
            return Costants.BID_MAP_FILE;
        }
        else if (map == askMapStop){
            return Costants.ASK_MAP_STOP_FILE;
        }
        else if (map == bidMapStop){
            return Costants.BID_MAP_STOP_FILE;
        }
        throw new IllegalArgumentException (map + " not supported");
    }

    // Metodo per ottenere il prezzo di mercato bid
    public int getBidMarketPrice (ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        if (!map.isEmpty()){
            return map.lastKey();
        } else {
            return 0;
        }
    }

    // Metodo per ottenere il prezzo di mercato ask
    public int getAskMarketPrice (ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        if (!map.isEmpty()){
            return map.firstKey();
        } else {
            return Integer.MAX_VALUE; // Rappresenta l'infinito positivo
        }
    }


    public void loadMapFromJson (String fileName, ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        MyUtils.loadMapFromJson(fileName, map, mapType, gson);
    }

    public void updateJson (String fileName, ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        MyUtils.updateJson(fileName, map, gson);
    }

    // Metodo per ottenere la dimensione totale degli ordini in una mappa, USATO PER CAPIRE SE UNA TRANSAZIONE PUÒ TERMINARE OPPURE NO
    public int getSizeFromMap (ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        int size = 0;
        for (int price : map.keySet()) {
            for (Order order : map.get(price)){
                size += order.getSize();
            }
        }
        return size;
    }

    // Metodo per invertire il tipo di ordine (ask <-> bid)
    public String reverseType (String type) {
        switch (type) {
            case Costants.ASK:
                type = Costants.BID;
                break;
            case Costants.BID:
                type = Costants.ASK;
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
        return type;
    }

    // aggiunge un ordine ad una mappa
    public void addOrder(Order order, ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        map.computeIfAbsent(order.getPrice(), _ -> new ConcurrentLinkedQueue<Order>()).add(order); 
    }

    // Metodi getter per ottenere le mappe degli ordini
    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> getAskMap() {
            return askMap;
    }

    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> getBidMap() {
            return bidMap;
    }

    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> getAskMapStop() {
            return askMapStop;
    }

    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> getBidMapStop() {
            return bidMapStop;
    }
}