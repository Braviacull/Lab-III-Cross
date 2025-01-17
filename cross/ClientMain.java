package cross;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// La classe ClientMain gestisce la comunicazione client-server e le operazioni dell'utente
public class ClientMain {
    private MyProperties properties; // Oggetto Properties per leggere la configurazione
    private Socket socket; // Socket per la comunicazione client-server
    private DataOutputStream out; // Stream di output per inviare dati al server
    private DataInputStream in; // Stream di input per ricevere dati dal server
    private Scanner scanner; // Scanner per leggere l'input dell'utente
    private Gson gson; // Oggetto Gson per la serializzazione/deserializzazione JSON
    private String username; // Nome utente dell'utente loggato
    private boolean firstTimeLogIn; // Flag per verificare se l'utente sta facendo il login per la prima volta
    private AutomaticLogout automaticLogout; // Oggetto per il logout automatico
    private ReceiveNotification receiveNotification; // Oggetto per ricevere notifiche
    private PeriodicPing periodicPing; // Oggetto per inviare ping periodici al server
    private AtomicBoolean loggedIn = new AtomicBoolean(false); // Flag per verificare se l'utente è loggato
    private AtomicBoolean isServerOnline = new AtomicBoolean(true); // Flag per verificare se il server è online

    public ClientMain() {
        try {
            try {
                System.out.println("ClientIP: " + InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            
            properties = new MyProperties(Costants.CLIENT_PROPERTIES_FILE); // Carica le proprietà dal file
            socket = new Socket(properties.getServerIP(), properties.getPort()); // stabilisce una connessione col server
            out = new DataOutputStream(socket.getOutputStream()); // Inizializza lo stream di output
            in = new DataInputStream(socket.getInputStream()); // Inizializza lo stream di input
            scanner = new Scanner(System.in); // Inizializza lo scanner per l'input dell'utente
            gson = new GsonBuilder().setPrettyPrinting().create(); // Inizializza l'oggetto Gson
            username = ""; // Inizializza il nome utente come vuoto
            loggedIn.set(false);

            sendRequests(); // Inizia a inviare richieste al server
        } catch (IOException e) {
            System.err.println("Error initializing client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public AtomicBoolean getIsServerOnline () {
        return isServerOnline;
    }

    private void defaultBehavior (String line) {
        if (line.equals(Costants.GET_PRICE_HISTORY)){
            handleGetPriceHistory();
        }
        else if (!line.equals(properties.getStopString())) {
            Sync.printlnSync("Unrecognized action");
        } 
        else if (line.equals(properties.getStopString())) {
            try {
                if (loggedIn.get()) {
                    handleLogout();
                }
                out.writeUTF(line); // comunica al server che il client sta per terminare la sua esecuzione
            } catch (IOException e) {
                System.err.println("Error sending stop command: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Metodo per verificare se il server è online, inviando un messaggio periodicamente
    public boolean isServerOnline() {
        try {
            synchronized (this) {
                out.writeUTF(Costants.PING);
                String response = in.readUTF();
                return response.equals(Costants.ONLINE);
            }
        } catch (IOException e) {
            return false;
        }
    }

    private void sendRequests() throws IOException {
        // Avvia il ping periodico per verificare lo stato del server
        periodicPing = new PeriodicPing(properties.getPeriodicPingTimeout(), this);
        Thread periodicPingThread = new Thread(periodicPing);
        periodicPingThread.start();

        String line = "";
        while (!line.equals(properties.getStopString())) {
            if (!loggedIn.get()) {
                Sync.printlnSync(Costants.LOGGED_OUT_POSSIBLE_ACTIONS);
            } else {
                Sync.printlnSync(Costants.LOGGED_IN_POSSIBLE_ACTIONS);
            }

            line = scanner.nextLine();

            if (!isServerOnline()) {
                close();
                return;
            }
            
            synchronized (this) { // ci sono diversi thread che inviano messaggi al server
                synchronized (Sync.console) { // ci sono diversi thread che scrivono sulla console
                    if (!loggedIn.get()){
                        username = "";
                        switch (line) {
                            case Costants.REGISTER:
                                handleRegister();
                                break;
                            case Costants.UPDATE_CREDENTIALS:
                                handleUpdateCredentials();
                                break;
                            case Costants.LOGIN:
                                handleLogin();
                                break;
                            default:
                                defaultBehavior(line);
                                break;
                        }
                    } else {
                        automaticLogout.resetTimer();
                        switch (line) {
                            case Costants.LOGOUT:
                                handleLogout();
                                break;
                            case Costants.INSERT_LIMIT_ORDER:
                                handleInsertLimitOrder();
                                break;
                            case Costants.INSERT_MARKET_ORDER:
                                handleInsertMarketOrder();
                                break;
                            case Costants.INSERT_STOP_ORDER:
                                handleInstertStopOrder();
                                break;
                            case Costants.CANCEL_ORDER:
                                handleCancelOrder();
                                break;
                            default:
                                defaultBehavior(line);
                                break;
                        }
                    }
                }
            }
        }
        close(); // Chiude le risorse prima di uscire
    }

    private void handleRegister(){
        String username = scanField("username"); // Scan username
        String password = scanField("password"); // Scan password
        register (username, password);
    }

    private void register (String username, String password) {
        MyUtils.sendLine(Costants.REGISTER, out, isServerOnline); // invia la parola scansionata da linea di comando al server
        RegistrationRequest reg = RequestFactory.createRegistrationRequest(username, password); // Crea richiesta di registrazione
        String jsonReg = gson.toJson(reg); // Converte la richiesta in JSON per inviarla al server
        MyUtils.sendLine(jsonReg, out, isServerOnline); // Invia la richiesta JSON al server
        checkResponse(Costants.REGISTER, username); // Verifica la risposta del server
        if (firstTimeLogIn) {
            firstTimeLogIn = false;
            login(username, password);
        }
    }

    private void handleUpdateCredentials(){
        String username = scanField("username"); // Scan username
        String currentPassword = scanField("current password"); // Scan current password
        String newPassword = scanField("new password"); // Scan new password
        updateCredentials(username, currentPassword, newPassword);
    }

    // analoga a register
    private void updateCredentials(String username, String currentPassword, String newPassword) {
        MyUtils.sendLine(Costants.UPDATE_CREDENTIALS, out, isServerOnline); 
        UpdateCredentialsRequest update = RequestFactory.createUpdateCredentialsRequest(username, currentPassword, newPassword);
        String jsonUpdate = gson.toJson(update);
        MyUtils.sendLine(jsonUpdate, out, isServerOnline);
        checkResponse(Costants.UPDATE_CREDENTIALS, username);
    }

    private void handleLogin(){
        String username = scanField("username"); // Scan username
        String password = scanField("password"); // Scan password
        login(username, password);
    }

    // analoga a register
    private void login (String username, String password) {
        MyUtils.sendLine(Costants.LOGIN, out, isServerOnline);
        LoginRequest login = RequestFactory.createLoginRequest(username, password);
        String jsonLogin = gson.toJson(login);
        MyUtils.sendLine(jsonLogin, out, isServerOnline);
        checkResponse(Costants.LOGIN, username);
    }

    public void handleLogout(){
        if (loggedIn.get()) {
            logout(username);
        } else {
            throw new IllegalArgumentException ("User not logged in");
        }
    }

    // analoga a register
    private void logout (String username) {
        MyUtils.sendLine(Costants.LOGOUT, out, isServerOnline);
        LogoutRequest logout = RequestFactory.createLogoutRequest();
        String jsonLogout = gson.toJson(logout);
        MyUtils.sendLine(jsonLogout, out, isServerOnline);
        MyUtils.sendLine(username, out, isServerOnline); // Invia il nome utente separatamente poiché logout.Values è un oggetto vuoto
        checkResponse(Costants.LOGOUT, username);
    }

    private void handleInsertLimitOrder(){
        String type = scanType(); // Scansiona il tipo di ordine (ask o bid)
        String size = scanIntField("size"); // Scansiona la dimensione dell'ordine
        String price = scanIntField("price"); // Scansiona il prezzo limite dell'ordine
        insertLimitOrder(type, Integer.parseInt(size), Integer.parseInt(price));
    }

    private void insertLimitOrder (String tipo, int dimensione, int prezzoLimite) {
        MyUtils.sendLine(Costants.INSERT_LIMIT_ORDER, out, isServerOnline); // invia la parola scansionata da linea di comando al server
        InsertLimitOrderRequest insertLimitOrderRequest = RequestFactory.createInsertLimitOrderRequest(tipo, dimensione, prezzoLimite); // Crea una richiesta per gestire il limit order
        String json = gson.toJson(insertLimitOrderRequest); // Converte la richiesta in JSON per inviarla al server
        MyUtils.sendLine(json, out, isServerOnline); // Invia la richiesta JSON al server
        receiveIdOrder(); // Riceve l'ID dell'ordine dal server
    }

    // analoga a handleInsertLimitOrder
    private void handleInsertMarketOrder(){
        String type = scanType(); 
        String size = scanIntField("size");
        insertMarketOrder(type, Integer.parseInt(size));
    }

    // analoga a insertLimitOrder
    private void insertMarketOrder (String tipo, int dimensione) {
        MyUtils.sendLine(Costants.INSERT_MARKET_ORDER, out, isServerOnline);
        InsertMarketOrderRequest insertMarketOrderRequest = RequestFactory.createInsertMarketOrderRequest(tipo, dimensione);
        String json = gson.toJson(insertMarketOrderRequest);
        MyUtils.sendLine(json, out, isServerOnline);
        receiveIdOrder();
    }

    // analoga a handleInsertLimitOrder
    private void handleInstertStopOrder(){
        String type = scanType();
        String size = scanIntField("size");
        String price = scanIntField("price");
        insertStopOrder(type, Integer.parseInt(size), Integer.parseInt(price));
    }

    // analoga a insertLimitOrder
    private void insertStopOrder (String tipo, int dimensione, int stopPrice) {
        MyUtils.sendLine(Costants.INSERT_STOP_ORDER, out, isServerOnline);
        InsertStopOrderRequest insertStopOrderRequest = RequestFactory.createInsertStopOrderRequest(tipo, dimensione, stopPrice);
        String json = gson.toJson(insertStopOrderRequest);
        MyUtils.sendLine(json, out, isServerOnline);
        receiveIdOrder();

    }

    private void handleCancelOrder() {
        String orderId = scanIntField("orderId"); // Scan orderId
        cancelOrder (Integer.parseInt(orderId));
    }

    private void cancelOrder (int orderId) {
        MyUtils.sendLine(Costants.CANCEL_ORDER, out, isServerOnline); // invia la parola scansionata da linea di comando al server
        CancelOrderRequest cancelOrderRequest = RequestFactory.createCancelOrderRequest(orderId); // crea la richiesta
        String json = gson.toJson(cancelOrderRequest); // Converte la richiesta in JSON per mandarla al server
        MyUtils.sendLine(json, out, isServerOnline); // Invia la richiesta JSON al server
        checkResponse(Costants.CANCEL_ORDER, username); // Verifica la risposta del server
    }

    // analoga a handleCancelOrder
    private void handleGetPriceHistory () {
        String month = scanMonth("month");
        getPriceHistory(Integer.parseInt(month));
    }

    // analoga a cancelOrder
    private void getPriceHistory (int month) {
        MyUtils.sendLine(Costants.GET_PRICE_HISTORY, out, isServerOnline);
        GetPriceHistoryRequest getPriceHistoryRequest = RequestFactory.createGetPriceHistoryRequest(month);
        String json = gson.toJson(getPriceHistoryRequest);
        MyUtils.sendLine(json, out, isServerOnline);
        checkResponse(Costants.GET_PRICE_HISTORY, username);
    }

    private void receiveIdOrder() {
        try {
            int id = in.readInt(); // Legge l'ID dell'ordine dallo stream di input
            if (id == -1) {
                System.out.println("Order aborted, error code: " + id);
            } else {
                System.out.println("Order " + id + " submitted");
            }
        } catch (IOException e) {
            if (isServerOnline.get()){ // é un errore solo se il server é online
                System.err.println("Error receiving order ID: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void checkResponse(String operation, String username) {
        try {
            String response = in.readUTF(); // Legge la risposta dal server
            ResponseStatus responseStatus = gson.fromJson(response, ResponseStatus.class); // Converte la risposta JSON in un oggetto ResponseStatus

            int responseCode = responseStatus.getResponseCode(); // Ottiene il codice di risposta
            String errorMessage = responseStatus.getErrorMessage(); // Ottiene il messaggio di errore

            // Stampa la risposta ricevuta dal server
            if (!operation.equals(properties.getStopString())) {
                System.out.println("Operation: " + operation + " Response " + responseCode + " - " + errorMessage);
            }

            if (responseCode == 100) { // CASO DI SUCCESSO
                switch (operation) {
                    case Costants.REGISTER:
                        firstTimeLogIn = true;
                        break;
                    case Costants.LOGIN:
                        this.username = username; // Imposta il nome utente
                        loggedIn.set(true);

                        // Inizializza l'oggetto per ricevere le notifiche in base a se client e server hanno IP diversi o meno
                        if (properties.getServerIP().equals(InetAddress.getLoopbackAddress().getHostAddress())){
                            receiveNotification = new ReceiveNotification(InetAddress.getLoopbackAddress(), properties.getNotificationPort());
                        } else {
                            receiveNotification = new ReceiveNotification(InetAddress.getLocalHost(), properties.getNotificationPort());
                        }
                        // Avvia il thread per ricevere le notifiche
                        Thread receiveNotificationThread = new Thread(receiveNotification);
                        receiveNotificationThread.start();
                        
                        // Avvia il timeout per il logout automatico
                        automaticLogout = new AutomaticLogout(properties.getTimeout(), this);
                        Thread timeouThread = new Thread(automaticLogout);
                        timeouThread.start();
                        break;
                    case Costants.LOGOUT:
                        loggedIn.set(false);

                        automaticLogout.stop(); // termina il thread per il logout automatico
                        automaticLogout = null;

                        receiveNotification.stop(); // termina il thread per la ricezione delle notifiche
                        receiveNotification = null;
                        break;
                    case Costants.GET_PRICE_HISTORY:
                        String history = MyUtils.receiveJson(in);
                        if (history == null) {
                            System.err.println("Failed to receive price history.");
                        } else {
                            System.out.println("Received price history:\n" + history); // stampa il resoconto del mese richiesto
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            if (isServerOnline.get()){
                System.err.println("Error checking response: " + e.getMessage());
                e.printStackTrace();
            } else {
            }
        }
    }

     // Metodo per chiudere le risorse
     public void close() {
        try {
            if (socket != null) socket.close(); // Chiude il socket
            if (out != null) out.close(); // Chiude lo stream di output
            if (in != null) in.close(); // Chiude lo stream di input
            if (scanner != null) scanner.close(); // Chiude lo scanner
            if (automaticLogout != null) automaticLogout.stop(); // Ferma il logout automatico
            if (receiveNotification != null) receiveNotification.stop(); // Ferma la ricezione delle notifiche
            if (periodicPing != null) periodicPing.stop(); // Ferma il ping periodico
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String scanField(String field) {
        String res = "";
        while (true) {
            System.out.println("Enter " + field); // Richiede all'utente di inserire il campo
            res = scanner.nextLine(); // Legge l'input dell'utente
            if (res.equals("")) {
                System.out.println(field + " cannot be empty");
            } else {
                break;
            }
        }
        return res;
    }

    private String scanType() {
        while (true) {
            String type = scanField("type");
            if (type.equals(Costants.ASK) || type.equals(Costants.BID)) {
                return type;
            } else {
                System.out.println("type must be 'ask' or 'bid'");
            }
        }
    }

    private boolean isMonth (String month) {
        // Verifica se la stringa è formattata come MMYYYY
        return month.matches("^(0[1-9]|1[0-2])\\d{4}$");
    }

    private String scanMonth(String field) {
        String res = "";
        while (true) {
            res = scanField(field);
            if (isMonth(res)) {
                break;
            } else {
                System.out.println("Month must be an integer in the form: MMYYYY, where MM is a number between 01 and 12.");
            }
        }
        return res;
    }

    private boolean isInt(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
    
    public String scanIntField(String field) {
        String res = "";
        while (true) {
            res = scanField(field);
            if (isInt(res)) {
                break;
            } else {
                System.out.println("size and price must be integers");
            }
        }
        return res;
    }

    public static void main(String[] args) {
        new ClientMain();
    }
}