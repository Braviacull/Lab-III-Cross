package client;

// La classe PeriodicPing implementa Runnable per inviare ping periodici al server
public class PeriodicPing implements Runnable {
    private final Object lock = new Object(); // Oggetto lock per la sincronizzazione
    private boolean running = true; // Flag per controllare se il thread è in esecuzione
    
    private final long timeout; // Timeout per il ping periodico
    private ClientMain clientMain; // Riferimento a ClientMain

    // Costruttore che inizializza il timeout e il clientMain
    public PeriodicPing(long timeout, ClientMain clientMain) {
        this.timeout = timeout;
        this.clientMain = clientMain;
    }

    // Metodo per fermare il thread
    public void stop() {
        synchronized (lock) {
            running = false;
            lock.notify();
        }
    }

    // Metodo run che viene eseguito quando il thread viene avviato
    public void run() {
        while (running) {
            synchronized (lock) {
                try {
                    // Attende per il timeout specificato
                    lock.wait(timeout);
                    if (running) {
                        synchronized (clientMain) {
                            // Verifica se il server è online
                            if (!clientMain.isServerOnline()) {
                                clientMain.getIsServerOnline().set(false);
                                Sync.printlnSync("Server Offline, scrivi qualsiasi cosa per terminare");
                                running = false;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // Interrompe il thread in caso di eccezione
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}