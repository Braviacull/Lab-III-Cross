package cross;

import java.util.Iterator;

import com.google.gson.Gson;

public class OrdersExecutor {
    public OrderBook orderBook;
    public Gson gson;

    OrdersExecutor (OrderBook orderBook, Gson gson) {
        this.orderBook = orderBook;
        this.gson = gson;
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
            int size = MyUtils.transaction(stopOrder.getSize(), stopOrder.getType() , orderBook);
            MyUtils.checkTransaction(size, stopOrder.getType(), orderBook);
        }
    }
}
