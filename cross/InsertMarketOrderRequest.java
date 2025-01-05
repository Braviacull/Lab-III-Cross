package cross;

public class InsertMarketOrderRequest extends Request{
    private Values values;

    public InsertMarketOrderRequest (String operation, Values values){
        super(operation);
        this.values = values;
    }

    public static class Values {
        private String type;
        private int size;

        public Values (String type, int size) {
            this.type = type;
            this.size = size;
        }

        public String getType() {
            return type;
        }

        public int getSize() {
            return size;
        }
    }

    public Values getValues() { 
        return values;
    }
}
