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
    
    private AtomicInteger marketPrice;

    public OrderBook (Gson gson) {
        this.gson = gson;
        bidMap = loadMapFromJson("BidMap.json");
        askMap = loadMapFromJson("AskMap.json");
    }

    private ConcurrentSkipListMap<Integer, List<LimitOrder>> loadMapFromJson(String name) {
        ConcurrentSkipListMap<Integer, List<LimitOrder>> Map = new ConcurrentSkipListMap<Integer, List<LimitOrder>>();
        try (FileReader reader = new FileReader(name)) {
            Type userMapType = new TypeToken<ConcurrentSkipListMap<Integer, List<LimitOrder>>>(){}.getType();
            Map = gson.fromJson(reader, userMapType);
        } catch (FileNotFoundException e) {
            // File not found, will create a new file
            try (FileWriter writer = new FileWriter(name)) {
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

    public void addLimitOrder(LimitOrder limitOrder) {
        String type = limitOrder.getType();
        switch (type) {
            case "ask":
                // Aggiungi l'ordine alla mappa askMap
                // * see below
                askMap.computeIfAbsent(limitOrder.getPrice(), k -> new LinkedList<>()).add(limitOrder);
                break;
            case "bid":
                // Aggiungi l'ordine alla mappa bidMap
                // * see below
                bidMap.computeIfAbsent(limitOrder.getPrice(), k -> new LinkedList<>()).add(limitOrder);
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
    }

    public int getMarketPrice() {
        return marketPrice.get();
    }

    public void setMarketPrice(int price) {
        marketPrice.set(price);
    }
}

// EQUIVALENTE *  // da usare se in java 8 desse problemi
    // public void addLimitOrderToMap (ConcurrentSkipListMap<Integer, List<LimitOrder>> map, LimitOrder limitOrder) {
    //     List<LimitOrder> orders = map.get(limitOrder.getPrice());
    //     if (orders == null) {
    //         orders = new LinkedList<>();
    //         map.put(limitOrder.getPrice(), orders);
    //     }
    //     orders.add(limitOrder);
    // }