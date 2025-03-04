package cross;

import java.util.HashMap;
import java.util.Map;

// classe che gestisce i responsestatus per richieste di tipo differente
public class ResponseStatus {
    private int responseCode;
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

    private static final Map<Integer, String> errorMessagesLogout = new HashMap<>();

    static {
        errorMessagesLogout.put(100, "OK");
        errorMessagesLogout.put(101, "username/connection mismatch or non existent username or user not logged in");
    }

    private static final Map<Integer, String> errorMessagesCancelOrder = new HashMap<>();

    static {
        errorMessagesCancelOrder.put(100, "OK");
        errorMessagesCancelOrder.put(101, "order does not exist or belongs to different user or has already been finalized");
    }

    private static final Map<Integer, String> errorMessagesGetPriceHistory = new HashMap<>();

    static {
        errorMessagesGetPriceHistory.put(100, "OK");
        errorMessagesGetPriceHistory.put(101, "month not found in history");
    }

    public ResponseStatus(int responseCode, RegistrationRequest reg) {
        this.responseCode = responseCode;
        this.errorMessage = errorMessagesRegistration.getOrDefault(responseCode, "other error cases");
    }

    public ResponseStatus(int responseCode, UpdateCredentialsRequest update) {
        this.responseCode = responseCode;
        this.errorMessage = errorMessagesUpdateCredentials.getOrDefault(responseCode, "other error cases");
    }

    public ResponseStatus(int responseCode, LoginRequest login) {
        this.responseCode = responseCode;
        this.errorMessage = errorMessagesLogin.getOrDefault(responseCode, "other error cases");
    }

    public ResponseStatus(int responseCode, LogoutRequest logout) {
        this.responseCode = responseCode;
        this.errorMessage = errorMessagesLogout.getOrDefault(responseCode, "other error cases");
    }

    public ResponseStatus(int responseCode, CancelOrderRequest cancel) {
        this.responseCode = responseCode;
        this.errorMessage = errorMessagesCancelOrder.getOrDefault(responseCode, "other error cases");
    }

    public ResponseStatus(int responseCode, GetPriceHistoryRequest getPriceHistoryRequest) {
        this.responseCode = responseCode;
        this.errorMessage = errorMessagesGetPriceHistory.getOrDefault(responseCode, "other error cases");
    }

    public ResponseStatus(){
        this.responseCode = 99;
        this.errorMessage = "Command not found";
    }
    
    public int getResponseCode() {
        return responseCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}

