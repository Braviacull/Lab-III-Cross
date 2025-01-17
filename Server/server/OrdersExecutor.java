package server;

import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;

// Classe per gestire gli Ordini (due classi la estendono (AskStopOrdersExecutor e BidStopOrdersExecutor) e sono gli unici punti in cui la classe é usata)
public class OrdersExecutor {
    public OrderBook orderBook;
    private ConcurrentHashMap<String, IpPort> userIpPortMap; // Mappa degli utenti con i loro indirizzi IP e porte (per inviare notifiche)
    private Gson gson;
    public AtomicBoolean running;

    OrdersExecutor (OrderBook orderBook, ConcurrentHashMap<String, IpPort> userIpPortMap, Gson gson) {
        this.orderBook = orderBook;
        this.userIpPortMap = userIpPortMap;
        this.gson = gson;
        this.running = new AtomicBoolean(true);
    }

    public void stop() {
        running.set(false);
        notifyOrdersExecutor();
    }

    public void notifyOrdersExecutor () {
        synchronized (this) {
            notify();
        }
    }

    public void myWait () {
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void iterate (Iterator<Order> iterator) {
        while (iterator.hasNext()) {
            Order stopOrder = iterator.next();
            int limit = 0;
            switch (stopOrder.getType()) {
                case Costants.ASK:
                    break; // voglio vendere a chiunque
                case Costants.BID:
                    limit = Integer.MAX_VALUE; // voglio comprare da chiunque non importa se mi fa un prezzo alto
                    break;
                default:
                    throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }
            int size = MyUtils.transaction(stopOrder.getSize(), limit, stopOrder.getType(), Costants.STOP, orderBook, userIpPortMap, gson);
            if (size == 0) {
                System.out.println("stop order " + stopOrder.getId() + " ESEGUITO");
                Trade trade = new Trade(stopOrder.getId(), stopOrder.getType(), Costants.STOP, stopOrder.getSize(), stopOrder.getPrice(), (int) Instant.now().getEpochSecond());
                MyUtils.sendNotification(userIpPortMap.get(stopOrder.getUsername()), new Notification(trade), gson);
            }
            else {
                System.out.println("stop order " + stopOrder.getId() + " SCARTATO");
                // viene notificata solamente l'avvenuta esecuzione
            }
        }
    }
}
