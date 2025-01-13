package cross;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class OrderBook {
    private Gson gson;
    private Type mapType;
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> bidMap = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> askMap = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> bidMapStop = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> askMapStop = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();

    public OrderBook(Gson gson) {
        this.gson = gson;

        // create map type for updatejsons and loadmapfromjson
        mapType = new TypeToken<ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>>(){}.getType();

        // Load bid and ask maps from JSON files
        loadMapFromJson(Costants.BID_MAP_FILE, bidMap);
        loadMapFromJson(Costants.ASK_MAP_FILE, askMap);
        loadMapFromJson(Costants.BID_MAP_STOP_FILE, bidMapStop);
        loadMapFromJson(Costants.ASK_MAP_STOP_FILE, askMapStop);
    }

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
        throw new IllegalArgumentException ("map not supported");
    }

    public int getBidMarketPrice (ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        if (!map.isEmpty()){
            return map.lastKey();
        } else {
            return 0;
        }
    }

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

    public int getSizeFromMap (ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map, int limit, String type) {
        int size = 0;
        switch (type) {
            case Costants.ASK:
                for (int price : map.keySet()) {
                    if (limit <= price){
                        for (Order order : map.get(price)){
                            size += order.getSize();
                        }
                    }
                }
                break;
            case Costants.BID:
                for (int price : map.keySet()) {
                    if (price <= limit){
                        for (Order order : map.get(price)){
                            size += order.getSize();
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
        return size;
    }

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

    // Syncronised: la mappa é concorrente
    public void addOrder(Order limitOrder, ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        map.computeIfAbsent(limitOrder.getPrice(), _ -> new ConcurrentLinkedQueue<Order>()).add(limitOrder); 
    }
    // Syncronised
    public void setAskMap(ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        Sync.askMapLock.writeLock().lock();
        try {
            askMap = map;
        } finally {
            Sync.askMapLock.writeLock().unlock();
        }
    }

    // Syncronised
    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> getAskMap() {
        Sync.askMapLock.readLock().lock();
        try {
            return askMap;
        } finally {
            Sync.askMapLock.readLock().unlock();
        }
    }

    // Syncronised
    public void setBidMap(ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        Sync.bidMapLock.writeLock().lock();
        try {
            bidMap = map;
        } finally {
            Sync.bidMapLock.writeLock().unlock();
        }
    }

    // Syncronised
    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> getBidMap() {
        Sync.bidMapLock.readLock().lock();
        try {
            return bidMap;
        } finally {
            Sync.bidMapLock.readLock().unlock();
        }
    }

    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> getAskMapStop() {
        Sync.askMapStopLock.readLock().lock();
        try {
            return askMapStop;
        } finally {
            Sync.askMapStopLock.readLock().unlock();
        }
    }

    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> getBidMapStop() {
        Sync.bidMapStopLock.readLock().lock();
        try {
            return bidMapStop;
        } finally {
            Sync.bidMapStopLock.readLock().unlock();
        }
    }
}