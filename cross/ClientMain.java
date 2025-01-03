package cross;

import com.google.gson.*;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    private MyProperties properties;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Scanner scanner;
    private Gson gson;

    // per controllo su logout
    private String username;
    private boolean loggedIn;

    public ClientMain(){
        try{
            properties = new MyProperties("client.properties");

            socket = new Socket(properties.getServerIP(), properties.getPort());
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            scanner = new Scanner(System.in);
            gson = new Gson();

            username = "";
            loggedIn = false;

            writeMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkResponse (String line, String username) {
            try {
                String response = in.readUTF();
                ResponseStatus responseStatus = gson.fromJson(response, ResponseStatus.class);

                int responseCode = responseStatus.getResponseCode();
                String errorMessage = responseStatus.getErrorMessage();

                // Print response received from server
                if (!line.equals(properties.getStopString())){
                    System.out.println(responseCode + " - " + errorMessage);
                }

                if (responseCode == 100){
                    switch (line) {
                        case "login":
                            this.username = username;
                            loggedIn = true;
                            break;
                        case "logout":
                            this.username = "";
                            loggedIn = false;
                            break;
                        default:
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void writeMessages() throws IOException {
        System.out.println("Type " + properties.getStopString()+ " to stop");
        String line = "";
        String username = "";
        String password = "";
        String old_password = "";
        String new_password = "";
        while(!line.equals(properties.getStopString())){

            // Manage cases
            if (!loggedIn) {
                System.out.println("Azioni possibili: (##, register, updateCredentials, login)");

                line = scanner.nextLine();

                switch (line) {
                    case "register":
                        out.writeUTF(line);
                        username = scanField("username");
                        password = scanField("password"); 
                        register(username, password);
                        checkResponse(line, username);
                        break;
                    case "updateCredentials":
                        out.writeUTF(line);
                        username = scanField("username");
                        old_password = scanField("old password");
                        new_password = scanField("new password");
                        updateCredentials(username, old_password, new_password);
                        checkResponse(line, username);
                        break;
                    case "login":
                        out.writeUTF(line);
                        username = scanField("username");
                        password = scanField("password");               
                        login(username, password);
                        checkResponse(line, username);
                        break;
                    default:
                        if (!line.equals(properties.getStopString())){
                            System.out.println("Azione non riconosciuta");
                        }
                        else {
                            out.writeUTF(line);
                        }
                        break;
                }
            }
            else if (loggedIn) {
                System.out.println("Azioni possibili: (##, logout)");

                line = scanner.nextLine();

                switch (line) {
                    case "logout":
                        out.writeUTF(line);
                        logout(this.username);
                        checkResponse(line, username);
                        break;
                    default:
                        System.out.println("Azione non riconosciuta");
                        break;
                }
            }
        }
        close();
    }

    private void close() throws IOException {
        socket.close();
        out.close();
        scanner.close();
    }

    // in MyUtils
    private void sendString (String jsonRequest) {
        try {
            // Send JSON to the Server
            out.writeUTF(jsonRequest);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String scanField (String field) {
        String res = "";
        while (true){
            System.out.println("Enter " + field);
            res = scanner.nextLine();
            if (res.equals(""))
                System.out.println(field + " cannot be empty");
            else break;
        }
        return res;
    }

    private void register (String username, String password) {    
        RegistrationRequest reg = RequestFactory.createRegistrationRequest(username, password);
        String jsonReg = gson.toJson(reg);

        sendString(jsonReg); // send Request
    }

    private void updateCredentials(String username, String old_password, String new_password) {

        UpdateCredentialsRequest update = RequestFactory.createUpdateCredentialsRequest(username, old_password, new_password);
        String jsonUpdate = gson.toJson(update);

        sendString(jsonUpdate); // send Request
    }

    private void login (String username, String password) {
        LoginRequest login = RequestFactory.createLoginRequest(username, password);
        String jsonLogin = gson.toJson(login);

        sendString(jsonLogin); // send Request
    }

    private void logout (String username){
        LogoutRequest logout = RequestFactory.createLogoutRequest();
        String jsonLogout = gson.toJson(logout);

        sendString(jsonLogout); // send Request

        // logout.Values is an empty obj so we need to send username separately
        sendString(username);
    }

    public static void main(String[] args) {
        new ClientMain();
    }
}