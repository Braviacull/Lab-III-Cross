package cross;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReceiveNotification implements Runnable {
    private InetAddress ipAddress;
    private int port;
    private AtomicBoolean running = new AtomicBoolean(true);
    private DatagramSocket socket;

    public ReceiveNotification(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];

        try (DatagramSocket ds = new DatagramSocket(port, ipAddress)) {
            this.socket = ds;
            while (running.get()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Receive UDP packet

                String json = new String(packet.getData(), 0, packet.getLength(), "UTF-8"); // Convert byte array to string
                System.out.println("Notification received: " + json);
            }
        } catch (IOException e) {
            if (running.get()) {
                e.printStackTrace();
            } else {
                System.out.println("ReceiveNotification thread stopped.");
            }
        }
    }

    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Close the DatagramSocket to interrupt the blocking receive call
        }
    }
}