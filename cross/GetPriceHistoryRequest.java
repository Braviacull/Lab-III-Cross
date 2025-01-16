package cross;

public class GetPriceHistoryRequest extends Request{
    private Values values;

    public GetPriceHistoryRequest (String operation, Values values){
        super(operation);
        this.values = values;
    }

    public static class Values {
        private int month;

        public Values (int month) {
            this.month = month;
        }

        public int getMonth() {
            return month;
        }
    }

    public Values getValues() { 
        return values;
    }
    
}
