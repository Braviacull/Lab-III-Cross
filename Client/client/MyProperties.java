package client;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

// La classe MyProperties gestisce la lettura e la scrittura delle proprietà di configurazione
public class MyProperties {
    private Properties properties; // Oggetto Properties per gestire le proprietà
    private String propertiesFilePath; // Percorso del file delle proprietà
    private String serverIP; // Indirizzo IP del server
    private int port; // Porta del server
    private String stopString; // Stringa di stop del server
    private int next_id; // Prossimo ID del server
    private int notificationPort; // Porta per le notifiche
    private int timeout; // Timeout per il logout automatico
    private int period; // Periodo dopo il quale si persistono i dati sul server (scrittura sui file json)
    private int await; // Tempo di attesa in secondi per i serverThread
    private int periodicPingTimeout; // Timeout per il ping periodico

    // Costruttore che carica le proprietà dal file specificato
    public MyProperties(String fileName) {
        this.propertiesFilePath = fileName;
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(fileName)) {
            properties.load(fis);
            // Campi in comune tra server.properties e client.properties
            port = Integer.parseInt(properties.getProperty(Costants.SERVER_PORT));
            stopString = properties.getProperty(Costants.SERVER_STOP_STRING);
            notificationPort = Integer.parseInt(properties.getProperty(Costants.NOTIFICATION_PORT));
            
            // Campi non in comune
            if (properties.getProperty(Costants.SERVER_IP) != null) {
                serverIP = properties.getProperty(Costants.SERVER_IP);
            }

            if (properties.getProperty(Costants.SERVER_NEXT_ID) != null) {
                next_id = Integer.parseInt(properties.getProperty(Costants.SERVER_NEXT_ID));
            }

            if (properties.getProperty(Costants.TIMEOUT) != null) {
                timeout = Integer.parseInt(properties.getProperty(Costants.TIMEOUT));
            }

            if (properties.getProperty(Costants.PERIOD) != null) {
                period = Integer.parseInt(properties.getProperty(Costants.PERIOD));
            }

            if (properties.getProperty(Costants.AWAIT_SECONDS) != null) {
                await = Integer.parseInt(properties.getProperty(Costants.AWAIT_SECONDS));
            }

            if (properties.getProperty(Costants.PERIODIC_PING_TIMEOUT) != null) {
                periodicPingTimeout = Integer.parseInt(properties.getProperty(Costants.PERIODIC_PING_TIMEOUT));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Metodo sincronizzato per impostare il prossimo ID e salvare le proprietà
    public synchronized void setNextId(int next_id) {
        this.next_id = next_id;
        properties.setProperty(Costants.SERVER_NEXT_ID, Integer.toString(next_id));
        try (FileOutputStream out = new FileOutputStream(propertiesFilePath)) {
            properties.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodi getter per ottenere le proprietà
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

    public int getNotificationPort() {
        return notificationPort;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getPeriod() {
        return period;
    }

    public int getAwaitSeconds() {
        return await;
    }

    public int getPeriodicPingTimeout() {
        return periodicPingTimeout;
    }
}