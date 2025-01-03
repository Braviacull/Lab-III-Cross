package cross;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class MyProperties {
    private Properties properties;
    private String propertiesFilePath;
    private String serverIP;
    private int port;
    private String stopString;
    private int next_id;

    public MyProperties(String fileName) {
        this.propertiesFilePath = fileName;
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(fileName)) {
            properties.load(fis);
            
            serverIP = properties.getProperty("server.ip");
            port = Integer.parseInt(properties.getProperty("server.port"));
            stopString = properties.getProperty("server.stop_string");

            if (properties.getProperty("server.next_id") != null) {
                next_id = Integer.parseInt(properties.getProperty("server.next_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setNextId(int next_id) {
        this.next_id = next_id;
        properties.setProperty("server.next_id", Integer.toString(next_id));
        try (FileOutputStream out = new FileOutputStream(propertiesFilePath)) {
            properties.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getServerIP() {
        return serverIP;
    }

    public int getPort() {
        return port;
    }

    public String getStopString() {
        return stopString;
    }

    public int getNextId() {
        return next_id;
    }
}