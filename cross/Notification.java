package cross;

public class Notification {
    private String notification;
    private Trade Trade;

    public Notification (Trade Trade) {
        notification = Costants.CLOSED_TRADES;
        this.Trade = Trade;
    }
}
