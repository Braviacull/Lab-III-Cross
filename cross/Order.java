package cross;

import java.util.concurrent.atomic.AtomicInteger;

public class Order {
    private static AtomicInteger nextID = new AtomicInteger(1); // reinizialised in ServerMain constructor
    
    private final int id;
    private String type; // ask/bid
    private int size;

    public Order(String type, int size) {
        this.id = Order.nextID.getAndIncrement();
        setType(type);
        this.size = size;
    }

    public void setType(String type) {
        switch (type) {
            case "ask":
                break;
            case "bid":
                break;
            default:
                throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
        }
        this.type = type;
    }

    public static void setNextID (int nextID) {
        Order.nextID = new AtomicInteger(nextID);
    }

    public static int getNextId () {
        return nextID.get();
    }

    public int getId () {
        return id;
    }

    public String getType () {
        return type;
    }

    public int getSize () {
        return size;
    }

}