package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

// classe contenente le funzioni per la gestione dello storico degli ordini
public class StoricoOrdini {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void loadStoricoOrdini(String fileName, ConcurrentLinkedQueue<Trade> storicoOrdini) {
        try (FileReader reader = new FileReader(fileName)) {
            Map<String, List<Trade>> tempMap = gson.fromJson(reader, new TypeToken<Map<String, List<Trade>>>() {}.getType());
            List<Trade> list = tempMap.get("trades");
            storicoOrdini.addAll(new ConcurrentLinkedQueue<>(list));
        } catch (FileNotFoundException e) {
            updateJson(fileName, storicoOrdini);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateJson(String filename, ConcurrentLinkedQueue<Trade> storicoOrdini) {
        HashMap<String, ConcurrentLinkedQueue<Trade>> map = new HashMap<String, ConcurrentLinkedQueue<Trade>>();
        map.put("trades", storicoOrdini);
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(map, writer); // Save user map to JSON file
        } 
        catch (IOException e) {
            System.err.println("Error updating map to " + filename + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
