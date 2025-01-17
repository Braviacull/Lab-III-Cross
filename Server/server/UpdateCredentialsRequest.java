package server;

public class UpdateCredentialsRequest extends Request{
    private Values values;

    public UpdateCredentialsRequest (String operation, Values values) {
        super(operation);
        this.values = values;
    }

    public static class Values {
        private String username;
        private String old_password;
        private String new_password;

        public Values(String username, String old_password, String new_password) {
            this.username = username;
            this.old_password = old_password;
            this.new_password = new_password;
        }

        public User getOldUser() {
            return new User (username, old_password);
        }

        public User getNewUser() {
            return new User (username, new_password);
        }

        public boolean arePasswordsEquals () {
            return new_password.equals(old_password);
        }
    }

    public UpdateCredentialsRequest.Values getValues() {
        return values;
    }
}
