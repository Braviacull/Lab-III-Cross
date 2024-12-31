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

public class ServerThread implements Runnable {
    private final Socket clientSocket;
    private final String stopString;
    private DataInputStream in;
    private DataOutputStream out;

    public ServerThread(Socket socket, String stopString) {
        this.clientSocket = socket;
        this.stopString = stopString;
    }

    public void run (){
        try {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("ServerMain listening");
            String line = "";
            while (!line.equals(stopString)) {
                line = in.readUTF();
                System.out.println(line);
                out.writeUTF("Received");
                out.flush();
                if (line.compareTo("register") == 0){
                    handleregister();
                }
                else if (line.compareTo("updateCredentials") == 0){
                    handleupdateCredentials();
                }
            }
            in.close();
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, User> getUserHashMap (Gson gson) {
        HashMap<String, User> users = new HashMap<>();
        try (FileReader reader = new FileReader("users.json")) {
            Type userMapType = new TypeToken<HashMap<String, User>>(){}.getType();
            users = gson.fromJson(reader, userMapType);
        } catch (IOException e) {
            // File not found, will create a new one
        }
        return users;
    }

    private void handleregister() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            HashMap<String, User> users = new HashMap<>();
            ResponseStatus responseStatus;

            // receive JSON from the ClientMain
            String json = in.readUTF();

            // deserialize the json in a User obj
            RegistrationRequest reg = gson.fromJson(json, RegistrationRequest.class);

            // get values (username, password) from reg
            RegistrationRequest.Values values = reg.getValues();
            User user = values.getUser();

            // Read existing users from the file, if the file is not empty
            File file = new File("users.json");
            if (file.length() != 0) {
                users = getUserHashMap(gson);
            }

            // Update the map with the received user
            if (users.containsKey(user.getUsername())){
                responseStatus = new ResponseStatus(102, reg);
            }
            else {
                users.put(user.getUsername(), user);
                System.out.println("User registered successfully");
                // update users.json with the new registered user
                try (FileWriter writer = new FileWriter("users.json")) {
                    gson.toJson(users, writer);
                }
                responseStatus = new ResponseStatus(100, reg);
            }

            // Send responseStatus to the Client
            String jsonResponseStatus = gson.toJson(responseStatus);
            out.writeUTF(jsonResponseStatus);
            out.flush();

        } catch (IOException e) {
            System.err.println("Error during register: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleupdateCredentials () {
        // UN UTENTE ATTUALMENTE LOGGATO NON PUÒ AGGIORNARE LA PROPRIA PASSWORD
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            HashMap<String, User> users = new HashMap<>();
            ResponseStatus responseStatus;

            // receive JSON from the ClientMain
            String json = in.readUTF(); // updateCredentialsRequest

            // deserialize the json in a User obj
            UpdateCredentialsRequest update = gson.fromJson(json, UpdateCredentialsRequest.class);

            // get values (username, old_password, new_password) from reg
            UpdateCredentialsRequest.Values values = update.getValues();

            // LA NUOVA PASSWORD NON PUÒ ESSERE UGUALE ALLA PRECEDENTE
            if (values.comparePasswords()) {
                System.out.println("passwords are equal");
                responseStatus = new ResponseStatus(103, update);
            }
            else {

                // get the hashMap containing the registered users from users.json
                File file = new File("users.json");
                if (file.length() != 0) {
                    users = getUserHashMap(gson);
                }

                //get old_user from values
                User old_user = values.getOldUser();

                if (users.containsKey(old_user.getUsername())) {
                    // OLD PASSWORD MUST MATCH WITH THE ONE IN users.json
                    User hashUser = users.get(old_user.getUsername());

                    if (gson.toJson(hashUser).equals(gson.toJson(old_user))) {
                        System.out.println("passwords MATCHES");

                        // get new_user from values
                        User new_user = values.getNewUser();

                        // replace the old user in the hashMap with the updated user
                        users.put(new_user.getUsername(), new_user);

                        // update users.json
                        try (FileWriter writer = new FileWriter("users.json")) {
                            gson.toJson(users, writer);
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
            }

            // Send responseStatus to the Client
            String jsonResponseStatus = gson.toJson(responseStatus);
            out.writeUTF(jsonResponseStatus);
            out.flush();

        } catch (IOException e) {
            System.err.println("Error during register: " + e.getMessage());
            e.printStackTrace();
        }
    }
}