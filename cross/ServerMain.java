package cross;

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
    private static Type mapType;
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ServerSocket server;
    private ExecutorService connessioni;
    private ConcurrentHashMap<String, User> usersMap = new ConcurrentHashMap<String, User> ();
    private ConcurrentHashMap<String, User> usersMapTemp = new ConcurrentHashMap<String, User> ();
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

    public static void loadMapFromJson (String fileName, ConcurrentHashMap<String, User> map) {
        MyUtils.loadMapFromJson(fileName, map, mapType, gson);
    }

    public static void updateJson (String fileName, ConcurrentHashMap<String, User> map) {
        MyUtils.updateJson(fileName, map, gson);
    }

    private void initializeServer() throws IOException {
        properties = new MyProperties(Costants.SERVER_PROPERTIES_FILE); // Load server properties

        mapType = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();
        loadMapFromJson(Costants.USERS_MAP_TEMP_FILE, usersMapTemp); // Load temporary user map from JSON
        loadMapFromJson(Costants.USERS_MAP_FILE, usersMap); // Load main user map from JSON
        updateUserMap(usersMap, usersMapTemp); // Update main user map with temporary user map

        updateJson(Costants.USERS_MAP_FILE, usersMap); // Save updated user map to JSON
        updateJson(Costants.USERS_MAP_TEMP_FILE, new ConcurrentHashMap<>()); // Clear temporary user map

        orderBook = new OrderBook(gson); // Initialize order book

        int nextID = properties.getNextId(); // Get next order ID from properties
        Order.setNextID(nextID); // Set next order ID

        server = new ServerSocket(properties.getPort(), 50, InetAddress.getByName(properties.getServerIP())); // Initialize server socket
    }

    private void updateUserMap(ConcurrentHashMap<String, User> targetMap, ConcurrentHashMap<String, User> sourceMap) {
        for (User user : sourceMap.values()) {
            targetMap.put(user.getUsername(), user); // Update main user map with users from temporary map
        }
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