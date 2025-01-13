package cross;

import com.google.gson.*;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientMain {
    private MyProperties properties; // Properties object to read configuration
    private Socket socket; // Socket for client-server communication
    private DataOutputStream out; // Output stream to send data to the server
    private DataInputStream in; // Input stream to receive data from the server
    private Scanner scanner; // Scanner to read user input
    private Gson gson; // Gson object for JSON serialization/deserialization

    // for login-logout control
    private String username; // Username of the logged-in user
    private boolean firstTimeLogIn; // Flag to check if new user is registered
    private AutomaticLogout automaticLogout;
    private ReceiveNotification receiveNotification;
    private AtomicBoolean loggedIn = new AtomicBoolean();

    public ClientMain() {
        try {
            try {
                System.out.println("ClientIP: " + InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            
            properties = new MyProperties(Costants.CLIENT_PROPERTIES_FILE); // Load properties from file

            socket = new Socket(properties.getServerIP(), properties.getPort()); // Connect to the server
            out = new DataOutputStream(socket.getOutputStream()); // Initialize output stream
            in = new DataInputStream(socket.getInputStream()); // Initialize input stream
            scanner = new Scanner(System.in); // Initialize scanner for user input
            gson = new GsonBuilder().setPrettyPrinting().create(); // Initialize Gson object

            username = ""; // Initialize username as empty
            loggedIn.set(false);

            sendRequests(); // Start sending requests to the server
        } catch (IOException e) {
            System.err.println("Error initializing client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void defaultBehavior (String line) {
        if (!line.equals(properties.getStopString())) {
            System.out.println("Unrecognized action");
        } else {
            try {
                if (loggedIn.get()) {
                    handleLogout();
                }
                out.writeUTF(line); // Send stop command to the server
            } catch (IOException e) {
                System.err.println("Error sending stop command: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void sendRequests() throws IOException {
        String line = "";
        while (!line.equals(properties.getStopString())) {
            if (loggedIn.get()) {
                System.out.println("Possible actions: (exit, logout, insertLimitOrder, insertMarketOrder, insertStopOrder, cancelOrder)");
            } else {
                System.out.println("Possible actions: (exit, register, updateCredentials, login)");
            }

            line = scanner.nextLine();

            synchronized (Sync.timeOutSync) {
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
        close(); // Close resources when done
    }

    private void handleRegister(){
        String username = scanField("username"); // Scan username
        String password = scanField("password"); // Scan password
        register (username, password);
    }

    private void register (String username, String password) {
        MyUtils.sendLine(Costants.REGISTER, out);
        RegistrationRequest reg = RequestFactory.createRegistrationRequest(username, password); // Create registration request
        String jsonReg = gson.toJson(reg); // Convert request to JSON
        MyUtils.sendLine(jsonReg, out); // Send JSON request to the server
        checkResponse(Costants.REGISTER, username); // Check server response
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

    private void updateCredentials(String username, String currentPassword, String newPassword) {
        MyUtils.sendLine(Costants.UPDATE_CREDENTIALS, out);
        UpdateCredentialsRequest update = RequestFactory.createUpdateCredentialsRequest(username, currentPassword, newPassword); // Create update credentials request
        String jsonUpdate = gson.toJson(update); // Convert request to JSON
        MyUtils.sendLine(jsonUpdate, out); // Send JSON request to the server
        checkResponse(Costants.UPDATE_CREDENTIALS, username); // Check server response
    }

    private void handleLogin(){
        String username = scanField("username"); // Scan username
        String password = scanField("password"); // Scan password
        login(username, password);
    }

    private void login (String username, String password) {
        MyUtils.sendLine(Costants.LOGIN, out);
        LoginRequest login = RequestFactory.createLoginRequest(username, password); // Create login request
        String jsonLogin = gson.toJson(login); // Convert request to JSON
        MyUtils.sendLine(jsonLogin, out); // Send JSON request to the server
        checkResponse(Costants.LOGIN, username); // Check server response
    }

    private void handleLogout(){
        if (loggedIn.get()) {
            logout(username);
        } else {
            throw new IllegalArgumentException ("User not logged in");
        }
    }

    private void logout (String username) {
        MyUtils.sendLine(Costants.LOGOUT, out);
        LogoutRequest logout = RequestFactory.createLogoutRequest(); // Create logout request
        String jsonLogout = gson.toJson(logout); // Convert request to JSON
        MyUtils.sendLine(jsonLogout, out); // Send JSON request to the server
        MyUtils.sendLine(username, out); // Send username separately as logout.Values is an empty object
        checkResponse(Costants.LOGOUT, username); // Check server response
    }

    private void handleInsertLimitOrder(){
        String type = scanType(); // Scan the type of order (ask or bid)
        String size = scanIntField("size"); // Scan the size of the order
        String price = scanIntField("price"); // Scan the price of the order
        insertLimitOrder(type, Integer.parseInt(size), Integer.parseInt(price));
    }

    private void insertLimitOrder (String tipo, int dimensione, int prezzoLimite) {
        MyUtils.sendLine(Costants.INSERT_LIMIT_ORDER, out);
        InsertLimitOrderRequest insertLimitOrderRequest = RequestFactory.createInsertLimitOrderRequest(tipo, dimensione, prezzoLimite); // Create limit order request
        String json = gson.toJson(insertLimitOrderRequest); // Convert request to JSON
        MyUtils.sendLine(json, out); // Send JSON request to the server
        receiveIdOrder(); // Receive the order ID from the server
    }

    private void handleInsertMarketOrder(){
        String type = scanType(); // Scan the type of order (ask or bid)
        String size = scanIntField("size"); // Scan the size of the order
        insertMarketOrder(type, Integer.parseInt(size));
    }

    private void insertMarketOrder (String tipo, int dimensione) {
        MyUtils.sendLine(Costants.INSERT_MARKET_ORDER, out);
        InsertMarketOrderRequest insertMarketOrderRequest = RequestFactory.createInsertMarketOrderRequest(tipo, dimensione); // Create market order request
        String json = gson.toJson(insertMarketOrderRequest); // Convert request to JSON
        MyUtils.sendLine(json, out); // Send JSON request to the server
        receiveIdOrder(); // Receive the order ID from the server
    }

    private void handleInstertStopOrder(){
        String type = scanType(); // Scan the type of order (ask or bid)
        String size = scanIntField("size"); // Scan the size of the order
        String price = scanIntField("price"); // Scan the price of the order
        insertStopOrder(type, Integer.parseInt(size), Integer.parseInt(price));
    }

    private void insertStopOrder (String tipo, int dimensione, int stopPrice) {
        MyUtils.sendLine(Costants.INSERT_STOP_ORDER, out);
        InsertStopOrderRequest insertStopOrderRequest = RequestFactory.createInsertStopOrderRequest(tipo, dimensione, stopPrice);
        String json = gson.toJson(insertStopOrderRequest); // Convert request to JSON
        MyUtils.sendLine(json, out); // Send JSON request to the server
        receiveIdOrder(); // Receive the order ID from the server

    }

    private void handleCancelOrder() {
        String orderId = scanIntField("orderId");
        cancelOrder (Integer.parseInt(orderId));
    }

    private void cancelOrder (int orderId) {
        MyUtils.sendLine(Costants.CANCEL_ORDER, out);
        CancelOrderRequest cancelOrderRequest = RequestFactory.createCancelOrderRequest(orderId);
        String json = gson.toJson(cancelOrderRequest);
        MyUtils.sendLine(json, out); // Send JSON request to the server
        checkResponse(Costants.CANCEL_ORDER, username); // Check server response
    }

    private void receiveIdOrder() {
        try {
            int id = in.readInt(); // Read the order ID from the input stream
            if (id == -1) {
                System.out.println("Order aborted, error code: " + id);
            } else {
                System.out.println("Order " + id + " submitted");
            }
        } catch (IOException e) {
            System.err.println("Error receiving order ID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkResponse(String line, String username) {
        try {
            String response = in.readUTF(); // Read the response from the server
            ResponseStatus responseStatus = gson.fromJson(response, ResponseStatus.class); // Convert JSON response to ResponseStatus object

            int responseCode = responseStatus.getResponseCode(); // Get response code
            String errorMessage = responseStatus.getErrorMessage(); // Get error message

            // Print response received from server
            if (!line.equals(properties.getStopString())) {
                System.out.println(responseCode + " - " + errorMessage);
            }

            if (responseCode == 100) { // If response code is 100 (success)
                switch (line) {
                    case Costants.REGISTER:
                        firstTimeLogIn = true;
                        break;
                    case Costants.LOGIN:
                        this.username = username; // Set username
                        loggedIn.set(true);

                        // start receiving notification
                        System.out.print(InetAddress.getLocalHost().getHostAddress() + " " + properties.getServerIP() + "\n");
                        if (InetAddress.getLocalHost().getHostAddress().equals(properties.getServerIP())){
                            receiveNotification = new ReceiveNotification(InetAddress.getLocalHost(), properties.getNotificationPort());
                            System.out.print("localHostaddress\n");
                        } else {
                            System.out.print("loopbackaddress\n");
                            receiveNotification = new ReceiveNotification(InetAddress.getLoopbackAddress(), properties.getNotificationPort());
                        }
                        
                        Thread receiveNotificationThread = new Thread(receiveNotification);
                        receiveNotificationThread.start();
                        
                        // start timeout
                        automaticLogout = new AutomaticLogout(600000, in, out, gson, username, loggedIn, receiveNotification);
                        Thread timeouThread = new Thread(automaticLogout);
                        timeouThread.start();
                        break;
                    case Costants.LOGOUT:
                        loggedIn.set(false);

                        automaticLogout.stop();// stop timeout
                        automaticLogout = null;

                        receiveNotification.stop();
                        receiveNotification = null;
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error checking response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            if (socket != null) socket.close(); // Close socket
            if (out != null) out.close(); // Close output stream
            if (in != null) in.close(); // Close input stream
            if (scanner != null) scanner.close(); // Close scanner
            if (automaticLogout != null) automaticLogout.stop();
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String scanField(String field) {
        String res = "";
        while (true) {
            System.out.println("Enter " + field); // Prompt user to enter the field
            res = scanner.nextLine(); // Read user input
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
            String type = scanField("type"); // Scan the type of order
            if (type.equals(Costants.ASK) || type.equals(Costants.BID)) {
                return type;
            } else {
                System.out.println("type must be 'ask' or 'bid'"); // Type must be 'ask' or 'bid'
            }
        }
    }

    public String scanIntField(String strInt) {
        String res = "";
        while (true) {
            res = scanField(strInt); // Scan the integer field
            if (isInt(res)) {
                break;
            } else {
                System.out.println("size and price must be integers"); // Size and price must be integers
            }
        }
        return res;
    }

    private boolean isInt(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str); // Try to parse the string as an integer
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        new ClientMain(); // Create a new instance of ClientMain
    }
}