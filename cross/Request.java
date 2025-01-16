package cross;

public class Request {
    private String operation;

    public Request(String operation) {
        setOperation(operation);
    }

    public void setOperation(String operation) {
        switch (operation) {
            case Costants.REGISTER:
                break;
            case Costants.UPDATE_CREDENTIALS:
                break;
            case Costants.LOGIN:
                break;
            case Costants.LOGOUT:
                break;
            case Costants.INSERT_LIMIT_ORDER:
                break;
            case Costants.INSERT_MARKET_ORDER:
                break;
            case Costants.INSERT_STOP_ORDER:
                break;
            case Costants.CANCEL_ORDER:
                break;
            case Costants.GET_PRICE_HISTORY:
                break;
            default:
                throw new IllegalArgumentException(operation + " not supported");
        }
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}