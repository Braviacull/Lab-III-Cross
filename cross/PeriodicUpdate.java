package cross;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeriodicUpdate implements Runnable{
    private final Object lock = new Object();
    private boolean running = true;
    private final long period;
    private ServerMain serverMain;

    public PeriodicUpdate (long period, ServerMain serverMain) {
        this.period = period;
        this.serverMain = serverMain;
    }

    public void run () {
        while (running) {
            synchronized(lock) {
                try {
                    lock.wait(period);
                    System.out.println("UPDATING");
                    update();
                    System.out.println("UPDATE finished");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("periodicUpdate terminato");
    }

    public void update () {
        OrderBook orderBook = serverMain.getOrderBook();
        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getAskMap()), orderBook.getAskMap());
        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getBidMap()), orderBook.getBidMap());
        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getAskMapStop()), orderBook.getAskMapStop());
        orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getBidMapStop()), orderBook.getBidMapStop());

        ConcurrentHashMap<String, User> usersMap = serverMain.getUsersMap();
        ServerMain.updateJson(Costants.USERS_MAP_FILE, usersMap);

        ConcurrentLinkedQueue<Trade> storicoOrdini = serverMain.getStoricoOrdini();
        StoricoOrdini.updateJson(Costants.STORICO_ORDINI_TEMP, storicoOrdini);
    }

    public void exit() {
        running = false;
        synchronized(lock) {
            lock.notify();
        }
    }
}
