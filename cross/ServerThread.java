package cross;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.Instant;

public class ServerThread implements Runnable {
    private final Socket clientSocket;
    private MyProperties properties;
    private final String stopString;
    private DataInputStream in;
    private DataOutputStream out;
    private Gson gson;
    private ResponseStatus responseStatus;
    private ConcurrentHashMap<String, User> usersMap;
    private ConcurrentHashMap<String, IpPort> userIpPortMap;
    private String username;
    private Boolean loggedIn;
    private OrderBook orderBook;
    private AskStopOrdersExecutor askStopOrdersExecutor;
    private BidStopOrdersExecutor bidStopOrdersExecutor;

    public ServerThread(Socket socket, MyProperties properties, ConcurrentHashMap<String, User> usersMap, ConcurrentHashMap<String, IpPort> userIpPortMap, OrderBook orderBook, Gson gson, AskStopOrdersExecutor askStopOrdersExecutor, BidStopOrdersExecutor bidStopOrdersExecutor) {
        this.clientSocket = socket;
        this.properties = properties; 
        this.stopString = properties.getStopString();
        this.gson = gson;
        this.usersMap = usersMap;
        this.userIpPortMap = userIpPortMap;
        this.username = "";
        this.loggedIn = false;
        this.orderBook = orderBook;
        this.askStopOrdersExecutor = askStopOrdersExecutor;
        this.bidStopOrdersExecutor = bidStopOrdersExecutor;
    }

