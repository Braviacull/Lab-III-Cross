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

                int orderId = extractOrderId(packet.getData());
                System.out.println("Notification received: Order ID = " + orderId + " executed successfully");
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

    private int extractOrderId(byte[] data) {
        // Assuming the order ID is an integer at the beginning of the data array
        int orderId = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        return orderId;
    }
}