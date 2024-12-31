package cross;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.lang.reflect.Type;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.io.File;

public class ServerThread implements Runnable {
    private Socket clientSocket;
    private String stopString;
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
            }
            in.close();
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, RegistrationRequest.Values> getUserHashMap (Gson gson) {
        HashMap<String, RegistrationRequest.Values> users = new HashMap<>();
        try (FileReader reader = new FileReader("users.json")) {
            Type userMapType = new TypeToken<HashMap<String, RegistrationRequest.Values>>(){}.getType();
            users = gson.fromJson(reader, userMapType);
        } catch (IOException e) {
            // File not found, will create a new one
        }
        return users;
    }

    private void handleregister() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            HashMap<String, RegistrationRequest.Values> users = new HashMap<>();
            ResponseStatus responseStatus;

            // receive JSON from the ClientMain
            String json = in.readUTF();

            RegistrationRequest reg = gson.fromJson(json, RegistrationRequest.class);
            // deserialize the json in a User obj
            RegistrationRequest.Values user = reg.getValues();
//            User user = gson.fromJson(json, User.class);

            System.out.println(json);

            // Read existing users from the file, if the file is not empty
            File file = new File("users.json");
            if (file.length() != 0) {
                users = getUserHashMap(gson);
            }

            // Update the map with the received user
            if (users.containsKey(user.getUsername())){
                System.out.println("utente esistente, scegline un altro");
                responseStatus = new ResponseStatus(102);
            }
            else {
                users.put(user.getUsername(), user);
                System.out.println("User registered: " + json);
                // Write the updated map back to the file with pretty printing
                try (FileWriter writer = new FileWriter("users.json")) {
                    gson.toJson(users, writer);
                }
                responseStatus = new ResponseStatus(100);
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