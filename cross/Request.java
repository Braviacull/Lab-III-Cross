package cross;

public class Request {
    private String operation;

    public Request(String operation) {
        setOperation(operation);
    }

    public void setOperation(String operation) {
        switch (operation) {
            case "register":
                break;
            case "updateCredentials":
                break;
            case "login":
                break;
            case "logout":
                break;
            case "insertLimitOrder":
                break;
            default:
                throw new IllegalArgumentException(operation + "not supported");
        }
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}