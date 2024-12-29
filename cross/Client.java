package cross;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataOutputStream out;
    private Scanner in;
    private String serverIP;
    private int port;
    private String stopString;


    public Client(){
        try{
            // Carica le proprietà dal file di configurazione
            Properties properties = new Properties();
            FileInputStream fis = new FileInputStream("server.properties");
            properties.load(fis);

            serverIP = properties.getProperty("server.ip");
            port = Integer.parseInt(properties.getProperty("server.port"));
            stopString = properties.getProperty("server.stop_string");

            socket = new Socket(serverIP, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new Scanner(System.in);
            writeMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMessages() throws IOException {
        String line = "";
        while(!line.equals(stopString)){
            line = in.nextLine();
            out.writeUTF(line);
        }
        close();
    }

    private void close() throws IOException {
        socket.close();
        out.close();
        in.close();
    }

    public static void main(String[] args) {
        new Client();
    }
}