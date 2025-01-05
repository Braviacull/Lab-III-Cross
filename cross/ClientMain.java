package cross;

import com.google.gson.*;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    private MyProperties properties; // Properties object to read configuration
    private Socket socket; // Socket for client-server communication
    private DataOutputStream out; // Output stream to send data to the server
    private DataInputStream in; // Input stream to receive data from the server
    private Scanner scanner; // Scanner to read user input
    private Gson gson; // Gson object for JSON serialization/deserialization

    // for login-logout control
    private String username; // Username of the logged-in user
    private boolean loggedIn; // Flag to check if the user is logged in

    public ClientMain() {
        try {
            properties = new MyProperties(Costants.CLIENT_PROPERTIES_FILE); // Load properties from file

            socket = new Socket(properties.getServerIP(), properties.getPort()); // Connect to the server
            out = new DataOutputStream(socket.getOutputStream()); // Initialize output stream
            in = new DataInputStream(socket.getInputStream()); // Initialize input stream
            scanner = new Scanner(System.in); // Initialize scanner for user input
            gson = new GsonBuilder().setPrettyPrinting().create(); // Initialize Gson object

            username = ""; // Initialize username as empty
            loggedIn = false; // Initialize loggedIn flag as false

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
            if (!loggedIn) {
                System.out.println("Possible actions: (exit, register, updateCredentials, login)");

                line = scanner.nextLine();

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
            } else if (loggedIn) {
                System.out.println("Possible actions: (exit, logout, insertLimitOrder, insertMarketOrder)");

                line = scanner.nextLine();

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
                    default:
                        defaultBehavior(line);
                        break;
                }
            }
        }
        close(); // Close resources when done
    }

    private void handleRegister() throws IOException {
        out.writeUTF(Costants.REGISTER);
        String username = scanField("username"); // Scan username
        String password = scanField("password"); // Scan password
        RegistrationRequest reg = RequestFactory.createRegistrationRequest(username, password); // Create registration request
        String jsonReg = gson.toJson(reg); // Convert request to JSON
        sendString(jsonReg); // Send JSON request to the server
        checkResponse(Costants.REGISTER, username); // Check server response
    }

    private void handleUpdateCredentials() throws IOException {
        out.writeUTF(Costants.UPDATE_CREDENTIALS);
        String username = scanField("username"); // Scan username
        String oldPassword = scanField("old password"); // Scan old password
        String newPassword = scanField("new password"); // Scan new password
        UpdateCredentialsRequest update = RequestFactory.createUpdateCredentialsRequest(username, oldPassword, newPassword); // Create update credentials request
        String jsonUpdate = gson.toJson(update); // Convert request to JSON
        sendString(jsonUpdate); // Send JSON request to the server
        checkResponse(Costants.UPDATE_CREDENTIALS, username); // Check server response
    }

    private void handleLogin() throws IOException {
        out.writeUTF(Costants.LOGIN);
        String username = scanField("username"); // Scan username
        String password = scanField("password"); // Scan password
        LoginRequest login = RequestFactory.createLoginRequest(username, password); // Create login request
        String jsonLogin = gson.toJson(login); // Convert request to JSON
        sendString(jsonLogin); // Send JSON request to the server
        checkResponse(Costants.LOGIN, username); // Check server response
    }

    private void handleLogout() throws IOException {
        out.writeUTF(Costants.LOGOUT);
        LogoutRequest logout = RequestFactory.createLogoutRequest(); // Create logout request
        String jsonLogout = gson.toJson(logout); // Convert request to JSON
        sendString(jsonLogout); // Send JSON request to the server
        sendString(username); // Send username separately as logout.Values is an empty object
        checkResponse(Costants.LOGOUT, username); // Check server response
    }

    private void handleInsertLimitOrder() throws IOException {
        out.writeUTF(Costants.INSERT_LIMIT_ORDER);
        String type = scanType(); // Scan the type of order (ask or bid)
        String size = scanIntField("size"); // Scan the size of the order
        String price = scanIntField("price"); // Scan the price of the order
        InsertLimitOrderRequest insertLimitOrderRequest = RequestFactory.createInsertLimitOrderRequest(type, Integer.parseInt(size), Integer.parseInt(price)); // Create limit order request
        String json = gson.toJson(insertLimitOrderRequest); // Convert request to JSON
        sendString(json); // Send JSON request to the server
        receiveIdOrder(); // Receive the order ID from the server
    }

    private void handleInsertMarketOrder() throws IOException {
        out.writeUTF(Costants.INSERT_MARKET_ORDER);
        String type = scanType(); // Scan the type of order (ask or bid)
        String size = scanIntField("size"); // Scan the size of the order
        InsertMarketOrderRequest insertMarketOrderRequest = RequestFactory.createInsertMarketOrderRequest(type, Integer.parseInt(size)); // Create market order request
        String json = gson.toJson(insertMarketOrderRequest); // Convert request to JSON
        sendString(json); // Send JSON request to the server
        receiveIdOrder(); // Receive the order ID from the server
    }

    private void receiveIdOrder() {
        try {
            int id = in.readInt(); // Read the order ID from the input stream
            if (id == -1) {
                System.out.println("Order aborted");
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
                    case Costants.LOGIN:
                        this.username = username; // Set username
                        loggedIn = true; // Set loggedIn flag to true
                        break;
                    case Costants.LOGOUT:
                        this.username = ""; // Clear username
                        loggedIn = false; // Set loggedIn flag to false
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
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendString(String jsonRequest) {
        try {
            out.writeUTF(jsonRequest); // Send JSON request to the server
            out.flush(); // Flush the output stream
        } catch (IOException e) {
            System.err.println("Error sending JSON request: " + e.getMessage());
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
        while (true) {
            strInt = scanField(strInt); // Scan the integer field
            if (isInt(strInt)) {
                break;
            } else {
                System.out.println("size and price must be integers"); // Size and price must be integers
            }
        }
        return strInt;
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