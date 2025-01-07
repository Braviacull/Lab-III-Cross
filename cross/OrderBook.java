package cross;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
    private ConcurrentSkipListMap<Integer, List<LimitOrder>> bidMap;
    private ConcurrentSkipListMap<Integer, List<LimitOrder>> askMap;
    private AtomicInteger marketPrice;

    public OrderBook(Gson gson) {
        this.gson = gson;

        // Load bid and ask maps from JSON files
        bidMap = loadMapFromJson(Costants.BID_MAP_FILE);
        askMap = loadMapFromJson(Costants.ASK_MAP_FILE);

        // Load temporary bid and ask maps from JSON files
        ConcurrentSkipListMap<Integer, List<LimitOrder>> bidMapTemp = loadMapFromJson(Costants.BID_MAP_TEMP_FILE);
        ConcurrentSkipListMap<Integer, List<LimitOrder>> askMapTemp = loadMapFromJson(Costants.ASK_MAP_TEMP_FILE);

        // Update main maps with temporary maps
        updateMap(bidMapTemp, bidMap, Costants.BID_MAP_FILE);
        updateMap(askMapTemp, askMap, Costants.ASK_MAP_FILE);

        // Clear temporary maps
        updateJson(new ConcurrentSkipListMap<>(), Costants.BID_MAP_TEMP_FILE);
        updateJson(new ConcurrentSkipListMap<>(), Costants.ASK_MAP_TEMP_FILE);
    }

    // Syncronised: LA MAPTEMP NON É CONDIVISA E addLimitOrder É SYNCRONISED
    public void updateMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> mapTemp, ConcurrentSkipListMap<Integer, List<LimitOrder>> map, String fileName) {
        for (List<LimitOrder> list : mapTemp.values()) {
            for (LimitOrder limitOrder : list) {
                addLimitOrder(limitOrder, map);
            }
        }
        updateJson(map, fileName);
    }

    // Syncronised
    public void updateJson(ConcurrentSkipListMap<Integer, List<LimitOrder>> map, String fileName) {
        Sync.safeWriteStarts(fileName);
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write the map to the JSON file
            gson.toJson(map, writer);
        } catch (IOException e) {
            System.err.println("Error updating JSON file " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            Sync.safeWriteEnds(fileName);
        }
    }

    // Syncronised
    public ConcurrentSkipListMap<Integer, List<LimitOrder>> loadMapFromJson(String fileName) {
        ConcurrentSkipListMap<Integer, List<LimitOrder>> map = new ConcurrentSkipListMap<>();
        Sync.safeReadStarts(fileName);
        try (FileReader reader = new FileReader(fileName)) {
            // Load the map from the JSON file
            Type mapType = new TypeToken<ConcurrentSkipListMap<Integer, List<LimitOrder>>>(){}.getType();
            map = gson.fromJson(reader, mapType);
        } catch (FileNotFoundException e) {
            // File not found, create a new file
            updateJson(new ConcurrentSkipListMap<Integer, List<LimitOrder>> (), fileName);
        } catch (IOException e) {
            System.err.println("Error loading JSON file " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            Sync.safeReadEnds(fileName);
        }
        return map;
    }

    // Syncronised
    public void resetOrderBook(String type) throws IOException {
        ConcurrentSkipListMap<Integer, List<LimitOrder>> map;
        ConcurrentSkipListMap<Integer, List<LimitOrder>> mapTemp;
        switch (type) {
            case Costants.ASK:
                mapTemp = loadMapFromJson(Costants.BID_MAP_TEMP_FILE);
                map = loadMapFromJson(Costants.BID_MAP_FILE);
                updateMap(mapTemp, map, type);
                setBidMap(map);
                updateOrderBook(type);
                break;
            case Costants.BID:
                mapTemp = loadMapFromJson(Costants.ASK_MAP_TEMP_FILE);
                map = loadMapFromJson(Costants.ASK_MAP_FILE);
                updateMap(mapTemp, map, type);
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
                updateJson(getBidMap(), Costants.BID_MAP_FILE);
                updateJson(new ConcurrentSkipListMap<>(), Costants.BID_MAP_TEMP_FILE);
                break;
            case Costants.BID:
                updateJson(getAskMap(), Costants.ASK_MAP_FILE);
                updateJson(new ConcurrentSkipListMap<>(), Costants.ASK_MAP_TEMP_FILE);
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