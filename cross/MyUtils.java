package cross;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.lang.reflect.Type;

import com.google.gson.*;

public class MyUtils {

    public static <K,V> void replaceMapContent(Map<K, V> targetMap, Map<K, V> sourceMap) {
        targetMap.clear(); // Svuota la mappa target
        targetMap.putAll(sourceMap); // Copia il contenuto della mappa source nella mappa target
    }

    public static <K,V> void updateJson(String filename, Map<K, V> map, Gson gson) {
        Sync.safeWriteStarts(filename);
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(map, writer); // Save user map to JSON file
        } catch (IOException e) {
            System.err.println("Error updating map to " + filename + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            Sync.safeWriteEnds(filename);
        }
    }

    public static <K,V> void loadMapFromJson(String filename, Map<K, V> map, Type type, Gson gson) {
        Sync.safeReadStarts(filename);
        try (FileReader reader = new FileReader(filename)) {
            Map<K, V> mapTemp = gson.fromJson(reader, type); // Load user map from JSON file
            Sync.safeReadEnds(filename);
            replaceMapContent(map, mapTemp);
        } catch (FileNotFoundException e) {
            Sync.safeReadEnds(filename);
            System.out.println(filename + " not found, creating a new file.");
            updateJson(filename, map, gson); // Create new JSON file if not found
        } catch (IOException e) {
            Sync.safeReadEnds(filename);
            System.err.println("Error loading map from " + filename + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendLine (String line, DataOutputStream out) {
        try {
            out.writeUTF(line);
        } catch (IOException e) {
            System.err.println("Error receiving order ID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendOrderId(int id, DataOutputStream out) {
        try {
            out.writeInt(id);
        } catch (IOException e) {
            System.err.println("Error sending Order ID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized int transaction(int size, String type, OrderBook orderBook) {
        if (!Costants.ASK.equals(type) && !Costants.BID.equals(type)) {
            throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }

        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = Costants.ASK.equals(type) ? orderBook.getBidMap() : orderBook.getAskMap();
        if (map.isEmpty()) {
            return 1;
        }

        Integer price = Costants.ASK.equals(type) ? orderBook.getBidMarketPrice(orderBook.getBidMap()) : orderBook.getAskMarketPrice(orderBook.getAskMap()); // get market price
        while (size > 0 && price != null) {
            ConcurrentLinkedQueue<Order> queue = map.get(price);
            Iterator<Order> iterator = queue.iterator();
            while (iterator.hasNext() && size > 0) {
                Order limitOrder = iterator.next();
                if (size >= limitOrder.getSize()) {
                    size -= limitOrder.getSize();
                    iterator.remove();
                } else {
                    limitOrder.setSize(limitOrder.getSize() - size);
                    size = 0;
                }

                if (queue.isEmpty()) {
                    map.remove(price);
                }

                if (size == 0) {
                    break;
                }
                else if (size < 0) {
                    throw new IllegalArgumentException("Size cannot be negative: " + size);
                }
            }
            price = Costants.ASK.equals(type) ? map.lowerKey(price) : map.higherKey(price);
        }
        return size;
    }

    public static void checkTransaction (int size, String type, OrderBook orderBook) {
        if (size > 0) {
            orderBook.resetOrderBook(type);
        } else if (size == 0) {
            orderBook.updateOrderBook(type);
        }
    }

    public static void printMap (ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map) {
        System.out.println("printing map...");
        if (map.keySet().isEmpty()) {
            System.out.println("empty map");
        }
        for (int key : map.keySet()) {
            ConcurrentLinkedQueue<Order> queue = map.get(key);
            for (Order order : queue) {
                if (queue.isEmpty()) {
                    System.out.println("empty queue");
                }
                int orderId = order.getId();
                System.out.println("orderId = " + orderId);
                String type = order.getType();
                System.out.println("type = " + type);
                int size = order.getSize();
                System.out.println("size = " + size);
                int price = order.getPrice();
                System.out.println("price = " + price);
                String username = order.getUsername();
                System.out.println("username = " + username);
            }
        }
        System.out.println("");
    }
}
