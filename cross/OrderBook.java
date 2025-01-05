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

    public OrderBook (Gson gson) {
        this.gson = gson;

        bidMap = loadMapFromJson("bidMap.json", this.gson);
        askMap = loadMapFromJson("askMap.json", this.gson);

        ConcurrentSkipListMap<Integer, List<LimitOrder>> bidMapTemp = loadMapFromJson("bidMapTemp.json", this.gson);
        ConcurrentSkipListMap<Integer, List<LimitOrder>> askMapTemp = loadMapFromJson("askMapTemp.json", this.gson);

        updateMap(bidMapTemp, bidMap, "bidMap.json");
        updateMap(askMapTemp, askMap, "askMap.json");

        updateJson(new ConcurrentSkipListMap<Integer, List<LimitOrder>> (),"bidMapTemp.json");
        updateJson(new ConcurrentSkipListMap<Integer, List<LimitOrder>> (), "askMapTemp.json");

    }

    public void updateMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> MapTemp, ConcurrentSkipListMap<Integer, List<LimitOrder>> map, String fileName) {
        for (List<LimitOrder> list : MapTemp.values()) {
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

    public void updateJson(ConcurrentSkipListMap<Integer, List<LimitOrder>> map, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void emptyBidTempANDUpdate () {
        updateJson(bidMap, "bidMap.json");
        updateJson(new ConcurrentSkipListMap<Integer, List<LimitOrder>>(),"bidMapTemp.json");
    }

    public void emptyAskTempANDUpdate () {
        updateJson(askMap, "askMap.json");
        updateJson(new ConcurrentSkipListMap<Integer, List<LimitOrder>>(),"askMapTemp.json");
    }

    public ConcurrentSkipListMap<Integer, List<LimitOrder>> loadMapFromJson(String fileName, Gson gson) {
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

    public void printMap(ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        for (Map.Entry<Integer, List<LimitOrder>> entry : map.entrySet()) {
            Integer price = entry.getKey();
            List<LimitOrder> limitOrders = entry.getValue();
            for (LimitOrder limitOrder : limitOrders) {
                System.out.println("Type: " +limitOrder.getType());
                System.out.println("Size: " +limitOrder.getSize());
                System.out.println("Price: " + price);
            }
        }
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

    public void setAskMap (ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        askMap = map;
    }
    
    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getAskMap() {
        return askMap;
    }

    public void setBidMap (ConcurrentSkipListMap<Integer, List<LimitOrder>> map) {
        bidMap = map;
    }
    
    public ConcurrentSkipListMap<Integer, List<LimitOrder>> getBidMap() {
        return bidMap;
    }
}