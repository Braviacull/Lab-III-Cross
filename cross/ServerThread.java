package cross;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Type;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;

public class ServerThread implements Runnable {
    private final Socket clientSocket;
    private final String stopString;
    private DataInputStream in;
    private DataOutputStream out;
    private Gson gson;
    private ResponseStatus responseStatus;
    private ConcurrentHashMap<String, User> usersMap;
    private ConcurrentHashMap<String, User> usersLogMap;
    private String username;
    private Boolean loggedIn;

    public ServerThread(Socket socket, String stopString, ConcurrentHashMap<String, User> usersMap, Gson gson) {
        clientSocket = socket;
        this.stopString = stopString;
        this.gson = gson;
        this.usersMap = usersMap;
        this.usersLogMap = new ConcurrentHashMap<String, User>();
        username = "";
        loggedIn = false;
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
                        out.writeUTF(gson.toJson(responseStatus));
                        break;
                    case "updateCredentials":
                        handleupdateCredentials();
                        out.writeUTF(gson.toJson(responseStatus));
                        break;
                    case "login":
                        handleLogin();
                        out.writeUTF(gson.toJson(responseStatus));
                        break;
                    case "logout":
                        handleLogout();
                        out.writeUTF(gson.toJson(responseStatus));
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

    private synchronized void updateUserslogJson(User user) {
        usersLogMap = loadMapFromJson("usersLogMap.json");
        usersLogMap.put(user.getUsername(), user);
        try (FileWriter writer = new FileWriter("usersLogMap.json")) {
            gson.toJson(usersLogMap, writer);
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

            //get the User obj
            User user = values.getUser();

            // Update the map with the received user
            if (usersMap.containsKey(user.getUsername())){
                responseStatus = new ResponseStatus(102, reg);
            }
            else {
                usersMap.put(user.getUsername(), user);
                updateUserslogJson(user);
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
            System.err.println("Error during register: " + e.getMessage());
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
                    updateUserslogJson(new_user);
                    
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
            System.err.println("Error during register: " + e.getMessage());
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
            System.err.println("Error during register: " + e.getMessage());
            e.printStackTrace();
        }
    }
}