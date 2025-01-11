package cross;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.gson.*;

public class BidStopOrdersExecutor extends OrdersExecutor implements Runnable {
    public int askMarketPrice;
    
    BidStopOrdersExecutor (OrderBook orderBook, Gson gson) {
        super(orderBook, gson);
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
        while (true) {
            synchronized (this) {
                while (calculateConditionToWait(null)) {
                    myWait();
                }
            }
            System.out.println("Risvegliato");

            if (orderBook.getBidMapStop().isEmpty()) {
                System.err.println("map should not be empty after while condition, Wrong logic");
                break;
            }

            while (!calculateConditionToWait(this.askMarketPrice)){
                Integer price = orderBook.getBidMarketPrice(orderBook.getBidMapStop());
                ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = orderBook.getBidMapStop();
                ConcurrentLinkedQueue<Order> queue = map.get(price);
                Iterator<Order> iterator = queue.iterator();
                iterate (iterator);
                map.remove(price);
            }
            System.out.println("uscito dal while");

            MyUtils.printMap(orderBook.getBidMapStop());
            MyUtils.printMap(orderBook.getAskMap());

            MyUtils.updateJson(Costants.BID_MAP_STOP_FILE, orderBook.getBidMapStop(), gson);
            MyUtils.updateJson(Costants.BID_MAP_TEMP_STOP_FILE, new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> (), gson);
        }
    }
}
