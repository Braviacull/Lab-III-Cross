package client;

// Factory per costruire richieste
public class RequestFactory {

    public static RegistrationRequest createRegistrationRequest(String username, String password) {
        RegistrationRequest.Values values = new RegistrationRequest.Values(username, password);
        return new RegistrationRequest(Costants.REGISTER, values);
    }

    public static UpdateCredentialsRequest createUpdateCredentialsRequest(String username, String old_password, String new_password) {
        UpdateCredentialsRequest.Values values = new UpdateCredentialsRequest.Values(username, old_password, new_password);
        return new UpdateCredentialsRequest(Costants.UPDATE_CREDENTIALS, values);
    }

    public static LoginRequest createLoginRequest(String username, String password) {
        LoginRequest.Values values = new LoginRequest.Values(username, password);
        return new LoginRequest(Costants.LOGIN, values);
    }

    public static LogoutRequest createLogoutRequest() {
        LogoutRequest.Values values = new LogoutRequest.Values();
        return new LogoutRequest(Costants.LOGOUT, values);
    }

    public static InsertLimitOrderRequest createInsertLimitOrderRequest(String type, int size, int price) {
        InsertLimitOrderRequest.Values values = new InsertLimitOrderRequest.Values(type, size, price);
        return new InsertLimitOrderRequest (Costants.INSERT_LIMIT_ORDER, values);
    }

    public static InsertMarketOrderRequest createInsertMarketOrderRequest(String type, int size) {
        InsertMarketOrderRequest.Values values = new InsertMarketOrderRequest.Values(type, size);
        return new InsertMarketOrderRequest (Costants.INSERT_MARKET_ORDER, values);
    }

    public static InsertStopOrderRequest createInsertStopOrderRequest(String type, int size, int price) {
        InsertStopOrderRequest.Values values = new InsertStopOrderRequest.Values(type, size, price);
        return new InsertStopOrderRequest (Costants.INSERT_STOP_ORDER, values);
    }

    public static CancelOrderRequest createCancelOrderRequest(int orderId) {
        CancelOrderRequest.Values values = new CancelOrderRequest.Values(orderId);
        return new CancelOrderRequest(Costants.CANCEL_ORDER, values);
    }

    public static GetPriceHistoryRequest createGetPriceHistoryRequest(int month) {
        GetPriceHistoryRequest.Values values = new GetPriceHistoryRequest.Values(month);
        return new GetPriceHistoryRequest(Costants.GET_PRICE_HISTORY, values);
    }
}