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
            
            serverIP = properties.getProperty(Costants.SERVER_IP);
            port = Integer.parseInt(properties.getProperty(Costants.SERVER_PORT));
            stopString = properties.getProperty(Costants.SERVER_STOP_STRING);

            if (properties.getProperty(Costants.SERVER_NEXT_ID) != null) {
                next_id = Integer.parseInt(properties.getProperty(Costants.SERVER_NEXT_ID));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setNextId(int next_id) {
        this.next_id = next_id;
        properties.setProperty(Costants.SERVER_NEXT_ID, Integer.toString(next_id));
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