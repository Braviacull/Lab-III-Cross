package cross;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

// La classe PeriodicUpdate implementa Runnable per aggiornare periodicamente i file JSON
public class PeriodicUpdate implements Runnable {
    private final Object lock = new Object(); // Oggetto lock per la sincronizzazione
    private AtomicBoolean running; // Flag per controllare se il thread è in esecuzione
    private final long period; // Periodo di aggiornamento
    private ServerMain serverMain; // Riferimento a ServerMain

    // Costruttore che inizializza il periodo e il serverMain
    public PeriodicUpdate(long period, ServerMain serverMain) {
        this.period = period;
        this.serverMain = serverMain;
        this.running = new AtomicBoolean(true);
    }

    // Metodo run che viene eseguito quando il thread viene avviato
    public void run() {
        while (running.get()) {
            synchronized (lock) {
                try {
                    // Attende per il periodo specificato
                    lock.wait(period);
                    // Esegue l'aggiornamento dei file JSON
                    update();
                } catch (InterruptedException e) {
                    // Interrompe il thread in caso di eccezione
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Metodo per aggiornare i file JSON
    public void update() {
        OrderBook orderBook = serverMain.getOrderBook();
        // Aggiorna i file JSON delle mappe degli ordini
        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getAskMap()), orderBook.getAskMap());
        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getBidMap()), orderBook.getBidMap());
        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getAskMapStop()), orderBook.getAskMapStop());
        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getBidMapStop()), orderBook.getBidMapStop());

        // Aggiorna il file JSON degli utenti
        ConcurrentHashMap<String, User> usersMap = serverMain.getUsersMap();
        ServerMain.updateJson(Costants.USERS_MAP_FILE, usersMap);

        // Aggiorna il file JSON dello storico ordini
        ConcurrentLinkedQueue<Trade> storicoOrdini = serverMain.getStoricoOrdini();
        StoricoOrdini.updateJson(Costants.STORICO_ORDINI, storicoOrdini);
    }

    // Metodo per fermare il thread
    public void stop() {
        synchronized (lock) {
            running.set(false);
            lock.notify();
        }
    }
}