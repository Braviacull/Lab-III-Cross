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
        System.out.println("askMarketPrice: " + askMarketPrice + " > " + "bidStopMarketPrice : " + orderBook.getBidMarketPrice(orderBook.getBidMapStop()));
        System.out.println(askMarketPrice > orderBook.getBidMarketPrice(orderBook.getBidMapStop()));
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
                System.out.println("BidStopOrdersExecutor stopped");
                return;
            }

            System.out.println("Risvegliato");

            if (orderBook.getBidMapStop().isEmpty()) {
                throw new IllegalStateException("map should not be empty after while condition, Wrong logic");
            }

            while (!calculateConditionToWait(this.askMarketPrice)){
                Integer price = orderBook.getBidMarketPrice(map);
                ConcurrentLinkedQueue<Order> queue = map.get(price);
                Iterator<Order> iterator = queue.iterator();
                iterate (iterator);
                map.remove(price);
            }
            System.out.println("uscito dal while");

            MyUtils.printMap(orderBook.getBidMapStop());
            MyUtils.printMap(orderBook.getAskMap());

        }
    }
}