    private void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            System.out.println("Server listening");

            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            String operation = "";
            while (!operation.equals(stopString)) {
                operation = in.readUTF();
                handleOperation(operation);
            }
        } catch (IOException e) {
            if (loggedIn){
                System.out.println(username + " disconnected: automatic logout");
            } else {
                System.out.println("Client disconnected: automatic logout");
            }
        } finally {
            close();
        }
    }

    private void handleOperation(String operation) throws IOException {
        switch (operation) {
            case Costants.REGISTER:
                handleRegister();
                out.writeUTF(gson.toJson(responseStatus));
                break;
            case Costants.UPDATE_CREDENTIALS:
                handleUpdateCredentials();
                out.writeUTF(gson.toJson(responseStatus));
                break;
            case Costants.LOGIN:
                handleLogin();
                out.writeUTF(gson.toJson(responseStatus));
                break;
            case Costants.LOGOUT:
                handleLogout();
                out.writeUTF(gson.toJson(responseStatus));
                break;
            case Costants.INSERT_LIMIT_ORDER:
                handleInsertLimitOrder();
                break;
            case Costants.INSERT_MARKET_ORDER:
                handleInsertMarketOrder();
                break;
            case Costants.INSERT_STOP_ORDER:
                handleInsertStopOrder();
                break;
            case Costants.CANCEL_ORDER:
                handleCancelOrder();
                out.writeUTF(gson.toJson(responseStatus));
                break;
            default:
                if (operation.equals(stopString)) {
                    System.out.println("Client disconnected");
                } else {
                    throw new IllegalArgumentException("Invalid operation: " + operation);
                }
                break;
        }
    }

    private void handleRegister() {
        try {
            String json = in.readUTF();
            RegistrationRequest reg = gson.fromJson(json, RegistrationRequest.class);

            if (!Costants.REGISTER.equals(reg.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + reg.getOperation());
            }

            RegistrationRequest.Values values = reg.getValues();
            User user = values.getUser();

            if (usersMap.containsKey(user.getUsername())) {
                responseStatus = new ResponseStatus(102, reg);
            } else {
                usersMap.put(user.getUsername(), user);
                ServerMain.updateJson(Costants.USERS_MAP_FILE, usersMap);

                responseStatus = new ResponseStatus(100, reg);
            }
        } catch (IOException e) {
            System.err.println("Error during register: " + e.getMessage());
        }
    }

    private void handleUpdateCredentials() {
        try {
            String json = in.readUTF();
            UpdateCredentialsRequest update = gson.fromJson(json, UpdateCredentialsRequest.class);

            if (loggedIn) {
                responseStatus = new ResponseStatus(104, update);
                return;
            }

            if (!Costants.UPDATE_CREDENTIALS.equals(update.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + update.getOperation());
            }

            UpdateCredentialsRequest.Values values = update.getValues();

            if (values.comparePasswords()) {
                responseStatus = new ResponseStatus(103, update);
                return;
            }

            User oldUser = values.getOldUser();
            String username = oldUser.getUsername();

            if (usersMap.containsKey(username)) {
                User registeredUser = usersMap.get(username);
                if (gson.toJson(registeredUser).equals(gson.toJson(oldUser))) {
                    User newUser = values.getNewUser();
                    usersMap.put(username, newUser);
                    ServerMain.updateJson(Costants.USERS_MAP_FILE, usersMap);

                    responseStatus = new ResponseStatus(100, update);
                } else {
                    responseStatus = new ResponseStatus(102, update);
                }
            } else {
                responseStatus = new ResponseStatus(102, update);
            }
        } catch (IOException e) {
            System.err.println("Error during updateCredentials: " + e.getMessage());
        }
    }

    private void handleLogin() {
        try {
            String json = in.readUTF();
            LoginRequest login = gson.fromJson(json, LoginRequest.class);

            if (loggedIn) {
                responseStatus = new ResponseStatus(102, login);
                return;
            }

            if (!Costants.LOGIN.equals(login.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + login.getOperation());
            }

            LoginRequest.Values values = login.getValues();
            User user = values.getUser();

            if (!usersMap.containsKey(user.getUsername())) {
                responseStatus = new ResponseStatus(101, login);
            } else {
                User registeredUser = usersMap.get(user.getUsername());
                if (gson.toJson(registeredUser).equals(gson.toJson(user))) {
                    loggedIn = true;
                    username = user.getUsername();
                    IpPort ipPort = new IpPort(clientSocket.getInetAddress(), properties.getNotificationPort());
                    userIpPortMap.put(username, ipPort);
                    responseStatus = new ResponseStatus(100, login);
                } else {
                    responseStatus = new ResponseStatus(101, login);
                }
            }
        } catch (IOException e) {
            System.err.println("Error during login: " + e.getMessage());
        }
    }

    private void handleLogout() {
        try {
            String jsonRequest = in.readUTF();
            LogoutRequest logout = gson.fromJson(jsonRequest, LogoutRequest.class);
            String username = in.readUTF();

            if (!loggedIn) {
                responseStatus = new ResponseStatus(101, logout);
                return;
            }

            if (!Costants.LOGOUT.equals(logout.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + logout.getOperation());
            }

            if (this.username.equals(username)) {
                loggedIn = false;
                userIpPortMap.remove(username);
                this.username = "";
                responseStatus = new ResponseStatus(100, logout);
            } else {
                responseStatus = new ResponseStatus(101, logout);
            }
        } catch (IOException e) {
            System.err.println("Error during logout: " + e.getMessage());
        }
    }

    private void handleInsertLimitOrder() {
        try {
            String json = in.readUTF();
            InsertLimitOrderRequest insertLO = gson.fromJson(json, InsertLimitOrderRequest.class);

            if (!Costants.INSERT_LIMIT_ORDER.equals(insertLO.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + insertLO.getOperation());
            }

            InsertLimitOrderRequest.Values values = insertLO.getValues();
            Order limitOrder = new Order(values.getType(), values.getSize(), values.getPrice(), username);

            int size = 1;
            switch (values.getType()) {
                case Costants.ASK:
                    if (values.getPrice() <= orderBook.getBidMarketPrice(orderBook.getBidMap())) { // spread <= 0
                        size = MyUtils.transaction(values.getSize(), values.getPrice(), values.getType(), orderBook, userIpPortMap, gson);
                        if (size == 0){ // transazione riuscita
                            orderBook.updateJson(Costants.BID_MAP_FILE, orderBook.getBidMap());
                        }
                    }
                    if (size != 0) { //transazione non riuscita o non provata
                        orderBook.addOrder(limitOrder, orderBook.getAskMap());
                        orderBook.updateJson(Costants.ASK_MAP_FILE, orderBook.getAskMap());
                        bidStopOrdersExecutor.myNotify();
                    }
                    break;
                case Costants.BID:
                    if (values.getPrice() >= orderBook.getAskMarketPrice(orderBook.getAskMap())) { // spread <= 0
                        size = MyUtils.transaction(values.getSize(), values.getPrice(), values.getType(), orderBook, userIpPortMap, gson);
                        if (size == 0){
                            orderBook.updateJson(Costants.ASK_MAP_FILE, orderBook.getAskMap());
                        }
                    }
                    if (size != 0) {
                        orderBook.addOrder(limitOrder, orderBook.getBidMap()); // add order to main map
                        orderBook.updateJson(Costants.BID_MAP_FILE, orderBook.getBidMap());
                        askStopOrdersExecutor.myNotify();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }

            properties.setNextId(Order.getNextId());
            MyUtils.sendOrderId(limitOrder.getId(), out);
        } catch (IOException e) {
            System.err.println("Error during insertLimitOrder: " + e.getMessage());
        }
    }

    private void handleInsertMarketOrder() {
        try {
            String json = in.readUTF();
            InsertMarketOrderRequest insertMO = gson.fromJson(json, InsertMarketOrderRequest.class);

            if (!Costants.INSERT_MARKET_ORDER.equals(insertMO.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + insertMO.getOperation());
            }

            InsertMarketOrderRequest.Values values = insertMO.getValues();
            
            int limit = 0;
            switch (values.getType()) {
                case Costants.ASK:
                    break; // voglio vendere a chiunque
                case Costants.BID:
                    limit = Integer.MAX_VALUE; // voglio comprare da chiunque non importa se mi fa un prezzo alto
                    break;
                default:
                    throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }
            int size = MyUtils.transaction(values.getSize(), limit, values.getType(), orderBook, userIpPortMap, gson);
            if (size == 0) {
                switch (orderBook.reverseType(values.getType())) {
                    case Costants.ASK:
                        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getAskMap()), orderBook.getAskMap());
                        break;
                    case Costants.BID:
                        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getBidMap()), orderBook.getBidMap());
                        break;
                    default:
                        throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
                }
            }
            sendMarketOrderIdAfterTransaction(size, values.getType());
            
        } catch (IOException e) {
            System.err.println("Error during insertMarketOrder: " + e.getMessage());
        }
    }

    private void sendMarketOrderIdAfterTransaction (int size, String type) {
        if (size > 0) {
            MyUtils.sendOrderId(-1, out);
        } else if (size == 0) {
            Order marketOrder = new Order(type, size, username);
            properties.setNextId(Order.getNextId());
            MyUtils.sendOrderId(marketOrder.getId(), out);
            Trades trade = new Trades(marketOrder.getId(), marketOrder.getType(), Costants.MARKET, marketOrder.getSize(), (int) Instant.now().getEpochSecond());
            MyUtils.sendNotification(userIpPortMap.get(marketOrder.getUsername()), new Notification(trade), gson);
        } else {
            throw new IllegalArgumentException ("size must not be negative, SIZE: " + size);
        }
    }

    private void handleInsertStopOrder () {
        try {
            String json = in.readUTF();
            InsertStopOrderRequest insertSO = gson.fromJson(json, InsertStopOrderRequest.class);

            if (!Costants.INSERT_STOP_ORDER.equals(insertSO.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + insertSO.getOperation());
            }

            InsertStopOrderRequest.Values values = insertSO.getValues();
            Order stopOrder = new Order(values.getType(), values.getSize(), values.getPrice(), username);

            switch (values.getType()) {
                case Costants.ASK:
                    orderBook.addOrder(stopOrder, orderBook.getAskMapStop());// add order to  map
                    orderBook.updateJson(Costants.ASK_MAP_STOP_FILE, orderBook.getAskMapStop());
                    askStopOrdersExecutor.myNotify();
                    break;
                case Costants.BID: // da aggiustare NON USARE
                    orderBook.addOrder(stopOrder, orderBook.getBidMapStop());// add order to  map
                    orderBook.updateJson(Costants.BID_MAP_STOP_FILE, orderBook.getBidMapStop());
                    bidStopOrdersExecutor.myNotify();
                    break;
                default:
                    throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }

            properties.setNextId(Order.getNextId());
            MyUtils.sendOrderId(stopOrder.getId(), out);
        } catch (IOException e) {
            System.err.println("Error during insertStopOrder: " + e.getMessage());
        }
    }

    private void handleCancelOrder () {
        try {
            String json = in.readUTF();
            CancelOrderRequest cancelOrderRequest = gson.fromJson(json, CancelOrderRequest.class);

            if (!Costants.CANCEL_ORDER.equals(cancelOrderRequest.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + cancelOrderRequest.getOperation());
            }

            CancelOrderRequest.Values values = cancelOrderRequest.getValues();
            int idToDelete = values.getOrderId();
            
            MyUtils.Bools bools = new MyUtils.Bools(false, false);
            List<ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>>> maps = new ArrayList<>();
            maps.add(orderBook.getAskMap());
            maps.add(orderBook.getBidMap());
            maps.add(orderBook.getAskMapStop());
            maps.add(orderBook.getBidMapStop());

            for (ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map : maps) {
                if (!bools.isFound() && !bools.isDeleted()) { // non trovato e non eliminato
                    bools = MyUtils.searchAndDeleteOrderById(idToDelete, map, username);
                    if (bools.isFound() && bools.isDeleted()) { // trovato ed eliminato
                        orderBook.updateJson(orderBook.getJsonFileNameFromMap(map), map);
                        break;
                    }
                    if (bools.isFound() && !bools.isDeleted()){ // trovato ma username non corretto
                        break;
                    }
                }
            }

            // set responseStatus
            if (!bools.isDeleted()) { // non eliminato
                responseStatus = new ResponseStatus(101, cancelOrderRequest);
            }
            else { // eliminato
                responseStatus = new ResponseStatus(100, cancelOrderRequest);
            }

        } catch (IOException e) {
            System.err.println("Error during CancelOrder: " + e.getMessage());
        }
    }
}