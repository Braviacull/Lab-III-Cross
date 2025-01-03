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
    private Gson gson;
    private ServerSocket server;
    private ExecutorService connessioni;
    private ConcurrentHashMap<String, User> usersMap;
    private ConcurrentHashMap<String, User> usersLogMap;
    private OrderBook orderBook;

    public ServerMain() {
        try {
            properties = new MyProperties("server.properties");
            gson = new GsonBuilder().setPrettyPrinting().create();

            usersLogMap = new ConcurrentHashMap<String, User>();
            usersMap = new ConcurrentHashMap<String, User>();

            usersLogMap = loadMapFromJson("usersLogMap.json");
            usersMap = loadMapFromJson("usersMap.json");
            usersMap = updateMap(usersLogMap, usersMap);

            orderBook = new OrderBook(gson);

            int nextID = properties.getNextId();
            Order.setNextID(nextID);
            
            // Scrivi una hashmap vuota in usersLogMap.json
            try (FileWriter writer = new FileWriter("usersLogMap.json")) {
                gson.toJson(new ConcurrentHashMap<String, User>(), writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            server = new ServerSocket(properties.getPort(), 50, InetAddress.getByName(properties.getServerIP()));
            
            acceptConnections();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ConcurrentHashMap<String, User> updateMap(ConcurrentHashMap<String, User> usersLogMap, ConcurrentHashMap<String, User> usersMap) {
        for (User user : usersLogMap.values()) {
            usersMap.put(user.getUsername(), user);
        }
        try (FileWriter writer = new FileWriter("usersMap.json")) {
            gson.toJson(usersMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return usersMap;
    }
    
    private ConcurrentHashMap<String, User> loadMapFromJson(String name) {
        ConcurrentHashMap<String, User> Map = new ConcurrentHashMap<String, User>();
        try (FileReader reader = new FileReader(name)) {
            Type userMapType = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();
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

    private void acceptConnections() throws IOException {
        System.out.println("ServerMain started");
        connessioni = Executors.newCachedThreadPool();
        try {
            while (true) {
                Socket clientSocket = server.accept();
                connessioni.execute(new ServerThread(clientSocket, properties, usersMap, orderBook , gson));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            new ServerMain();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}