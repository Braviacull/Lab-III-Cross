package cross;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Type;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ServerThread implements Runnable {
    private final Socket clientSocket;
    private MyProperties properties;
    private final String stopString;
    private DataInputStream in;
    private DataOutputStream out;
    private Gson gson;
    private ResponseStatus responseStatus;
    private ConcurrentHashMap<String, User> usersMap;
    private ConcurrentHashMap<String, User> usersMapTemp;
    private String username;
    private Boolean loggedIn;
    private OrderBook orderBook;

    public ServerThread(Socket socket, MyProperties properties, ConcurrentHashMap<String, User> usersMap, OrderBook orderBook, Gson gson) {

        clientSocket = socket;
        this.properties = properties; 
        this.stopString = properties.getStopString();
        this.gson = gson;
        this.usersMap = usersMap;
        this.usersMapTemp = new ConcurrentHashMap<String, User>();
        username = "";
        loggedIn = false;
        this.orderBook = orderBook;
    }

    private void close () throws IOException {
        in.close();
        out.close();
    }

    public void run (){
        try {
            System.out.println("Server listening");

            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            String operation = "";
            while (!operation.equals(stopString)) {
                // receiving operation from client
                operation = in.readUTF();

                // Manage cases
                switch (operation) {
                    case "register":
                        handleregister();
                        out.writeUTF(gson.toJson(responseStatus)); // send response
                        break;
                    case "updateCredentials":
                        handleupdateCredentials();
                        out.writeUTF(gson.toJson(responseStatus)); // send response
                        break;
                    case "login":
                        handleLogin();
                        out.writeUTF(gson.toJson(responseStatus)); // send response
                        break;
                    case "logout":
                        handleLogout();
                        out.writeUTF(gson.toJson(responseStatus)); // send response
                        break;
                    case "insertLimitOrder":
                        handleInsertLimitOrder();
                        break;
                    case "insertMarketOrder":
                        handleInsertMarketOrder();
                        break;
                    default:
                        System.out.println("Client disconnected");
                        break;
                }
            }

            close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void updateJson (ConcurrentMap map, String jsonName) {
        try (FileWriter writer = new FileWriter(jsonName)) {
            gson.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized ConcurrentHashMap<String, User> loadMapFromJson(String name) {
        ConcurrentHashMap<String, User> Map = new ConcurrentHashMap<String, User>();
        try (FileReader reader = new FileReader(name)) {
            Type userMapType = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();
            Map = gson.fromJson(reader, userMapType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Map;
    }

    private void handleInsertMarketOrder() {
        // receive JSON from the ClientMain
        String json = "";
        try {
            json = in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // deserialize the json
        InsertMarketOrderRequest insertMO = gson.fromJson(json, InsertMarketOrderRequest.class);

        // Check if the operation is the expected one
        if (!insertMO.getOperation().equals("insertMarketOrder")) {
            throw new IllegalArgumentException("Operazione non valida: " + insertMO.getOperation());
        }

        // get values (type, size)
        InsertMarketOrderRequest.Values values = insertMO.getValues();

        String type = values.getType();
        int size = values.getSize();

        exchangeMatching(size, type);
    }

    private void exchangeMatching(int size, String type) {
        try {
            if (!type.equals("ask") && !type.equals("bid")) {
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }

            ConcurrentSkipListMap<Integer, List<LimitOrder>> map = type.equals("ask") ? orderBook.getBidMap() : orderBook.getAskMap();
            if (map.isEmpty()){
                out.writeInt(-1);
                return;
            }

            Integer price = type.equals("ask") ? map.lastKey() : map.firstKey();
            while (size > 0 && price != null) { // scorro la mappa di liste di ordini
                List<LimitOrder> list = map.get(price);
                System.out.println("Processing price: " + price + ", list size: " + (list != null ? list.size() : "null"));
                Iterator<LimitOrder> iterator = list.iterator();
                while (iterator.hasNext() && size > 0) {
                    LimitOrder limitOrder = iterator.next(); // scorro la lista con price più alto (priorità)
                    System.out.println("Processing limit order: " + limitOrder.getId() + ", size: " + limitOrder.getSize());
                    if (size >= limitOrder.getSize()) {
                        size -= limitOrder.getSize();
                        iterator.remove();
                        System.out.println("Removed limit order: " + limitOrder.getId());
                    } else {
                        int newSize = limitOrder.getSize() - size;
                        System.out.println("Updated limit order " + limitOrder.getId() + " old size: " + limitOrder.getSize() + " new size: " + newSize);
                        limitOrder.setSize(newSize);
                        size = 0;
                    }
    
                    if (list.isEmpty()) { // se esco dal for perché la lista é vuota, la elimino
                        map.remove(price);
                        System.out.println("Removed empty list for price: " + price);
                    }
    
                    if (size == 0) { // se esco dal for perché size == 0, la lista non é vuota, allora la lascio
                        System.out.println("TRANSAZIONE ESEGUITA CORRETTAMENTE");
                        break;
                    }
    
                    // non dovrebbe succedere
                    if (size < 0) {
                        out.writeInt(-1);
                        throw new IllegalArgumentException("Size cannot be negative: " + size);
                    }
                }
                price = type.equals("ask") ? map.lowerKey(price) : map.higherKey(price);
                System.out.println("Next price: " + price);
            }
            if (size > 0) { // ordine parzialmente evaso: NON deve avere effetto
                System.out.println("size > 0");
                // Annullo le modifiche sulla map
                switch (type) {
                    case "ask":
                        map = orderBook.loadMapFromJson("bidMap.json", gson);
                        orderBook.setBidMap(map);
                        break;
                    case "bid":
                        map = orderBook.loadMapFromJson("askMap.json", gson);
                        orderBook.setAskMap(map);
                        break;
                    default:
                        throw new IllegalArgumentException("Size cannot be negative: " + size);
                }
                out.writeInt(-1);
            }
            if (size == 0) { // ordine completamente evaso: DEVE avere effetto
                System.out.println("size == 0");

                // aggiorna il json della mappa e resetta la mappa di log dopo l'aggiornamento
                switch (type) {
                    case "ask":
                        orderBook.emptyBidTempANDUpdate();
                        break;
                    case "bid":
                        orderBook.emptyAskTempANDUpdate();
                        break;
                    default:
                        out.writeInt(-1);
                        throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
                }
    
                // mando l'id dell'ordine appena evaso al client
                MarketOrder marketOrder = new MarketOrder(type, size);
                properties.setNextId(Order.getNextId());
                out.writeInt(marketOrder.getId());
            }
        } catch (IOException e) {
            System.err.println("Error during insertMarketOrder: " + e.getMessage());
            try {
                out.writeInt(-1);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void handleInsertLimitOrder () {
        try {
            // receive JSON from the ClientMain
            String json = in.readUTF();

            // deserialize the json
            InsertLimitOrderRequest insertLO = gson.fromJson(json, InsertLimitOrderRequest.class);

            // Check if the operation is the expected one
            if (!insertLO.getOperation().equals("insertLimitOrder")) {
                throw new IllegalArgumentException("Operazione non valida: " + insertLO.getOperation());
            }

            // get values (type, size, price) from insertLO
            InsertLimitOrderRequest.Values values = insertLO.getValues();

            String type = values.getType();
            int size = values.getSize();
            int price = values.getPrice();

            // Create the limitOrder obj
            LimitOrder limitOrder = new LimitOrder(type, size, price);

            ConcurrentSkipListMap<Integer, List<LimitOrder>> MapTemp;

            switch (type) {
                case "ask":
                    orderBook.addLimitOrder(limitOrder, orderBook.getAskMap());
                    MapTemp = orderBook.loadMapFromJson("askMapTemp.json", gson);
                    orderBook.addLimitOrder(limitOrder, MapTemp);
                    updateJson(MapTemp, "askMapTemp.json");
                    break;
                case "bid":
                    orderBook.addLimitOrder(limitOrder, orderBook.getBidMap());
                    MapTemp = orderBook.loadMapFromJson("bidMapTemp.json", gson);
                    orderBook.addLimitOrder(limitOrder, MapTemp);
                    updateJson(MapTemp, "bidMapTemp.json");
                    break;
                default:
                    out.writeInt(-1);
                    throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }

            properties.setNextId(Order.getNextId());

            out.writeInt(limitOrder.getId());
            
        } catch (IOException e) {
            System.err.println("Error during insertLimitOrder: " + e.getMessage());
            try {
                out.writeInt(-1);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void handleregister() {
        try {
            // receive JSON from the ClientMain
            String json = in.readUTF();

            // deserialize the json
            RegistrationRequest reg = gson.fromJson(json, RegistrationRequest.class);

            // Check if the operation is the expected one
            if (!reg.getOperation().equals("register")) {
                throw new IllegalArgumentException("Operazione non valida: " + reg.getOperation());
            }

            // get values (username, password) from reg
            RegistrationRequest.Values values = reg.getValues();

            // get the User obj
            User user = values.getUser();

            if (usersMap.containsKey(user.getUsername())){
                responseStatus = new ResponseStatus(102, reg);
            }
            else {
                usersMap.put(user.getUsername(), user);

                usersMapTemp = loadMapFromJson("usersMapTemp.json");
                usersMapTemp.put(user.getUsername(), user);
                updateJson(usersMapTemp, "usersMapTemp.json");

                System.out.println("User registered successfully");

                responseStatus = new ResponseStatus(100, reg);
            }
        } catch (IOException e) {
            System.err.println("Error during register: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleLogin() {
        try {
            // receive JSON from the ClientMain
            String json = in.readUTF();

            // deserialize the json
            LoginRequest login = gson.fromJson(json, LoginRequest.class);

            if (loggedIn) {
                responseStatus = new ResponseStatus(102, login);
                return;
            }

            // Check if the operation is the expected one
            if (!login.getOperation().equals("login")) {
                throw new IllegalArgumentException("Operazione non valida: " + login.getOperation());
            }

            // get values (username, password) from reg
            LoginRequest.Values values = login.getValues();

            //get the User obj
            User user = values.getUser();

            // Check if the user is among the registered users
            if (!usersMap.containsKey(user.getUsername())){
                responseStatus = new ResponseStatus(101, login);
            }
            else if (usersMap.containsKey(user.getUsername())){
                User registeredUser = usersMap.get(user.getUsername());

                // OLD PASSWORD MUST MATCH WITH THE ONE IN usersMap.json
                if (gson.toJson(registeredUser).equals(gson.toJson(user))){
                    loggedIn = true;
                    username = user.getUsername();
                    responseStatus = new ResponseStatus(100, login);
                }
                else responseStatus = new ResponseStatus(101, login);
            }
        } catch (IOException e) {
            System.err.println("Error during login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleupdateCredentials () {
        try {
            // receive JSON from the ClientMain
            String json = in.readUTF(); // updateCredentialsRequest

            // deserialize the json in a User obj
            UpdateCredentialsRequest update = gson.fromJson(json, UpdateCredentialsRequest.class);

            // Un utente loggato non può cambiare la password
            if (loggedIn) {
                responseStatus = new ResponseStatus(104, update);
                return;
            }

            // Check if the operation is the expected one
            if (!update.getOperation().equals("updateCredentials")) {
                throw new IllegalArgumentException("Operazione non valida: " + update.getOperation());
            }

            // get values (username, old_password, new_password) from update
            UpdateCredentialsRequest.Values values = update.getValues();

            // Check if the new password is different
            if (values.comparePasswords()) {
                System.out.println("passwords must be different");
                responseStatus = new ResponseStatus(103, update);
                return;
            }

            //get old_user from values
            User old_user = values.getOldUser();

            // the username of old_user and new_user are the same
            String username = old_user.getUsername();

            // Check if the user is registered
            if (usersMap.containsKey(username)) {
                User registeredUser = usersMap.get(username);
                
                // OLD PASSWORD MUST MATCH WITH THE ONE IN usersMap.json
                if (gson.toJson(registeredUser).equals(gson.toJson(old_user))) {
                    System.out.println("passwords match");

                    // get new_user from values
                    User new_user = values.getNewUser();

                    // replace the old user in the hashMap with the updated user
                    usersMap.put(username, new_user);

                    // carico la mappa di log, aggiungo il nuovo user e poi faccio l'update del json
                    usersMapTemp = loadMapFromJson("usersMapTemp.json");
                    usersMapTemp.put(username, new_user);
                    updateJson(usersMapTemp, "usersMapTemp.json");
                    
                    responseStatus = new ResponseStatus(100, update);
                }
                else {
                    System.out.println("passwords mismatch");
                    responseStatus = new ResponseStatus(102, update);
                }
            }
            else {
                System.out.println("User not found");
                responseStatus = new ResponseStatus(102, update);
            }
        } catch (IOException e) {
            System.err.println("Error during updateCredentials: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleLogout () {
        try {
            // receive JSON from the ClientMain
            String jsonRequest = in.readUTF();
            
            // deserialize the json
            LogoutRequest logout = gson.fromJson(jsonRequest, LogoutRequest.class);

            // logout.Values is an empty obj so we need to receive username separately
            String username = in.readUTF();
            
            if (!loggedIn){
                responseStatus = new ResponseStatus(101, logout);
                return;
            }

            // Check if the operation is the expected one
            if (!logout.getOperation().equals("logout")) {
                throw new IllegalArgumentException("Operazione non valida: " + logout.getOperation());
            }

            if (this.username.equals(username)) {
                loggedIn = false;
                this.username = "";
                responseStatus = new ResponseStatus(100, logout);
            }
            else {
                responseStatus = new ResponseStatus(101, logout);
            }
        } catch (IOException e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
}