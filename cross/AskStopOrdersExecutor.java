// solo per StopAsk
package cross;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.gson.*;

public class AskStopOrdersExecutor implements Runnable {
    public OrderBook orderBook;
    public Gson gson;
    public int bidMarketPrice;
    
    AskStopOrdersExecutor (OrderBook orderBook, Gson gson) {
        this.orderBook = orderBook;
        this.gson = gson;
    }

    public void myNotify () {
        synchronized (this) {
            notify();
        }
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
                    try {
                        wait();
                        System.out.println("Notifica ricevuta");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Risvegliato");

            if (orderBook.getAskMapStop().isEmpty()) {
                System.err.println("map should not be empty after while condition, Wrong logic");
            }
            else {
                while (!calculateConditionToWait(this.bidMarketPrice)){
                    Integer price = orderBook.getAskMarketPrice(orderBook.getAskMapStop());
                    ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = orderBook.getAskMapStop();
                    ConcurrentLinkedQueue<Order> queue = map.get(price);
                    Iterator<Order> iterator = queue.iterator();
                    while (iterator.hasNext()) {
                        Order stopOrder = iterator.next();
                        int size = MyUtils.transaction(stopOrder.getSize(), stopOrder.getType() , orderBook);
                        MyUtils.checkTransaction(size, stopOrder.getType() , orderBook);
                        if (!iterator.hasNext()) {
                            if (queue.isEmpty()) {
                                System.out.println("queue is empty");
                            }
                            map.remove(price);
                        }
                    }
                }
                System.out.println("uscito dal while");
            }
            MyUtils.printMap(orderBook.getAskMapStop());
            MyUtils.printMap(orderBook.getBidMap());
            MyUtils.updateJson(Costants.ASK_MAP_STOP_FILE, orderBook.getAskMapStop(), gson);
            MyUtils.updateJson(Costants.ASK_MAP_TEMP_STOP_FILE, new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> (), gson);
        }
    }
}
