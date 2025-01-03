package cross;

public class LimitOrder extends Order{
    private int price;

    public LimitOrder(String type, int size, int price) {
        super(type, size);
        this.price = price;
    }

    public int getPrice () {
        return price;
    }
}
