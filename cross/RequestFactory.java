package cross;

public class RequestFactory {

    public static RegistrationRequest createRegistrationRequest(String username, String password) {
        RegistrationRequest.Values values = new RegistrationRequest.Values(username, password);
        return new RegistrationRequest("register", values);
    }

    public static UpdateCredentialsRequest createUpdateCredentialsRequest(String username, String old_password, String new_password) {
        UpdateCredentialsRequest.Values values = new UpdateCredentialsRequest.Values(username, old_password, new_password);
        return new UpdateCredentialsRequest("updateCredentials", values);
    }
}