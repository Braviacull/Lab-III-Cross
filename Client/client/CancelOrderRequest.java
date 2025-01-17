package client;

public class CancelOrderRequest extends Request{
    private Values values;

    public CancelOrderRequest (String operation, Values values){
        super(operation);
        this.values = values;
    }

    public static class Values {
        private int orderId;

        public Values (int orderId) {
            this.orderId = orderId;
        }

        public int getOrderId() {
            return orderId;
        }
    }

    public Values getValues() { 
        return values;
    }
}
