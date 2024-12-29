package cross;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class Server {
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;
    private String serverIP;
    private int port;
    private String stopString;

    public Server() {
        try {
            // Carica le proprietà dal file di configurazione
            Properties properties = new Properties();
            FileInputStream fis = new FileInputStream("server.properties");
            properties.load(fis);

            serverIP = properties.getProperty("server.ip");
            port = Integer.parseInt(properties.getProperty("server.port"));
            stopString = properties.getProperty("server.stop_string");

            server = new ServerSocket(port, 50, InetAddress.getByName(serverIP));
            System.out.println("Indirizzo del server: " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
            iniConnections();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void iniConnections() throws IOException {
        System.out.println("Server started");
        Socket clientSocket = server.accept();
        in = new DataInputStream(clientSocket.getInputStream());
        out = new DataOutputStream(clientSocket.getOutputStream());
        readMessages();
        close();
    }

    private void close() throws IOException {
        in.close();
        out.close();
        server.close();
    }

    private void readMessages() throws IOException {
        System.out.println("Server listening");
        String line = "";
        while (!line.equals(stopString)) {
            line = in.readUTF();
            System.out.println(line);
            out.writeUTF("Ricevuto");
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}