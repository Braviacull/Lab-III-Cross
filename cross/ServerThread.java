package cross;

import com.google.gson.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.lang.Math;

public class ServerThread implements Runnable {
    private final Socket clientSocket;
    private AskStopOrdersExecutor askStopOrdersExecutor;
    private BidStopOrdersExecutor bidStopOrdersExecutor;
    private MyProperties properties;
    private final String stopString;
    private Gson gson;
    private ConcurrentHashMap<String, User> usersMap;
    private ConcurrentHashMap<String, IpPort> userIpPortMap;
    private OrderBook orderBook;
    private ConcurrentLinkedQueue<Trade> storicoOrdini;
    private AtomicBoolean running;
    private Boolean loggedIn;
    private String username;
    private DataInputStream in;
    private DataOutputStream out;
    private ResponseStatus responseStatus;

    public ServerThread(Socket socket, AskStopOrdersExecutor askStopOrdersExecutor, BidStopOrdersExecutor bidStopOrdersExecutor, ServerMain serverMain) {
        this.clientSocket = socket;
        this.askStopOrdersExecutor = askStopOrdersExecutor;
        this.bidStopOrdersExecutor = bidStopOrdersExecutor;
        this.properties = serverMain.getProperties(); 
        this.stopString = properties.getStopString();
        this.gson = serverMain.getGson();
        this.usersMap = serverMain.getUsersMap();
        this.userIpPortMap = serverMain.getUserIpPortMap();
        this.orderBook = serverMain.getOrderBook();
        this.storicoOrdini = serverMain.getStoricoOrdini();
        this.running = serverMain.getRunning();
        this.username = "";
        this.loggedIn = false;
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
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            String operation = "";
            while (!operation.equals(stopString) && running.get()) {
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
            case Costants.PING:
                if (running.get()){
                    out.writeUTF(Costants.ONLINE);
                } else {
                    out.writeUTF(Costants.OFFLINE);
                }
                break;
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
            case Costants.GET_PRICE_HISTORY:
                handleGetPriceHistory();
                break;
            default:
                if (operation.equals(stopString)) return;
        }
    }

