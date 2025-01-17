package server;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// La classe ServerMain gestisce il server e le sue operazioni principali
public class ServerMain {
    private MyProperties properties; // Oggetto per gestire le proprietà del server
    private static Type mapType; // Tipo della mappa per la deserializzazione JSON
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Oggetto Gson per la serializzazione/deserializzazione JSON
    private ServerSocket server; // Socket del server
    private ExecutorService connessioni; // Thread pool per gestire le connessioni
    private ExecutorService services; // Thread pool per eseguire i servizi
    private ConcurrentHashMap<String, User> usersMap = new ConcurrentHashMap<>(); // Mappa degli utenti
    private ConcurrentHashMap<String, IpPort> userIpPortMap = new ConcurrentHashMap<>(); // Mappa degli indirizzi IP e porte degli utenti
    private OrderBook orderBook; // Oggetto OrderBook per gestire gli ordini
    private ConcurrentLinkedQueue<Trade> storicoOrdini = new ConcurrentLinkedQueue<>(); // Coda concorrente per lo storico degli ordini
    private AtomicBoolean running = new AtomicBoolean(true); // Flag per controllare se il server è in esecuzione
    private AskStopOrdersExecutor askStopOrdersExecutor; // Esecutore per gli ordini stop ask
    private BidStopOrdersExecutor bidStopOrdersExecutor; // Esecutore per gli ordini stop bid
    private PeriodicUpdate periodicUpdate; // Oggetto per l'aggiornamento periodico dei file JSON

    // Costruttore che inizializza il server
    public ServerMain() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown)); // Aggiunge un hook per gestire lo shutdown del server
            initializeServer(); // Inizializza le configurazioni e le risorse del server
            acceptConnections(); // Inizia ad accettare le connessioni dei client
        } catch (IOException e) {
            System.err.println("Error initializing server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Metodo per gestire lo shutdown del server
    public void shutdown() {
        System.out.println("Server will shut down soon...");
        running.set(false);

        connessioni.shutdown();
        services.shutdown();

        askStopOrdersExecutor.stop();
        bidStopOrdersExecutor.stop();
        periodicUpdate.stop();

        try {
            services.awaitTermination(properties.getAwaitSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            connessioni.awaitTermination(properties.getAwaitSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Server shut down");
        
    }

    // Metodi getter per ottenere le proprietà e le mappe
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

    public AtomicBoolean getRunning () {
        return running;
    }

    // Metodo per caricare una mappa da un file JSON
    public static void loadMapFromJson (String fileName, ConcurrentHashMap<String, User> map) {
        MyUtils.loadMapFromJson(fileName, map, mapType, gson);
    }

    // Metodo per aggiornare un file JSON con i dati di una mappa
    public static void updateJson (String fileName, ConcurrentHashMap<String, User> map) {
        MyUtils.updateJson(fileName, map, gson);
    }

    // Metodo per inizializzare il server
    private void initializeServer() throws IOException {
        properties = new MyProperties(Costants.SERVER_PROPERTIES_FILE); // Carica le proprietà del server
        mapType = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();

        loadMapFromJson(Costants.USERS_MAP_FILE, usersMap); // Carica la mappa degli utenti dal file JSON

        StoricoOrdini.loadStoricoOrdini(Costants.STORICO_ORDINI, storicoOrdini); // Carica lo storico degli ordini
        
        orderBook = new OrderBook(gson); // Inizializza l'order book

        int nextID = properties.getNextId(); // Ottiene il prossimo ID dell'ordine dalle proprietà
        Order.setNextID(nextID); // Imposta il prossimo ID dell'ordine

        server = new ServerSocket(properties.getPort()); // Inizializza il socket del server
    }

    private void acceptConnections(){
        try {
            System.out.println("Server started\nServerIP: " + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        connessioni = Executors.newCachedThreadPool();
        services = Executors.newFixedThreadPool(3);

        askStopOrdersExecutor = new AskStopOrdersExecutor(this);
        services.execute(askStopOrdersExecutor);

        bidStopOrdersExecutor = new BidStopOrdersExecutor(this);
        services.execute(bidStopOrdersExecutor);
        
        periodicUpdate = new PeriodicUpdate(properties.getPeriod(), this);
        services.execute(periodicUpdate);
        try {
            while (true) {
                Socket clientSocket = server.accept(); // Accetta la connessione del client
                System.out.println("Server listening\nClientIP: " + clientSocket.getInetAddress().getHostAddress());
                connessioni.execute(new ServerThread(clientSocket, askStopOrdersExecutor, bidStopOrdersExecutor, this)); // gestisci la connessione in un thread separato
            }
        } catch (IOException e) {
            if (!running.get()){
                System.err.println("Error accepting connections: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new ServerMain();
    }
}