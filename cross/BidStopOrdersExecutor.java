package cross;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

public class BidStopOrdersExecutor extends OrdersExecutor implements Runnable {
    public int askMarketPrice;
    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map;
    
    BidStopOrdersExecutor (ServerMain serverMain) {
        super(serverMain.getOrderBook(), serverMain.getUserIpPortMap(), serverMain.getGson());
        map = orderBook.getBidMapStop();
    }

    public boolean calculateConditionToWait(Integer askMarketPrice) {
        if (askMarketPrice == null) {
            askMarketPrice = orderBook.getAskMarketPrice(orderBook.getAskMap());
            this.askMarketPrice = askMarketPrice;
        }
        return askMarketPrice > orderBook.getBidMarketPrice(orderBook.getBidMapStop());
    }

    public void run () {
        while (running.get()) {
            synchronized (this) {
                while (running.get() && calculateConditionToWait(null)) {
                    myWait();
                }
            }

            if (!running.get()) {
                return;
            }

            if (orderBook.getBidMapStop().isEmpty()) {
                throw new IllegalStateException("map should not be empty after while condition, Wrong logic");
            }

            while (!calculateConditionToWait(this.askMarketPrice)){
                // non c'é bisogno di sincronizzare dato che sono l'unico thread che accede a questa mappa 
                Integer price = orderBook.getBidMarketPrice(map);
                ConcurrentLinkedQueue<Order> queue = map.get(price);
                Iterator<Order> iterator = queue.iterator();
                iterate (iterator);
                map.remove(price);
            }
        }
    }
}
