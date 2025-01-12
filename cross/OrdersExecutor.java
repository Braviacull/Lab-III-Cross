package cross;

import java.util.Iterator;

public class OrdersExecutor {
    public OrderBook orderBook;

    OrdersExecutor (OrderBook orderBook) {
        this.orderBook = orderBook;
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
            int size = MyUtils.transaction(stopOrder.getSize(), stopOrder.getType(), orderBook);
            if (size == 0) {
                System.out.println("stop order " + stopOrder.getId() + " ESEGUITO");
            }
            else {
                System.out.println("stop order " + stopOrder.getId() + " SCARTATO");
            }
        }
    }
}
