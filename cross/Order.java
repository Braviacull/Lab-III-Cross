package cross;

import java.util.concurrent.atomic.AtomicInteger;

public class Order {
    private static AtomicInteger nextID = new AtomicInteger(1); // reinizialised in ServerMain constructor
    
    private final int orderId;
    private String type; // ask/bid
    private int size;
    private int price = -1;
    private String username;

    // per marketOrder
    public Order(String type, int size, String username) {
        this.orderId = Order.nextID.getAndIncrement();
        setType(type);
        this.size = size;
        this.username = username;
    }

    // quando uno StopOrder si trasforma in un MarketOrder voglio mantenere l'id
    public Order(String type, int size, String username, int orderId) {
        this.orderId = orderId;
        setType(type);
        this.size = size;
        this.username = username;
    }

    // per limitOrder e StopOrder
    public Order(String type, int size, int price, String username) {
        this.orderId = Order.nextID.getAndIncrement();
        setType(type);
        this.size = size;
        this.price = price;
        this.username = username;
    }

    public void setType(String type) { // controlla prima che il tipo sia valido, altrimento lancia un'eccezione
        switch (type) {
            case Costants.ASK:
                break;
            case Costants.BID:
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
        return orderId;
    }

    public String getType () {
        return type;
    }

    public void setSize (int newSize) {
        this.size = newSize;
    }
    
    public int getSize () {
        return size;
    }

    public int getPrice () {
        if (price != -1) {
            return price;
        } else {
            switch (type) {
                case Costants.ASK:
                    return 0;
                case Costants.BID:
                    return Integer.MAX_VALUE;
                default:
                    throw new IllegalArgumentException("Type must be 'ask' or 'bid'");
            }
        }
    }

    public String getUsername () {
        return username;
    }

}