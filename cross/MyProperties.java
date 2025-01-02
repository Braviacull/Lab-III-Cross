package cross;

import java.io.FileInputStream;
import java.util.Properties;

public class MyProperties {
    private String serverIP;
    private int port;
    private String stopString;

    public MyProperties (String fileName) {
        try (FileInputStream fis = new FileInputStream(fileName)) {
            Properties properties = new Properties();
            properties.load(fis);
            
            serverIP = properties.getProperty("server.ip");
            port = Integer.parseInt(properties.getProperty("server.port"));
            stopString = properties.getProperty("server.stop_string");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getServerIP () {
        return serverIP;
    }
    
    public int getPort () {
        return port;
    }
    
    public String getStopString () {
        return stopString;
    }
}
