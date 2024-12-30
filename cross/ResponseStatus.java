package cross;

import java.util.HashMap;
import java.util.Map;

public class ResponseStatus {
    private int response;
    private String errorMessage;

    private static final Map<Integer, String> errorMessages = new HashMap<>();

    static {
        errorMessages.put(100, "OK");
        errorMessages.put(101, "invalid password");
        errorMessages.put(102, "username not available");
        errorMessages.put(103, "other error cases");
    }

    public ResponseStatus(int response) {
        this.response = response;
        this.errorMessage = errorMessages.getOrDefault(response, "unknown error");
    }

    public int getResponse() {
        return response;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}