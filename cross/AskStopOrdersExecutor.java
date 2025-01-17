package cross;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

public class AskStopOrdersExecutor extends OrdersExecutor implements Runnable {
    public int bidMarketPrice;
    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map;
    
    AskStopOrdersExecutor (ServerMain serverMain) {
        super(serverMain.getOrderBook(), serverMain.getUserIpPortMap(), serverMain.getGson());
        map = orderBook.getAskMapStop();
    }

    public boolean calculateConditionToWait(Integer bidMarketPrice) {
        if (bidMarketPrice == null) {
            bidMarketPrice = orderBook.getBidMarketPrice(orderBook.getBidMap());
            this.bidMarketPrice = bidMarketPrice;
        }
        return bidMarketPrice < orderBook.getAskMarketPrice(orderBook.getAskMapStop());
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

            if (orderBook.getAskMapStop().isEmpty()) {
                throw new IllegalStateException("map should not be empty after while condition, Wrong logic");
            }

            while (!calculateConditionToWait(this.bidMarketPrice)){
                // non c'é bisogno di sincronizzare dato che sono l'unico thread che accede a questa mappa 
                Integer price = orderBook.getAskMarketPrice(map);
                ConcurrentLinkedQueue<Order> queue = map.get(price);
                Iterator<Order> iterator = queue.iterator();
                iterate (iterator);
                map.remove(price);
            }

        }
    }
}
