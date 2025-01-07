package cross;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class OrderBook {
    private Gson gson;
    private ConcurrentSkipListMap<Integer, List<LimitOrder>> bidMap = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();
    private ConcurrentSkipListMap<Integer, List<LimitOrder>> askMap = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();
    private AtomicInteger marketPrice;
    private Type mapType;

    public OrderBook(Gson gson) {
        this.gson = gson;

        mapType = new TypeToken<ConcurrentSkipListMap<Integer, List<LimitOrder>>>(){}.getType();
        // Load bid and ask maps from JSON files
        loadMapFromJson(Costants.BID_MAP_FILE, bidMap);
        loadMapFromJson(Costants.ASK_MAP_FILE, askMap);

        // Load temporary bid and ask maps from JSON files
        ConcurrentSkipListMap<Integer, List<LimitOrder>> bidMapTemp = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();
        ConcurrentSkipListMap<Integer, List<LimitOrder>> askMapTemp = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();
        loadMapFromJson(Costants.BID_MAP_TEMP_FILE, bidMapTemp);
        loadMapFromJson(Costants.ASK_MAP_TEMP_FILE, askMapTemp);

        // Update main maps with temporary maps
        updateLimitOrderMap(bidMap, bidMapTemp);
        updateLimitOrderMap(askMap, askMapTemp);

        // Update main jsons with main maps
        updateJson(Costants.BID_MAP_FILE, bidMap);
        updateJson(Costants.ASK_MAP_FILE, askMap);

        // Clear temporary maps
        updateJson(Costants.BID_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, List<LimitOrder>>());
        updateJson(Costants.ASK_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, List<LimitOrder>>());
    }

    public void loadMapFromJson (String fileName, ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        MyUtils.loadMapFromJson(fileName, map, mapType, gson);
    }

    public void updateJson (String fileName, ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        MyUtils.updateJson(fileName, map, gson);
    }

    // Syncronised: LA SOURCEMAP NON É CONDIVISA E addLimitOrder É SYNCRONISED
    public void updateLimitOrderMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> targetMap, ConcurrentSkipListMap<Integer, List<LimitOrder>> sourceMap) {
        for (List<LimitOrder> list : sourceMap.values()) {
            for (LimitOrder limitOrder : list) {
                addLimitOrder(limitOrder, targetMap);
            }
        }
    }

    // Syncronised
    public void resetOrderBook(String type) throws IOException {
        ConcurrentSkipListMap<Integer, List<LimitOrder>> map = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();
        ConcurrentSkipListMap<Integer, List<LimitOrder>> mapTemp = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();
        switch (type) {
            case Costants.ASK:
                loadMapFromJson(Costants.BID_MAP_TEMP_FILE, mapTemp);
                loadMapFromJson(Costants.BID_MAP_FILE, map);
                updateLimitOrderMap(map, mapTemp);
                setBidMap(map);
                updateOrderBook(type);
                break;
            case Costants.BID:
                loadMapFromJson(Costants.ASK_MAP_TEMP_FILE, mapTemp);
                loadMapFromJson(Costants.ASK_MAP_FILE, map);
                updateLimitOrderMap(map, mapTemp);
                setAskMap(map);
                updateOrderBook(type);
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
    }

    // Syncronised
    public void updateOrderBook(String type) throws IOException {
        switch (type) {
            case Costants.ASK:
                updateJson(Costants.BID_MAP_FILE, getBidMap());
                updateJson(Costants.BID_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, List<LimitOrder>>());
                break;
            case Costants.BID:
                updateJson(Costants.ASK_MAP_FILE, getAskMap());
                updateJson(Costants.ASK_MAP_TEMP_FILE, new ConcurrentSkipListMap<Integer, List<LimitOrder>>());
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
    }

    // da eliminare
    public void printMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        for (Map.Entry<Integer, List<LimitOrder>> entry : map.entrySet()) {
            Integer price = entry.getKey();
            List<LimitOrder> limitOrders = entry.getValue();
            for (LimitOrder limitOrder : limitOrders) {
                System.out.println("Type: " + limitOrder.getType());
                System.out.println("Size: " + limitOrder.getSize());
                System.out.println("Price: " + price);
                System.out.println("");
            }
        }
    }

    // Syncronised
    public synchronized void addLimitOrder(LimitOrder limitOrder, ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        map.computeIfAbsent(limitOrder.getPrice(), k -> new LinkedList<>()).add(limitOrder);
    }

    // Syncronised
    public int getMarketPrice() {
        Sync.markerPriceLock.readLock().lock();
        try {
            return marketPrice.get();
        } finally {
            Sync.markerPriceLock.readLock().unlock();
        }
    }

    // Syncronised
    public void setMarketPrice(int price) {
        Sync.markerPriceLock.writeLock().lock();
        try {
            marketPrice.set(price);
        } finally {
            Sync.markerPriceLock.writeLock().unlock();
        }
    }

    // Syncronised
    public void setAskMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        Sync.askMapLock.writeLock().lock();
        try {
            askMap = map;
        } finally {
            Sync.askMapLock.writeLock().unlock();
        }
    }

    // Syncronised
    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getAskMap() {
        Sync.askMapLock.readLock().lock();
        try {
            return askMap;
        } finally {
            Sync.askMapLock.readLock().unlock();
        }
    }

    // Syncronised
    public void setBidMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        Sync.bidMapLock.writeLock().lock();
        try {
            bidMap = map;
        } finally {
            Sync.bidMapLock.writeLock().unlock();
        }
    }

    // Syncronised
    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getBidMap() {
        Sync.bidMapLock.readLock().lock();
        try {
            return bidMap;
        } finally {
            Sync.bidMapLock.readLock().unlock();
        }
    }
}