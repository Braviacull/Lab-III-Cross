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

    public ClientMain(){
        try{
            properties = new MyProperties("client.properties");

            socket = new Socket(properties.getServerIP(), properties.getPort());
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            scanner = new Scanner(System.in);
            gson = new Gson();

            writeMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() throws IOException {
        socket.close();
        out.close();
        scanner.close();
    }

    private void writeMessages() throws IOException {
        System.out.println("Type " + properties.getStopString()+ " to stop");
        String line = "";
        while(!line.equals(properties.getStopString())){
            System.out.println("Azioni possibili: (##, register, updateCredentials)");

            line = scanner.nextLine();

            out.writeUTF(line);

            // Manage cases
            switch (line) {
                case "register":
                    register();
                    break;
                case "updateCredentials":
                    updateCredentials();
                    break;
                default:
                    break;
            }

            // Print response received from server
            if (!line.equals(properties.getStopString()))
                System.out.println(in.readUTF());
        }
        close();
    }

    private void sendRequest (String jsonRequest) {
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

    private void register () {
        String username = scanField("username");
        String password= scanField("password");        

        RegistrationRequest reg = RequestFactory.createRegistrationRequest(username, password);
        String jsonReg = gson.toJson(reg);

        sendRequest(jsonReg);
    }

    private void updateCredentials() {
        String username = scanField("username");
        String old_password= scanField("old password");
        String new_password= scanField("new password");

        UpdateCredentialsRequest update = RequestFactory.createUpdateCredentialsRequest(username, old_password, new_password);
        String jsonUpdate = gson.toJson(update);

        sendRequest(jsonUpdate);
    }

    public static void main(String[] args) {
        new ClientMain();
    }
}