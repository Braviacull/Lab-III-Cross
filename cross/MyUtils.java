package cross;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
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
}
