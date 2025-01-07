package cross;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class OrderBook {
    private Gson gson;
    private ConcurrentSkipListMap<Integer, List<Order>> bidMap = new ConcurrentSkipListMap<Integer, List<Order>>();
    private ConcurrentSkipListMap<Integer, List<Order>> askMap = new ConcurrentSkipListMap<Integer, List<Order>>();
    private ConcurrentSkipListMap<Integer, List<Order>> bidMapStop = new ConcurrentSkipListMap<Integer, List<Order>>();
    private ConcurrentSkipListMap<Integer, List<Order>> askMapStop = new ConcurrentSkipListMap<Integer, List<Order>>();
    private Type mapType;

    public OrderBook(Gson gson) {
        this.gson = gson;

        // create map type for updatejsons and loadmapfromjson
        mapType = new TypeToken<ConcurrentSkipListMap<Integer, List<Order>>>(){}.getType();

        // Load bid and ask maps from JSON files
        loadMapFromJson(Costants.BID_MAP_FILE, bidMap);
        loadMapFromJson(Costants.ASK_MAP_FILE, askMap);
        loadMapFromJson(Costants.BID_MAP_STOP_FILE, bidMapStop);
        loadMapFromJson(Costants.ASK_MAP_STOP_FILE, askMapStop);

        // Load temporary bid and ask maps from JSON files
        ConcurrentSkipListMap<Integer, List<Order>> bidMapTemp = new ConcurrentSkipListMap<Integer, List<Order>>();
        ConcurrentSkipListMap<Integer, List<Order>> askMapTemp = new ConcurrentSkipListMap<Integer, List<Order>>();
        loadMapFromJson(Costants.BID_MAP_TEMP_FILE, bidMapTemp);
        loadMapFromJson(Costants.ASK_MAP_TEMP_FILE, askMapTemp);
        ConcurrentSkipListMap<Integer, List<Order>> bidMapTempStop = new ConcurrentSkipListMap<Integer, List<Order>>();
        ConcurrentSkipListMap<Integer, List<Order>> askMapTempStop = new ConcurrentSkipListMap<Integer, List<Order>>();
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
        updateJson(Costants.BID_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, List<Order>>());
        updateJson(Costants.ASK_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, List<Order>>());
        updateJson(Costants.BID_MAP_TEMP_STOP_FILE, new ConcurrentSkipListMap<Integer, List<Order>>());
        updateJson(Costants.ASK_MAP_TEMP_STOP_FILE, new ConcurrentSkipListMap<Integer, List<Order>>());
    }

    public int getBidMarketPrice () {
        return bidMap.lastKey();
    }

    public int getAskMarketPrice () {
        return askMap.firstKey();
    }

    public void loadMapFromJson (String fileName, ConcurrentSkipListMap<Integer, List<Order>> map) {
        MyUtils.loadMapFromJson(fileName, map, mapType, gson);
    }

    public void updateJson (String fileName, ConcurrentSkipListMap<Integer, List<Order>> map) {
        MyUtils.updateJson(fileName, map, gson);
    }

    // Syncronised: LA SOURCEMAP NON É CONDIVISA E addLimitOrder É SYNCRONISED
    public void updateOrderMap(ConcurrentSkipListMap<Integer, List<Order>> targetMap, ConcurrentSkipListMap<Integer, List<Order>> sourceMap) {
        for (List<Order> list : sourceMap.values()) {
            for (Order limitOrder : list) {
                addOrder(limitOrder, targetMap);
            }
        }
    }


    // Syncronised // NOT FOR STOP ORDERS
    public void resetOrderBook(String type) throws IOException {
        ConcurrentSkipListMap<Integer, List<Order>> map = new ConcurrentSkipListMap<Integer, List<Order>>();
        ConcurrentSkipListMap<Integer, List<Order>> mapTemp = new ConcurrentSkipListMap<Integer, List<Order>>();
        switch (type) {
            case Costants.ASK:
                loadMapFromJson(Costants.BID_MAP_TEMP_FILE, mapTemp);
                loadMapFromJson(Costants.BID_MAP_FILE, map);
                updateOrderMap(map, mapTemp);
                setBidMap(map);
                updateOrderBook(type);
                break;
            case Costants.BID:
                loadMapFromJson(Costants.ASK_MAP_TEMP_FILE, mapTemp);
                loadMapFromJson(Costants.ASK_MAP_FILE, map);
                updateOrderMap(map, mapTemp);
                setAskMap(map);
                updateOrderBook(type);
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
    }

    // Syncronised // NOT FOR STOP ORDERS
    public void updateOrderBook(String type) throws IOException {
        switch (type) {
            case Costants.ASK:
                updateJson(Costants.BID_MAP_FILE, getBidMap());
                updateJson(Costants.BID_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, List<Order>>());
                break;
            case Costants.BID:
                updateJson(Costants.ASK_MAP_FILE, getAskMap());
                updateJson(Costants.ASK_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, List<Order>>());
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
    }

    // da eliminare
    public void printMap(ConcurrentSkipListMap<Integer, List<Order>> map) {
        for (Map.Entry<Integer, List<Order>> entry : map.entrySet()) {
            Integer price = entry.getKey();
            List<Order> limitOrders = entry.getValue();
            for (Order limitOrder : limitOrders) {
                System.out.println("Type: " + limitOrder.getType());
                System.out.println("Size: " + limitOrder.getSize());
                System.out.println("Price: " + price);
                System.out.println("");
            }
        }
    }

    // Syncronised
    public synchronized void addOrder(Order limitOrder, ConcurrentSkipListMap<Integer, List<Order>> map) {
        map.computeIfAbsent(limitOrder.getPrice(), k -> new LinkedList<>()).add(limitOrder);
    }

    // Syncronised
    public void setAskMap(ConcurrentSkipListMap<Integer, List<Order>> map) {
        Sync.askMapLock.writeLock().lock();
        try {
            askMap = map;
        } finally {
            Sync.askMapLock.writeLock().unlock();
        }
    }

    // Syncronised
    public ConcurrentSkipListMap<Integer, List<Order>> getAskMap() {
        Sync.askMapLock.readLock().lock();
        try {
            return askMap;
        } finally {
            Sync.askMapLock.readLock().unlock();
        }
    }

    // Syncronised
    public void setBidMap(ConcurrentSkipListMap<Integer, List<Order>> map) {
        Sync.bidMapLock.writeLock().lock();
        try {
            bidMap = map;
        } finally {
            Sync.bidMapLock.writeLock().unlock();
        }
    }

    // Syncronised
    public ConcurrentSkipListMap<Integer, List<Order>> getBidMap() {
        Sync.bidMapLock.readLock().lock();
        try {
            return bidMap;
        } finally {
            Sync.bidMapLock.readLock().unlock();
        }
    }
}