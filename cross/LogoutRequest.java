package cross;

public class LogoutRequest extends Request {
    private Values values;

    public LogoutRequest(String operation, Values values) {
        super(operation);
        this.values = values;
    }

    public static class Values {
        private String username;

        public Values() {
            //empty obj
        }
    }

    public Values getValues() {
        return values;
    }
}
