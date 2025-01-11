package cross;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.gson.*;

public class AskStopOrdersExecutor extends OrdersExecutor implements Runnable {
    public int bidMarketPrice;
    
    AskStopOrdersExecutor (OrderBook orderBook, Gson gson) {
        super(orderBook, gson);
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
                System.err.println("map should not be empty after while condition, Wrong logic");
                break;
            }

            while (!calculateConditionToWait(this.bidMarketPrice)){
                Integer price = orderBook.getAskMarketPrice(orderBook.getAskMapStop());
                ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = orderBook.getAskMapStop();
                ConcurrentLinkedQueue<Order> queue = map.get(price);
                Iterator<Order> iterator = queue.iterator();
                iterate (iterator);
                map.remove(price);
            }
            System.out.println("uscito dal while");

            MyUtils.printMap(orderBook.getAskMapStop());
            MyUtils.printMap(orderBook.getBidMap());

            MyUtils.updateJson(Costants.ASK_MAP_STOP_FILE, orderBook.getAskMapStop(), gson);
            MyUtils.updateJson(Costants.ASK_MAP_TEMP_STOP_FILE, new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> (), gson);
        }
    }
}
