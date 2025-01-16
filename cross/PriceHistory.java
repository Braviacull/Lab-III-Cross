package cross;

public class PriceHistory {
    private int max;
    private int min;
    private int apertura;
    private int chiusura;

    public PriceHistory(int max, int min, int apertura, int chiusura) {
        this.max = max;
        this.min = min;
        this.apertura = apertura;
        this.chiusura = chiusura;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public int getApertura() {
        return apertura;
    }

    public int getChiusura() {
        return chiusura;
    }

    @Override
    public String toString() {
        return "PriceHistory{" +
                "max=" + max +
                ", min=" + min +
                ", apertura=" + apertura +
                ", chiusura=" + chiusura +
                '}';
    }
}