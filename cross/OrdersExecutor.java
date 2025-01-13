package cross;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class OrdersExecutor {
    public OrderBook orderBook;
    private ConcurrentHashMap<String, IpPort> userIpPortMap;

    OrdersExecutor (OrderBook orderBook, ConcurrentHashMap<String, IpPort> userIpPortMap) {
        this.orderBook = orderBook;
        this.userIpPortMap = userIpPortMap;
    }

    public void myNotify () {
        synchronized (this) {
            notify();
        }
    }

    public void myWait () {
        try {
            wait();
            System.out.println("Notifica ricevuta");
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
            int size = MyUtils.transaction(stopOrder.getSize(), limit, stopOrder.getType(), orderBook, userIpPortMap);
            if (size == 0) {
                System.out.println("stop order " + stopOrder.getId() + " ESEGUITO");
                MyUtils.sendNotification(userIpPortMap.get(stopOrder.getUsername()), stopOrder.getId());
            }
            else {
                System.out.println("stop order " + stopOrder.getId() + " SCARTATO");
                MyUtils.sendNotification(userIpPortMap.get(stopOrder.getUsername()), stopOrder.getId());
            }
        }
    }
}
