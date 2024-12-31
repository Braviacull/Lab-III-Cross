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
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Scanner scanner;
    private String serverIP;
    private int port;
    private String stopString;


    public ClientMain(){
        try{
            // Carica le proprietà dal file di configurazione
            Properties properties = new Properties();
            FileInputStream fis = new FileInputStream("server.properties");
            properties.load(fis);

            serverIP = properties.getProperty("server.ip");
            port = Integer.parseInt(properties.getProperty("server.port"));
            stopString = properties.getProperty("server.stop_string");
            // Proprietà caricate

            socket = new Socket(serverIP, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            scanner = new Scanner(System.in);
            writeMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            // Send JSON to the ServerMain
            out.writeUTF(jsonRequest);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void register () {
        String username = "";
        String password= "";
        Gson gson = new Gson();

        System.out.println("Enter username");
        username = scanner.nextLine();
        System.out.println("Enter password");
        password = scanner.nextLine();

        RegistrationRequest reg = RequestFactory.createRegistrationRequest(username, password);
        String jsonReg = gson.toJson(reg);

        try {
            // Send JSON to the ServerMain
            out.writeUTF(jsonReg);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateCredentials() {
        String username = "";
        String old_password= "";
        String new_password= "";
        Gson gson = new Gson();

        System.out.println("Enter username");
        username = scanner.nextLine();
        System.out.println("Enter old password");
        old_password = scanner.nextLine();
        System.out.println("Enter new password");
        new_password = scanner.nextLine();

        UpdateCredentialsRequest update = RequestFactory.createUpdateCredentialsRequest(username, old_password, new_password);
        String jsonUpdate = gson.toJson(update);

        send_request(jsonUpdate);
    }

    private void close() throws IOException {
        socket.close();
        out.close();
        scanner.close();
    }

    public static void main(String[] args) {
        new ClientMain();
    }
}