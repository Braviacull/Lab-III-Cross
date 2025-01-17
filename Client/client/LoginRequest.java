package client;

public class LoginRequest extends Request {
    private Values values;

    public LoginRequest(String operation, Values values) {
        super(operation);
        this.values = values;
    }

    public static class Values {
        private String username;
        private String password;

        public Values(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public User getUser () {
            return new User (username, password);
        }
    }

    public Values getValues() {
        return values;
    }
}
