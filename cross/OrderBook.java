package cross;

import java.util.concurrent.ConcurrentSkipListMap;

public class OrderBook {
    private ConcurrentSkipListMap<Object, Object> BidMap;
    private ConcurrentSkipListMap<Object, Object> AskMap;
    
    private volatile int marketPrice;

    public OrderBook () {
        
    }
}