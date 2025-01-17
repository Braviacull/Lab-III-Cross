package server;

public class LogoutRequest extends Request {
    private Values values;

    public LogoutRequest(String operation, Values values) {
        super(operation);
        this.values = values;
    }

    public static class Values {

        public Values() {
            //empty obj
        }
    }

    public Values getValues() {
        return values;
    }
}
