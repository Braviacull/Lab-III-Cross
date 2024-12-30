package cross;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private ServerSocket server;
    private String serverIP;
    private int port;
    private String stopString;

    private ExecutorService connessioni;

    public ServerMain() {
        try {
            getProperties();

            connessioni = Executors.newCachedThreadPool();

            server = new ServerSocket(port, 50, InetAddress.getByName(serverIP));
            System.out.println("Indirizzo del ServerMain: " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());

            acceptConnections();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getProperties() {
        // Proprietà caricate
        try {
            Properties properties = new Properties();
            FileInputStream fis = new FileInputStream("server.properties");
            properties.load(fis);

            serverIP = properties.getProperty("server.ip");
            port = Integer.parseInt(properties.getProperty("server.port"));
            stopString = properties.getProperty("server.stop_string");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void acceptConnections() throws IOException {
        System.out.println("ServerMain started");
        try {
            while (true) {
                Socket clientSocket = server.accept();
                connessioni.execute(new ServerThread(clientSocket, stopString));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            new ServerMain();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}