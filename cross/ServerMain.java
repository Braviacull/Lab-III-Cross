package cross;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMain {

    private MyProperties properties;
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ServerSocket server;
    private ExecutorService connessioni;
    private ConcurrentHashMap<String, User> usersMap;
    private ConcurrentHashMap<String, User> usersMapTemp;
    private OrderBook orderBook;

    public ServerMain() {
        try {
            initializeServer(); // Initialize server configurations and resources
            acceptConnections(); // Start accepting client connections
        } catch (IOException e) {
            System.err.println("Error initializing server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeServer() throws IOException {
        properties = new MyProperties(Costants.SERVER_PROPERTIES_FILE); // Load server properties

        usersMapTemp = loadMapFromJson(Costants.USERS_MAP_TEMP_FILE); // Load temporary user map from JSON
        usersMap = loadMapFromJson(Costants.USERS_MAP_FILE); // Load main user map from JSON
        usersMap = updateMap(usersMapTemp, usersMap); // Update main user map with temporary user map

        updateJson(usersMap, Costants.USERS_MAP_FILE); // Save updated user map to JSON
        updateJson(new ConcurrentHashMap<>(), Costants.USERS_MAP_TEMP_FILE); // Clear temporary user map

        orderBook = new OrderBook(gson); // Initialize order book

        int nextID = properties.getNextId(); // Get next order ID from properties
        Order.setNextID(nextID); // Set next order ID

        server = new ServerSocket(properties.getPort(), 50, InetAddress.getByName(properties.getServerIP())); // Initialize server socket
    }

    private ConcurrentHashMap<String, User> updateMap(ConcurrentHashMap<String, User> usersMapTemp, ConcurrentHashMap<String, User> usersMap) {
        for (User user : usersMapTemp.values()) {
            usersMap.put(user.getUsername(), user); // Update main user map with users from temporary map
        }
        return usersMap;
    }

    public static void updateJson(ConcurrentHashMap<String, User> map, String filename) {
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

    public static ConcurrentHashMap<String, User> loadMapFromJson(String filename) {
        Sync.safeReadStarts(filename);
        ConcurrentHashMap<String, User> map = new ConcurrentHashMap<>();
        try (FileReader reader = new FileReader(filename)) {
            Type userMapType = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();
            map = gson.fromJson(reader, userMapType); // Load user map from JSON file
        } catch (FileNotFoundException e) {
            System.out.println(filename + " not found, creating a new file.");
            updateJson(new ConcurrentHashMap<>() , filename); // Create new JSON file if not found
        } catch (IOException e) {
            System.err.println("Error loading map from " + filename + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            Sync.safeReadEnds(filename);
        }
        return map;
    }

    private void acceptConnections() {
        System.out.println("ServerMain started");
        connessioni = Executors.newCachedThreadPool(); // Initialize thread pool for handling connections
        try {
            while (true) {
                Socket clientSocket = server.accept(); // Accept client connection
                connessioni.execute(new ServerThread(clientSocket, properties, usersMap, orderBook, gson)); // Handle client connection in a new thread
            }
        } catch (IOException e) {
            System.err.println("Error accepting connections: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ServerMain(); // Start the server
    }
}