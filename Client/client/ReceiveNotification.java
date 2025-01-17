package client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

// La classe ReceiveNotification implementa Runnable per ricevere notifiche via UDP
public class ReceiveNotification implements Runnable {
    private InetAddress ipAddress; // Indirizzo IP su cui ricevere le notifiche
    private int port; // Porta su cui ricevere le notifiche
    private AtomicBoolean running = new AtomicBoolean(true); // Flag per controllare se il thread è in esecuzione
    private DatagramSocket socket; // Socket per ricevere i pacchetti UDP

    // Costruttore che inizializza l'indirizzo IP e la porta
    public ReceiveNotification(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    // Metodo run che viene eseguito quando il thread viene avviato
    public void run() {
        byte[] buffer = new byte[1024]; // Buffer per ricevere i dati

        try (DatagramSocket ds = new DatagramSocket(port, ipAddress)) {
            this.socket = ds;
            while (running.get()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Riceve il pacchetto UDP

                String json = new String(packet.getData(), 0, packet.getLength(), "UTF-8"); // Converte l'array di byte in una stringa
                Sync.printlnSync("Notification received: " + json); // Stampa la notifica ricevuta
            }
        } catch (IOException e) {
            if (running.get()) {
                e.printStackTrace();
            }
        }
    }

    // Metodo per fermare il thread
    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Chiude il DatagramSocket per interrompere la chiamata di ricezione bloccante
        }
    }
}