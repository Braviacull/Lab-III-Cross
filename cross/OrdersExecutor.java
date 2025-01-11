package cross;

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
}
