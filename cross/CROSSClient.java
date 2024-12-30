package cross;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

public class CROSSClient {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Scanner scanner;
    private String serverIP;
    private int port;
    private String stopString;


    public CROSSClient(){
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
        System.out.println("Azioni possibili: (register, login)");
        String line = "";
        while(!line.equals(stopString)){
            line = scanner.nextLine();
            out.writeUTF(line);
            System.out.println(in.readUTF());
        }
        close();
    }

    private void close() throws IOException {
        socket.close();
        out.close();
        scanner.close();
    }

    public static void main(String[] args) {
        new CROSSClient();
    }
}