package cross;

import java.util.HashMap;
import java.util.Map;

public class ResponseStatus {
    private int response;
    private String errorMessage;

    private static final Map<Integer, String> errorMessagesRegistration = new HashMap<>();

    static {
        errorMessagesRegistration.put(100, "OK");
        errorMessagesRegistration.put(101, "invalid password");
        errorMessagesRegistration.put(102, "username not available");
    }

    private static final Map<Integer, String> errorMessagesUpdateCredentials = new HashMap<>();

    static {
        errorMessagesUpdateCredentials.put(100, "OK");
        errorMessagesUpdateCredentials.put(101, "invalid new password");
        errorMessagesUpdateCredentials.put(102, "username/old_password mismatch or non existent username");
        errorMessagesUpdateCredentials.put(103, "new password equal to old one");
        errorMessagesUpdateCredentials.put(104, "user currently logged in");
    }

    private static final Map<Integer, String> errorMessagesLogin = new HashMap<>();

    static {
        errorMessagesLogin.put(100, "OK");
        errorMessagesLogin.put(101, "username/password mismatch or non existent username");
        errorMessagesLogin.put(102, "user already logged in");
    }

    public ResponseStatus(int response, RegistrationRequest reg) {
        this.response = response;
        this.errorMessage = errorMessagesRegistration.getOrDefault(response, "other error cases");
    }

    public ResponseStatus(int response, UpdateCredentialsRequest update) {
        this.response = response;
        this.errorMessage = errorMessagesUpdateCredentials.getOrDefault(response, "other error cases");
    }

    public ResponseStatus(int response, LoginRequest login) {
        this.response = response;
        this.errorMessage = errorMessagesLogin.getOrDefault(response, "other error cases");
    }

    public ResponseStatus(){
        this.response = 99;
        this.errorMessage = "Command not found";
    }
    
    public int getResponse() {
        return response;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}

