package client;

public class InsertLimitOrderRequest extends Request{
    private Values values;

    public InsertLimitOrderRequest (String operation, Values values) {
        super(operation);
        this.values = values;
    }

    public static class Values {
        private String type;
        private int size;
        private int price;

        public Values (String type, int size, int price) {
            this.type = type;
            this.size = size;
            this.price = price;
        }

        public String getType() {
            return type;
        }

        public int getSize() {
            return size;
        }

        public int getPrice() {
            return price;
        }

    }

    public Values getValues() { 
        return values;
    }
}
