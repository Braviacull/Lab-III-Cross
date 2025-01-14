package cross;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
    private ConcurrentHashMap<String, IpPort> userIpPortMap = new ConcurrentHashMap<>();
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

        loadMapFromJson(Costants.USERS_MAP_FILE, usersMap); // Load user map from JSON

        orderBook = new OrderBook(gson); // Initialize order book
        
        // PRINTS
        System.out.println("AskMap");
        MyUtils.printMap(orderBook.getAskMap());
        System.out.println("BidMap");
        MyUtils.printMap(orderBook.getBidMap());
        System.out.println("AskMapStop");
        MyUtils.printMap(orderBook.getAskMapStop());
        System.out.println("BidMapStop");
        MyUtils.printMap(orderBook.getBidMapStop());

        int nextID = properties.getNextId(); // Get next order ID from properties
        Order.setNextID(nextID); // Set next order ID

        server = new ServerSocket(properties.getPort()); // Initialize server socket
    }

    private void acceptConnections(){
        System.out.println("Server started");
        try {
            System.out.println("ServerIP: " + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        connessioni = Executors.newCachedThreadPool(); // Initialize thread pool for handling connections
        AskStopOrdersExecutor askStopOrdersExecutor = new AskStopOrdersExecutor(orderBook, userIpPortMap, gson);
        Thread threadAsk = new Thread(askStopOrdersExecutor);
        threadAsk.start();
        BidStopOrdersExecutor bidStopOrdersExecutor = new BidStopOrdersExecutor(orderBook, userIpPortMap, gson);
        Thread threadBid = new Thread(bidStopOrdersExecutor);
        threadBid.start();
        try {
            while (true) {
                Socket clientSocket = server.accept(); // Accept client connection
                System.out.println("ClientIP: " + clientSocket.getInetAddress().getHostAddress());
                connessioni.execute(new ServerThread(clientSocket, properties, usersMap, userIpPortMap, orderBook, gson, askStopOrdersExecutor, bidStopOrdersExecutor)); // Handle client connection in a new thread
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