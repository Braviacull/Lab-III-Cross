package cross;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerMain {
    private MyProperties properties;
    private static Type mapType;
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ServerSocket server;
    private ExecutorService connessioni;
    private ConcurrentHashMap<String, User> usersMap = new ConcurrentHashMap<String, User> ();
    private ConcurrentHashMap<String, IpPort> userIpPortMap = new ConcurrentHashMap<>();
    private OrderBook orderBook;
    private ConcurrentLinkedQueue<Trade> storicoOrdini = new ConcurrentLinkedQueue<>();
    private AtomicInteger workingThreads = new AtomicInteger(0);

    public ServerMain() {
        try {
            initializeServer(); // Initialize server configurations and resources
            acceptConnections(); // Start accepting client connections
        } catch (IOException e) {
            System.err.println("Error initializing server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public MyProperties getProperties() {
        return properties;
    }

    public Gson getGson () {
        return gson;
    }

    public ConcurrentHashMap<String, User> getUsersMap () {
        return usersMap;
    }

    public ConcurrentHashMap<String, IpPort> getUserIpPortMap () {
        return userIpPortMap;
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }

    public ConcurrentLinkedQueue<Trade> getStoricoOrdini () {
        return storicoOrdini;
    }

    public AtomicInteger getWorkingThreads () {
        return workingThreads;
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

        StoricoOrdini.loadStoricoOrdini(Costants.STORICO_ORDINI_TEMP, storicoOrdini);
        
        orderBook = new OrderBook(gson); // Initialize order book

        int nextID = properties.getNextId(); // Get next order ID from properties
        Order.setNextID(nextID); // Set next order ID

        server = new ServerSocket(properties.getPort()); // Initialize server socket
    }

    private void acceptConnections(){
        System.out.println();
        try {
            System.out.println("Server started\nServerIP: " + InetAddress.getLocalHost().getHostAddress());
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
        
        PeriodicUpdate periodicUpdate = new PeriodicUpdate(30000, this);
        Thread threadPeriodicUpdate = new Thread(periodicUpdate);
        threadPeriodicUpdate.start();
        try {
            while (true) {
                Socket clientSocket = server.accept(); // Accept client connection
                System.out.println("Server listening\nClientIP: " + clientSocket.getInetAddress().getHostAddress());
                connessioni.execute(new ServerThread(clientSocket, askStopOrdersExecutor, bidStopOrdersExecutor, this)); // Handle client connection in a new thread
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