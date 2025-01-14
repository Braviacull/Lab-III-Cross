package cross;

public class Trade {
    private int orderId;
    private String type; // ask or bid
    private String orderType; // limit, market o stop
    private int size;
    private int price;
    private int timestamp;

    public Trade(int orderId, String type, String orderType, int size, int price, int timestamp) {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
    }

    public Trade(int orderId, String type, String orderType, int size, int timestamp) {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.timestamp = timestamp;
    }

    // Getters
    public int getOrderId() {
        return orderId;
    }

    public String getType() {
        return type;
    }

    public String getOrderType() {
        return orderType;
    }

    public int getSize() {
        return size;
    }

    public int getPrice() {
        return price;
    }

    public int getTimestamp() {
        return timestamp;
    }
}