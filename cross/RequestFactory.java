package cross;

public class RequestFactory {

    public static RegistrationRequest createRegistrationRequest(String username, String password) {
        RegistrationRequest.Values values = new RegistrationRequest.Values(username, password);
        return new RegistrationRequest("register", values);
    }
}