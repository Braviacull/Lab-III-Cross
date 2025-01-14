package cross;

public class Trades {
    private int orderId;
    private String type; // ask or bid
    private String orderType; // limit, market o stop
    private int size;
    private int price;
    private int timestamp;

    public Trades(int orderId, String type, String orderType, int size, int price, int timestamp) {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
    }

    public Trades(int orderId, String type, String orderType, int size, int timestamp) {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.timestamp = timestamp;
    }
}