package cross;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class OrderBook {
    private Gson gson;
    private ConcurrentSkipListMap<Integer, List<LimitOrder>> bidMap;
    private ConcurrentSkipListMap<Integer, List<LimitOrder>> askMap;
    private ConcurrentSkipListMap<Integer, List<LimitOrder>> bidMapLog;
    private ConcurrentSkipListMap<Integer, List<LimitOrder>> askMapLog;
    
    private AtomicInteger marketPrice;

    public OrderBook (Gson gson) {
        this.gson = gson;

        bidMap = loadMapFromJson("bidMap.json", this.gson);
        askMap = loadMapFromJson("askMap.json", this.gson);

        bidMapLog = loadMapFromJson("bidMapLog.json", this.gson);
        askMapLog = loadMapFromJson("askMapLog.json", this.gson);

        updateMap(bidMapLog, bidMap, "bidMap.json");
        updateMap(askMapLog, askMap, "askMap.json");

        emptyJson("bidMapLog.json");
        emptyJson("askMapLog.json");
        
        bidMapLog = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();
        askMapLog = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();

    }

    private void updateMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> mapLog, ConcurrentSkipListMap<Integer, List<LimitOrder>> map, String fileName) {
        for (List<LimitOrder> list : mapLog.values()) {
            for (LimitOrder limitOrder : list) {
                addLimitOrder(limitOrder, map);
            }
        }
        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void emptyJson (String fileName) {
        // Scrivi una hashmap vuota in usersMapLog.json
        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(new ConcurrentSkipListMap<Integer, List<LimitOrder>>(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    

    private static ConcurrentSkipListMap<Integer, List<LimitOrder>> loadMapFromJson(String fileName, Gson gson) {
        ConcurrentSkipListMap<Integer, List<LimitOrder>> Map = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();
        try (FileReader reader = new FileReader(fileName)) {
            Type userMapType = new TypeToken<ConcurrentSkipListMap<Integer, List<LimitOrder>>>(){}.getType();
            Map = gson.fromJson(reader, userMapType);
        } catch (FileNotFoundException e) {
            // File not found, will create a new file
            try (FileWriter writer = new FileWriter(fileName)) {
                gson.toJson(Map, writer);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Map;
    }

    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getAskMap() {
        return askMap;
    }
    
    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getBidMap() {
        return bidMap;
    }
    
    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getAskMapLog() {
        return askMapLog;
    }

    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getBidMapLog() {
        return bidMapLog;
    }

    public void addLimitOrder(LimitOrder limitOrder, ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        map.computeIfAbsent(limitOrder.getPrice(), k -> new LinkedList<>()).add(limitOrder);
    }

    public int getMarketPrice() {
        return marketPrice.get();
    }

    public void setMarketPrice(int price) {
        marketPrice.set(price);
    }
}

// EQUIVALENTE  // da usare se in java 8 desse problemi
    // public void addLimitOrderToMap (ConcurrentSkipListMap<Integer, List<LimitOrder>> map, LimitOrder limitOrder) {
    //     List<LimitOrder> orders = map.get(limitOrder.getPrice());
    //     if (orders == null) {
    //         orders = new LinkedList<>();
    //         map.put(limitOrder.getPrice(), orders);
    //     }
    //     orders.add(limitOrder);
    // }