package cross;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

public class AskStopOrdersExecutor extends OrdersExecutor implements Runnable {
    public int bidMarketPrice;
    public ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map;
    
    AskStopOrdersExecutor (OrderBook orderBook) {
        super(orderBook);
        map = orderBook.getAskMapStop();
    }

    public boolean calculateConditionToWait(Integer bidMarketPrice) {
        if (bidMarketPrice == null) {
            bidMarketPrice = orderBook.getBidMarketPrice(orderBook.getBidMap());
            this.bidMarketPrice = bidMarketPrice;
        }
        System.out.println("bidMarketPrice: " + bidMarketPrice + " < " + "askStopMarketPrice : " + orderBook.getAskMarketPrice(orderBook.getAskMapStop()));
        System.out.println(bidMarketPrice < orderBook.getAskMarketPrice(orderBook.getAskMapStop()));
        return bidMarketPrice < orderBook.getAskMarketPrice(orderBook.getAskMapStop());
    }

    public void run () {
        while (true) {
            synchronized (this) {
                while (calculateConditionToWait(null)) {
                    myWait();
                }
            }
            System.out.println("Risvegliato");

            if (orderBook.getAskMapStop().isEmpty()) {
                throw new IllegalStateException("map should not be empty after while condition, Wrong logic");
            }

            while (!calculateConditionToWait(this.bidMarketPrice)){
                Integer price = orderBook.getAskMarketPrice(map);
                ConcurrentLinkedQueue<Order> queue = map.get(price);
                Iterator<Order> iterator = queue.iterator();
                iterate (iterator);
                map.remove(price);
            }
            System.out.println("uscito dal while");

            MyUtils.printMap(orderBook.getAskMapStop());
            MyUtils.printMap(orderBook.getBidMap());

            orderBook.updateJson(orderBook.getJsonFileNameFromMap(orderBook.getAskMapStop()), orderBook.getAskMapStop());
        }
    }
}
