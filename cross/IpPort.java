package cross;

import java.net.InetAddress;

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