package cross;

import java.net.InetAddress;

// La classe IpPort rappresenta una coppia di indirizzo IP e porta
public class IpPort {
    private InetAddress ipAddress;
    private int port;

    public IpPort(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }
}