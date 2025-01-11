package cross;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class OrderBook {
    private Gson gson;
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> bidMap = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> askMap = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> bidMapStop = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> askMapStop = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
    private Type mapType;

    public OrderBook(Gson gson) {
        this.gson = gson;

        // create map type for updatejsons and loadmapfromjson
        mapType = new TypeToken<ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>>(){}.getType();

        // Load bid and ask maps from JSON files
        loadMapFromJson(Costants.BID_MAP_FILE, bidMap);
        loadMapFromJson(Costants.ASK_MAP_FILE, askMap);
        loadMapFromJson(Costants.BID_MAP_STOP_FILE, bidMapStop);
        loadMapFromJson(Costants.ASK_MAP_STOP_FILE, askMapStop);

        // Load temporary bid and ask maps from JSON files
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> bidMapTemp = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> askMapTemp = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
        loadMapFromJson(Costants.BID_MAP_TEMP_FILE, bidMapTemp);
        loadMapFromJson(Costants.ASK_MAP_TEMP_FILE, askMapTemp);
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> bidMapTempStop = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> askMapTempStop = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
        loadMapFromJson(Costants.BID_MAP_TEMP_STOP_FILE, bidMapTempStop);
        loadMapFromJson(Costants.ASK_MAP_TEMP_STOP_FILE, askMapTempStop);

        // Update main maps with temporary maps
        updateOrderMap(bidMap, bidMapTemp);
        updateOrderMap(askMap, askMapTemp);
        updateOrderMap(bidMapStop, bidMapTempStop);
        updateOrderMap(askMapStop, askMapTempStop);

        // Update main jsons with main maps
        updateJson(Costants.BID_MAP_FILE, bidMap);
        updateJson(Costants.ASK_MAP_FILE, askMap);
        updateJson(Costants.BID_MAP_STOP_FILE, bidMapStop);
        updateJson(Costants.ASK_MAP_STOP_FILE, askMapStop);

        // Clear temporary json maps
        updateJson(Costants.BID_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>());
        updateJson(Costants.ASK_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>());
        updateJson(Costants.BID_MAP_TEMP_STOP_FILE, new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>());
        updateJson(Costants.ASK_MAP_TEMP_STOP_FILE, new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>());
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

    // Syncronised: LA SOURCEMAP NON É CONDIVISA E addLimitOrder É SYNCRONISED
    public void updateOrderMap(ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> targetMap, ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> sourceMap) {
        for (ConcurrentLinkedQueue<Order> list : sourceMap.values()) {
            for (Order limitOrder : list) {
                addOrder(limitOrder, targetMap);
            }
        }
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

    // Syncronised // NOT FOR STOP ORDERS
    public void resetOrderBook(String type){
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> mapTemp = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
        switch (type) {
            case Costants.ASK:
                loadMapFromJson(Costants.ASK_MAP_TEMP_FILE, mapTemp);
                loadMapFromJson(Costants.ASK_MAP_FILE, map);
                updateOrderMap(map, mapTemp);
                setAskMap(map);
                updateOrderBook(type);
                break;
            case Costants.BID:
                loadMapFromJson(Costants.BID_MAP_TEMP_FILE, mapTemp);
                loadMapFromJson(Costants.BID_MAP_FILE, map);
                updateOrderMap(map, mapTemp);
                setBidMap(map);
                updateOrderBook(type);
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
    }

    // Syncronised // NOT FOR STOP ORDERS
    public void updateOrderBook(String type){
        switch (type) {
            case Costants.ASK:
                updateJson(Costants.ASK_MAP_FILE, getAskMap());
                updateJson(Costants.ASK_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>());
                break;
            case Costants.BID:
                updateJson(Costants.BID_MAP_FILE, getBidMap());
                updateJson(Costants.BID_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>());
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
    }

    // Syncronised: la mappa é concorrente
    public void addOrder(Order limitOrder, ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        map.computeIfAbsent(limitOrder.getPrice(), k -> new ConcurrentLinkedQueue<Order>()).add(limitOrder);
    }

    // Syncronised: chiama solamente metodi syncronised
    public void addOrderToMapAndUpdateJson (String fileName, Order order) {
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> temp = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>();
        loadMapFromJson(fileName, temp);
        addOrder(order, temp);
        updateJson(fileName, temp);
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