package cross;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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
            System.out.println("Server listening");
            String line = "";
            while (!line.equals(stopString)) {
                line = in.readUTF();
                System.out.println(line);
                out.writeUTF("Ricevuto");
                out.flush();
            }
            in.close();
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}