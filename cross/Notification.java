package cross;

public class Notification {
    private String notification;
    private Trades trades;

    public Notification (Trades trades) {
        notification = Costants.CLOSED_TRADES;
        this.trades = trades;
    }
}
