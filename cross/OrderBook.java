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

import com.google.gson.*;
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

    public void updateMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> mapTemp, ConcurrentSkipListMap<Integer, List<LimitOrder>> map, String fileName) {
        for (List<LimitOrder> list : mapTemp.values()) {
            for (LimitOrder limitOrder : list) {
                addLimitOrder(limitOrder, map);
            }
        }
        updateJson(map, fileName);
    }

    public void updateJson(ConcurrentSkipListMap<Integer, List<LimitOrder>> map, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write the map to the JSON file
            gson.toJson(map, writer);
        } catch (IOException e) {
            System.err.println("Error updating JSON file " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void emptyBidTempANDupdate() {
        // Update bid map JSON file and clear temporary bid map
        updateJson(bidMap, Costants.BID_MAP_FILE);
        updateJson(new ConcurrentSkipListMap<>(), Costants.BID_MAP_TEMP_FILE);
    }

    public void emptyAskTempANDupdate() {
        // Update ask map JSON file and clear temporary ask map
        updateJson(askMap, Costants.ASK_MAP_FILE);
        updateJson(new ConcurrentSkipListMap<>(), Costants.ASK_MAP_TEMP_FILE);
    }

    public ConcurrentSkipListMap<Integer, List<LimitOrder>> loadMapFromJson(String fileName) {
        ConcurrentSkipListMap<Integer, List<LimitOrder>> map = new ConcurrentSkipListMap<>();
        try (FileReader reader = new FileReader(fileName)) {
            // Load the map from the JSON file
            Type mapType = new TypeToken<ConcurrentSkipListMap<Integer, List<LimitOrder>>>(){}.getType();
            map = gson.fromJson(reader, mapType);
        } catch (FileNotFoundException e) {
            // File not found, create a new file
            try (FileWriter writer = new FileWriter(fileName)) {
                gson.toJson(map, writer);
            } catch (IOException ex) {
                System.err.println("Error creating JSON file " + fileName + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("Error loading JSON file " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return map;
    }

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

    public void addLimitOrder(LimitOrder limitOrder, ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        // Add limit order to the map
        map.computeIfAbsent(limitOrder.getPrice(), k -> new LinkedList<>()).add(limitOrder);
    }

    public int getMarketPrice() {
        return marketPrice.get();
    }

    public void setMarketPrice(int price) {
        marketPrice.set(price);
    }

    public void setAskMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        askMap = map;
    }

    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getAskMap() {
        return askMap;
    }

    public void setBidMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        bidMap = map;
    }

    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getBidMap() {
        return bidMap;
    }
}