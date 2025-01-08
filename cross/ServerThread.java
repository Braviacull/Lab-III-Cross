package cross;

import com.google.gson.*;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
        this.clientSocket = socket;
        this.properties = properties; 
        this.stopString = properties.getStopString();
        this.gson = gson;
        this.usersMap = usersMap;
        this.usersMapTemp = new ConcurrentHashMap<>();
        this.username = "";
        this.loggedIn = false;
        this.orderBook = orderBook;
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
            System.err.println("Error in server thread: " + e.getMessage());
            e.printStackTrace();
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

                // Update usersMapTemp.json
                ServerMain.loadMapFromJson(Costants.USERS_MAP_TEMP_FILE, usersMapTemp);
                usersMapTemp.put(user.getUsername(), user);
                ServerMain.updateJson(Costants.USERS_MAP_TEMP_FILE, usersMapTemp);

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

                    // Update usersMapTemp.json
                    ServerMain.loadMapFromJson(Costants.USERS_MAP_TEMP_FILE, usersMapTemp);
                    usersMapTemp.put(username, newUser);
                    ServerMain.updateJson(Costants.USERS_MAP_TEMP_FILE, usersMapTemp);

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
                    if (values.getPrice() <= orderBook.getBidMarketPrice()) { // spread <= 0
                        System.out.println(values.getPrice() + " " + orderBook.getBidMarketPrice());
                        size = transaction(values.getSize(), values.getType());
                        checkTransaction(size, values.getType());
                    }
                    if (size != 0) { //transazione non provata o non riuscita
                        orderBook.addOrder(limitOrder, orderBook.getAskMap());
                        orderBook.addOrderToMapAndUpdateJson (Costants.ASK_MAP_TEMP_FILE, limitOrder); // update temp map and json
                    }
                    break;
                case Costants.BID:
                    if (values.getPrice() >= orderBook.getAskMarketPrice()) { // spread <= 0
                        System.out.println(values.getPrice() + " " + orderBook.getAskMarketPrice());
                        size = transaction(values.getSize(), values.getType());
                        checkTransaction(size, values.getType());
                    }
                    if (size != 0) {
                        orderBook.addOrder(limitOrder, orderBook.getBidMap()); // add order to main map
                        orderBook.addOrderToMapAndUpdateJson (Costants.BID_MAP_TEMP_FILE, limitOrder); // update temp map and json
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
            int size = transaction(values.getSize(), values.getType());
            checkTransaction(size, values.getType());
            sendMarketOrderIdAfterTransaction(size, values.getType());
        } catch (IOException e) {
            System.err.println("Error during insertMarketOrder: " + e.getMessage());
        }
    }

    private int transaction(int size, String type) {
        if (!Costants.ASK.equals(type) && !Costants.BID.equals(type)) {
            throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }

        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = Costants.ASK.equals(type) ? orderBook.getBidMap() : orderBook.getAskMap();
        if (map.isEmpty()) {
            return 1;
        }

        Integer price = Costants.ASK.equals(type) ? orderBook.getBidMarketPrice() : orderBook.getAskMarketPrice(); // get market price
        while (size > 0 && price != null) {
            ConcurrentLinkedQueue<Order> list = map.get(price);
            Iterator<Order> iterator = list.iterator();
            while (iterator.hasNext() && size > 0) {
                Order limitOrder = iterator.next();
                if (size >= limitOrder.getSize()) {
                    size -= limitOrder.getSize();
                    iterator.remove();
                } else {
                    limitOrder.setSize(limitOrder.getSize() - size);
                    size = 0;
                }

                if (list.isEmpty()) {
                    map.remove(price);
                }

                if (size == 0) {
                    break;
                }
                else if (size < 0) {
                    throw new IllegalArgumentException("Size cannot be negative: " + size);
                }
            }
            price = Costants.ASK.equals(type) ? map.lowerKey(price) : map.higherKey(price);
        }
        return size;
    }

    private void checkTransaction (int size, String type) {
        if (size > 0) {
            orderBook.resetOrderBook(type);
        } else if (size == 0) {
            orderBook.updateOrderBook(type);
        }
    }

    private void sendMarketOrderIdAfterTransaction (int size, String type) {
        if (size > 0) {
            MyUtils.sendOrderId(-1, out);
        } else if (size == 0) {
            Order marketOrder = new Order(type, size, username);
            properties.setNextId(Order.getNextId());
            MyUtils.sendOrderId(marketOrder.getId(), out);
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
                    orderBook.addOrder(stopOrder, orderBook.getAskMap());// add order to main map
                    orderBook.addOrderToMapAndUpdateJson (Costants.ASK_MAP_TEMP_STOP_FILE, stopOrder);// update temp map and json
                    break;
                case Costants.BID:
                    orderBook.addOrder(stopOrder, orderBook.getBidMap());// add order to main map
                    orderBook.addOrderToMapAndUpdateJson (Costants.BID_MAP_TEMP_STOP_FILE, stopOrder);// update temp map and json
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
}