    private void handleRegister() {
        try {
            String json = in.readUTF();
            RegistrationRequest reg = gson.fromJson(json, RegistrationRequest.class);

            if (loggedIn) {
                responseStatus = new ResponseStatus(103, reg);
                return;
            }

            if (!Costants.REGISTER.equals(reg.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + reg.getOperation());
            }

            RegistrationRequest.Values values = reg.getValues();
            User user = values.getUser();

            if (usersMap.containsKey(user.getUsername())) {
                responseStatus = new ResponseStatus(102, reg);
            } else {
                usersMap.put(user.getUsername(), user);
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

            if (values.arePasswordsEquals()) {
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
                responseStatus = new ResponseStatus(104, login);
                return;
            }

            if (!Costants.LOGIN.equals(login.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + login.getOperation());
            }

            LoginRequest.Values values = login.getValues();
            User user = values.getUser();

            if (!usersMap.containsKey(user.getUsername())) {
                responseStatus = new ResponseStatus(101, login);
            } else if (userIpPortMap.containsKey(user.getUsername())){
                responseStatus = new ResponseStatus(102, login);
            } else {
                User registeredUser = usersMap.get(user.getUsername());
                if (gson.toJson(registeredUser).equals(gson.toJson(user))) {
                    loggedIn = true;
                    username = user.getUsername();
                    responseStatus = new ResponseStatus(100, login);

                    // rendo il client raggiungibile per le notifiche
                    IpPort ipPort = new IpPort(clientSocket.getInetAddress(), properties.getNotificationPort());
                    userIpPortMap.put(username, ipPort);
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
                System.out.println(username + " logged Out");

                loggedIn = false;
                this.username = "";
                responseStatus = new ResponseStatus(100, logout);
                
                userIpPortMap.remove(username); // il client non é più raggiungibile per le notifiche
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

            if (!loggedIn) {
                MyUtils.sendOrderId(-1, out);
            }

            if (!Costants.INSERT_LIMIT_ORDER.equals(insertLO.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + insertLO.getOperation());
            }

            InsertLimitOrderRequest.Values values = insertLO.getValues();
            Order limitOrder = new Order(values.getType(), values.getSize(), values.getPrice(), username);

            Trade trade = new Trade(limitOrder.getId(), limitOrder.getType(), Costants.LIMIT, limitOrder.getSize(), limitOrder.getPrice(), (int) Instant.now().getEpochSecond());
            storicoOrdini.add(trade);

            int size = limitOrder.getSize();
            switch (values.getType()) {
                case Costants.ASK:
                // MyUtils.limitCondition(limitOrder, orderBook.getBidMarketPrice(orderBook.getBidMap()))
                    if (values.getPrice() <= orderBook.getBidMarketPrice(orderBook.getBidMap())) { // spread <= 0
                        size = MyUtils.limitTransaction(limitOrder, orderBook, userIpPortMap, gson);
                    }
                    if (size != 0) { // transazione immediata COMPLETA non riuscita
                        // non c'é bisogno di sincronizzare perché l'aggiunta non é un problema (al massimo é più size disponibile) e la mappa é concorrente
                        limitOrder.setSize(size);
                        orderBook.addOrder(limitOrder, orderBook.getAskMap());
                        bidStopOrdersExecutor.notifyOrdersExecutor();
                    } else {
                        MyUtils.sendNotification(userIpPortMap.get(limitOrder.getUsername()), new Notification(trade), gson);
                    }
                    break;
                case Costants.BID:
                    if (values.getPrice() >= orderBook.getAskMarketPrice(orderBook.getAskMap())) { // spread <= 0
                        size = MyUtils.limitTransaction(limitOrder, orderBook, userIpPortMap, gson);
                    }
                    if (size != 0) { // transazione immediata COMPLETA non riuscita
                        // non c'é bisogno di sincronizzare perché l'aggiunta non é un problema (al massimo é più size disponibile) e la mappa é concorrente
                        limitOrder.setSize(size);
                        orderBook.addOrder(limitOrder, orderBook.getBidMap());
                        askStopOrdersExecutor.notifyOrdersExecutor();
                    } else {
                        MyUtils.sendNotification(userIpPortMap.get(limitOrder.getUsername()), new Notification(trade), gson);
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

            if (!loggedIn) {
                MyUtils.sendOrderId(-1, out);
            }

            if (!Costants.INSERT_MARKET_ORDER.equals(insertMO.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + insertMO.getOperation());
            }

            InsertMarketOrderRequest.Values values = insertMO.getValues();
            int marketPrice = Costants.ASK.equals(values.getType()) ? orderBook.getBidMarketPrice(orderBook.getBidMap()) : orderBook.getAskMarketPrice(orderBook.getAskMap());
            
            int limit;
            switch (values.getType()) {
                case Costants.ASK:
                    limit = 0; //voglio vendere a chiunque
                    break;
                case Costants.BID:
                    limit = Integer.MAX_VALUE; // voglio comprare da chiunque non importa se mi fa un prezzo alto
                    break;
                default:
                    throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }
            
            int size = MyUtils.transaction(values.getSize(), limit, values.getType(), Costants.MARKET, orderBook, userIpPortMap, gson);
            
            if (size > 0) marketPrice = -1; // registro un ordine con price -1 se l'ordine viene scartato e non lo considero in getPriceHistory

            Order marketOrder = new Order(values.getType(), values.getSize(), username);
            Trade trade = new Trade(marketOrder.getId(), marketOrder.getType(), Costants.MARKET, marketOrder.getSize(), marketPrice, (int) Instant.now().getEpochSecond());
            storicoOrdini.add(trade);

            if (size > 0) {
                System.out.println("Market Order: " + marketOrder.getId() + " SCARTATO");

                MyUtils.sendOrderId(-1, out);
            } else if (size == 0) {
                System.out.println("Market Order: " + marketOrder.getId() + " ESEGUITO");

                properties.setNextId(Order.getNextId());
                MyUtils.sendOrderId(marketOrder.getId(), out);
            } else {
                throw new IllegalArgumentException ("size must not be negative, SIZE: " + size);
            }
        } catch (IOException e) {
            System.err.println("Error during insertMarketOrder: " + e.getMessage());
        }
    }

    private void handleInsertStopOrder () {
        try {
            String json = in.readUTF();
            InsertStopOrderRequest insertSO = gson.fromJson(json, InsertStopOrderRequest.class);

            if (!loggedIn) {
                MyUtils.sendOrderId(-1, out);
            }

            if (!Costants.INSERT_STOP_ORDER.equals(insertSO.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + insertSO.getOperation());
            }

            InsertStopOrderRequest.Values values = insertSO.getValues();
            Order stopOrder = new Order(values.getType(), values.getSize(), values.getPrice(), username);

            Trade trade = new Trade(stopOrder.getId(), stopOrder.getType(), Costants.STOP, stopOrder.getSize(), stopOrder.getPrice(), (int) Instant.now().getEpochSecond());
            storicoOrdini.add(trade);

            switch (values.getType()) {
                case Costants.ASK:
                    orderBook.addOrder(stopOrder, orderBook.getAskMapStop());
                    askStopOrdersExecutor.notifyOrdersExecutor();
                    break;
                case Costants.BID:
                    orderBook.addOrder(stopOrder, orderBook.getBidMapStop());
                    bidStopOrdersExecutor.notifyOrdersExecutor();
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

            if (!loggedIn) {
                responseStatus = new ResponseStatus(101, cancelOrderRequest);
                return;
            }

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
                        responseStatus = new ResponseStatus(100, cancelOrderRequest);
                        break;
                    }
                    if (bools.isFound() && !bools.isDeleted()){ // trovato ma username non corretto
                        responseStatus = new ResponseStatus(101, cancelOrderRequest);
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error during CancelOrder: " + e.getMessage());
        }
    }

    private void handleGetPriceHistory () {
        try {
            String json = in.readUTF();
            GetPriceHistoryRequest getPriceHistoryRequest = gson.fromJson(json, GetPriceHistoryRequest.class);

            if (!Costants.GET_PRICE_HISTORY.equals(getPriceHistoryRequest.getOperation())) {
                throw new IllegalArgumentException("Invalid operation: " + getPriceHistoryRequest.getOperation());
            }

            GetPriceHistoryRequest.Values values = getPriceHistoryRequest.getValues();
            int monthTarget = values.getMonth();

            int max = 0;
            int min = Integer.MAX_VALUE;
            int apertura = 0;
            int chiusura = 0;
            boolean firstTradeFound = false;
            
            for (Trade trade : storicoOrdini) {
                int epoch = trade.getTimestamp();
                int month = DateGMT.convertEpochToGMT(epoch);
                if (monthTarget == month) {
                    int price = trade.getPrice();

                    if (price != -1) {
                        max = Math.max(max, price);
                        min = Math.min(min, price);
                
                        if (!firstTradeFound) {
                            apertura = price;
                            firstTradeFound = true;
                        }
                        chiusura = price;
                    }
                }
            }

            if (!firstTradeFound) {
                // invia un responso negativo al client
                responseStatus = new ResponseStatus(101, getPriceHistoryRequest);
                out.writeUTF(gson.toJson(responseStatus));

            } else {
                // invia un responso positivo al client
                responseStatus = new ResponseStatus(100, getPriceHistoryRequest);
                out.writeUTF(gson.toJson(responseStatus));

                // crea una struttura dati da inviare come json
                PriceHistory priceHistory = new PriceHistory(max, min, apertura, chiusura);
                json = gson.toJson(priceHistory);
                MyUtils.sendJson(json, out);
            }

        } catch (IOException e) {
            System.err.println("Error during getPriceHistory: " + e.getMessage());
        }
    }
}