package cross;

import com.google.gson.*;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

public class ClientMain {
    private String serverIP;
    private int port;
    private String stopString;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Scanner scanner;
    private Gson gson;


    public ClientMain(){
        try{
            getProperties("client.properties");

            socket = new Socket(serverIP, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            scanner = new Scanner(System.in);

            gson = new Gson();

            writeMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getProperties(String name) {
        try {
            Properties properties = new Properties();
            FileInputStream fis = new FileInputStream(name);
            properties.load(fis);

            serverIP = properties.getProperty("server.ip");
            port = Integer.parseInt(properties.getProperty("server.port"));
            stopString = properties.getProperty("server.stop_string");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void close() throws IOException {
        socket.close();
        out.close();
        scanner.close();
    }

    private void writeMessages() throws IOException {
        System.out.println("Type " + stopString + " to stop");
        String line = "";
        while(!line.equals(stopString)){
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
            if (!line.equals(stopString))
                System.out.println(in.readUTF());
        }
        close();
    }

    private void send_request (String jsonRequest) {
        try {
            // Send JSON to the Server
            out.writeUTF(jsonRequest);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String scan_field (String field) {
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
        String username = scan_field("username");
        String password= scan_field("password");        

        RegistrationRequest reg = RequestFactory.createRegistrationRequest(username, password);
        String jsonReg = gson.toJson(reg);

        send_request(jsonReg);
    }

    private void updateCredentials() {
        String username = scan_field("username");
        String old_password= scan_field("old password");
        String new_password= scan_field("new password");

        UpdateCredentialsRequest update = RequestFactory.createUpdateCredentialsRequest(username, old_password, new_password);
        String jsonUpdate = gson.toJson(update);

        send_request(jsonUpdate);
    }

    public static void main(String[] args) {
        new ClientMain();
    }
}