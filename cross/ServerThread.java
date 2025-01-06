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
            default:
                if (operation.equals(stopString)) {
                    System.out.println("Client disconnected");
                } else {
                    throw new IllegalArgumentException("Invalid operation: " + operation);
                }
                break;
        }
    }

    private synchronized void updateJson(ConcurrentMap<String, User> map, String jsonName) {
        try (FileWriter writer = new FileWriter(jsonName)) {
            gson.toJson(map, writer);
        } catch (IOException e) {
            System.err.println("Error updating JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private synchronized ConcurrentHashMap<String, User> loadMapFromJson(String name) {
        ConcurrentHashMap<String, User> map = new ConcurrentHashMap<>();
        try (FileReader reader = new FileReader(name)) {
            Type userMapType = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();
            map = gson.fromJson(reader, userMapType);
        } catch (IOException e) {
            System.err.println("Error loading JSON: " + e.getMessage());
            e.printStackTrace();
        }
        return map;
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
                usersMapTemp = loadMapFromJson(Costants.USERS_MAP_TEMP_FILE);
                usersMapTemp.put(user.getUsername(), user);
                updateJson(usersMapTemp, Costants.USERS_MAP_TEMP_FILE);

                responseStatus = new ResponseStatus(100, reg);
            }
        } catch (IOException e) {
            System.err.println("Error during register: " + e.getMessage());
            sendErrorResponse();
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
                    usersMapTemp = loadMapFromJson(Costants.USERS_MAP_TEMP_FILE);
                    usersMapTemp.put(username, newUser);
                    updateJson(usersMapTemp, Costants.USERS_MAP_TEMP_FILE);

                    responseStatus = new ResponseStatus(100, update);
                } else {
                    responseStatus = new ResponseStatus(102, update);
                }
            } else {
                responseStatus = new ResponseStatus(102, update);
            }
        } catch (IOException e) {
            System.err.println("Error during updateCredentials: " + e.getMessage());
            sendErrorResponse();
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
            sendErrorResponse();
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
            sendErrorResponse();
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
            LimitOrder limitOrder = new LimitOrder(values.getType(), values.getSize(), values.getPrice());

            ConcurrentSkipListMap<Integer, List<LimitOrder>> mapTemp;
            switch (values.getType()) {
                case Costants.ASK:
                    orderBook.addLimitOrder(limitOrder, orderBook.getAskMap());
                    mapTemp = orderBook.loadMapFromJson(Costants.ASK_MAP_TEMP_FILE);
                    orderBook.addLimitOrder(limitOrder, mapTemp);
                    orderBook.updateJson(mapTemp, Costants.ASK_MAP_TEMP_FILE);
                    orderBook.printMap(orderBook.getAskMap()); // print
                    break;
                case Costants.BID:
                    orderBook.addLimitOrder(limitOrder, orderBook.getBidMap());
                    mapTemp = orderBook.loadMapFromJson(Costants.BID_MAP_TEMP_FILE);
                    orderBook.addLimitOrder(limitOrder, mapTemp);
                    orderBook.updateJson(mapTemp, Costants.BID_MAP_TEMP_FILE);
                    orderBook.printMap(orderBook.getBidMap()); // print
                    break;
                default:
                    out.writeInt(-1);
                    throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }

            properties.setNextId(Order.getNextId());
            out.writeInt(limitOrder.getId());
        } catch (IOException e) {
            System.err.println("Error during insertLimitOrder: " + e.getMessage());
            sendErrorResponse();
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
            transaction(values.getSize(), values.getType());
        } catch (IOException e) {
            System.err.println("Error during insertMarketOrder: " + e.getMessage());
            sendErrorResponse();
        }
    }

    private void transaction(int size, String type) {
        try {
            if (!Costants.ASK.equals(type) && !Costants.BID.equals(type)) {
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }

            ConcurrentSkipListMap<Integer, List<LimitOrder>> map = Costants.ASK.equals(type) ? orderBook.getBidMap() : orderBook.getAskMap();
            if (map.isEmpty()) {
                out.writeInt(-1);
                return;
            }

            Integer price = Costants.ASK.equals(type) ? map.lastKey() : map.firstKey();
            while (size > 0 && price != null) {
                List<LimitOrder> list = map.get(price);
                Iterator<LimitOrder> iterator = list.iterator();
                while (iterator.hasNext() && size > 0) {
                    LimitOrder limitOrder = iterator.next();
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

                    if (size < 0) {
                        out.writeInt(-1);
                        throw new IllegalArgumentException("Size cannot be negative: " + size);
                    }
                }
                price = Costants.ASK.equals(type) ? map.lowerKey(price) : map.higherKey(price);
            }
            if (size > 0) {
                orderBook.resetOrderBook(type);
                out.writeInt(-1);
            } else {
                orderBook.updateOrderBook(type);
                MarketOrder marketOrder = new MarketOrder(type, size);
                properties.setNextId(Order.getNextId());
                out.writeInt(marketOrder.getId());
            }
            if (type.equals(Costants.ASK)){
                System.out.println("\n\nbidMap");
                orderBook.printMap(orderBook.getBidMap()); // print
            }

            else if (type.equals(Costants.BID)){
                System.out.println("\n\naskMap");
                orderBook.printMap(orderBook.getAskMap()); // print
            }
        } catch (IOException e) {
            System.err.println("Error during transaction: " + e.getMessage());
            sendErrorResponse();
        }
    }

    private void sendErrorResponse() {
        try {
            out.writeInt(-1);
        } catch (IOException e) {
            System.err.println("Error sending error response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}