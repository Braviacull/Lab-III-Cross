package cross;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.lang.reflect.Type;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.io.File;
import java.io.FileNotFoundException;

public class ServerThread implements Runnable {
    private final Socket clientSocket;
    private final String stopString;
    private DataInputStream in;
    private DataOutputStream out;
    private Gson gson;
    private HashMap<String, User> usersMap;
    private ResponseStatus responseStatus;

    public ServerThread(Socket socket, String stopString) {
        this.clientSocket = socket;
        this.stopString = stopString;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.usersMap = new HashMap<>();
    }

    private void close () throws IOException {
        in.close();
        out.close();
    }

    public void run (){
        try {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            System.out.println("Server listening");
            String line = "";
            while (!line.equals(stopString)) {
                // receiving line from client
                line = in.readUTF();

                // Manage cases
                switch (line) {
                    case "register":
                        handleregister();
                        break;
                    case "updateCredentials":
                        handleupdateCredentials();
                        break;
                    default:
                        responseStatus = new ResponseStatus();
                        break;
                }
                // Send response
                out.writeUTF(gson.toJson(responseStatus));
            }

            close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // If user.json does not exist, will be created
    private HashMap<String, User> getUserHashMap(Gson gson) {
        HashMap<String, User> usersMap = new HashMap<>();
        File file = new File("usersMap.json");
        if (file.length() != 0) {
            try (FileReader reader = new FileReader("usersMap.json")) {
                Type userMapType = new TypeToken<HashMap<String, User>>(){}.getType();
                usersMap = gson.fromJson(reader, userMapType);
            } catch (FileNotFoundException e) {
                // File not found, will create a new file
                try (FileWriter writer = new FileWriter("usersMap.json")) {
                    gson.toJson(usersMap, writer);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return usersMap;
    }

    private void handleregister() {
        try {
            // receive JSON from the ClientMain
            String json = in.readUTF();

            // deserialize the json
            RegistrationRequest reg = gson.fromJson(json, RegistrationRequest.class);

            // get values (username, password) from reg
            RegistrationRequest.Values values = reg.getValues();

            //get the User obj
            User user = values.getUser();

            // Get the hashmap from usersMap.json
            usersMap = getUserHashMap(gson);

            // Update the map with the received user
            if (usersMap.containsKey(user.getUsername())){
                responseStatus = new ResponseStatus(102, reg);
            }
            else {
                usersMap.put(user.getUsername(), user);
                System.out.println("User registered successfully");
                // update usersMap.json with the new registered user
                try (FileWriter writer = new FileWriter("usersMap.json")) {
                    gson.toJson(usersMap, writer);
                }
                responseStatus = new ResponseStatus(100, reg);
            }
        } catch (IOException e) {
            System.err.println("Error during register: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleupdateCredentials () {
        // UN UTENTE ATTUALMENTE LOGGATO NON PUÒ AGGIORNARE LA PROPRIA PASSWORD
        try {
            // receive JSON from the ClientMain
            String json = in.readUTF(); // updateCredentialsRequest

            // deserialize the json in a User obj
            UpdateCredentialsRequest update = gson.fromJson(json, UpdateCredentialsRequest.class);

            // get values (username, old_password, new_password) from update
            UpdateCredentialsRequest.Values values = update.getValues();

            // Check if the new password is different
            if (values.comparePasswords()) {
                System.out.println("passwords must be different");
                responseStatus = new ResponseStatus(103, update);
                return;
            }
            
            // Get the hashmap from usersMap.json
            usersMap = getUserHashMap(gson);

            //get old_user from values
            User old_user = values.getOldUser();

            // the username of old_user and new_user are the same
            String username = old_user.getUsername();

            // Check if the user is registered
            if (usersMap.containsKey(username)) {
                User hashUser = usersMap.get(username);
                
                // OLD PASSWORD MUST MATCH WITH THE ONE IN usersMap.json
                if (gson.toJson(hashUser).equals(gson.toJson(old_user))) {
                    System.out.println("passwords MATCHES");

                    // get new_user from values
                    User new_user = values.getNewUser();

                    // replace the old user in the hashMap with the updated user
                    usersMap.put(username, new_user);

                    // update usersMap.json
                    try (FileWriter writer = new FileWriter("usersMap.json")) {
                        gson.toJson(usersMap, writer);
                    }
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
}