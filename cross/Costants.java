package cross;

public class Costants {
    // File di configurazione del client
    public static final String CLIENT_PROPERTIES_FILE = "client.properties";

    // Stato del server
    public static final String ONLINE = "online";
    public static final String OFFLINE = "offline";
    public static final String PING = "ping"; 

    // Operazioni
    public static final String REGISTER = "register";
    public static final String LOGIN = "login";
    public static final String LOGOUT = "logout";
    public static final String UPDATE_CREDENTIALS = "updateCredentials";
    public static final String INSERT_LIMIT_ORDER = "insertLimitOrder";
    public static final String INSERT_MARKET_ORDER = "insertMarketOrder";
    public static final String INSERT_STOP_ORDER = "insertStopOrder";
    public static final String CANCEL_ORDER = "cancelOrder";
    public static final String GET_PRICE_HISTORY = "getPriceHistory";

    // Azioni possibili
    public static final String LOGGED_IN_POSSIBLE_ACTIONS = "Possible actions: (exit, logout, insertLimitOrder, insertMarketOrder, insertStopOrder, cancelOrder, getPriceHistory)";
    public static final String LOGGED_OUT_POSSIBLE_ACTIONS = "Possible actions: (exit, register, updateCredentials, login, getPriceHistory)";

    // Tipi di ordini
    public static final String LIMIT = "limit";
    public static final String MARKET = "market";
    public static final String STOP = "stop";

    // Tipi di operazioni
    public static final String ASK = "ask";
    public static final String BID = "bid";

    // Proprietà del server
    public static final String SERVER_IP = "server.ip";
    public static final String SERVER_PORT = "server.port";
    public static final String SERVER_STOP_STRING = "server.stop_string";
    public static final String SERVER_NEXT_ID = "server.next_id";
    public static final String NOTIFICATION_PORT = "notification.port";
    public static final String TIMEOUT = "automaticLogout.timeout";
    public static final String PERIOD = "server.period";
    public static final String AWAIT_SECONDS = "server.await";
    public static final String PERIODIC_PING_TIMEOUT = "periodicPing.timeout";

    // File supportati dall'orderbook
    public static final String BID_MAP_FILE = "bidMap.json";
    public static final String ASK_MAP_FILE = "askMap.json";
    public static final String BID_MAP_STOP_FILE = "bidMapStop.json";
    public static final String ASK_MAP_STOP_FILE = "askMapStop.json";

    // File degli utenti
    public static final String USERS_MAP_FILE = "usersMap.json";
    public static final String SERVER_PROPERTIES_FILE = "server.properties";

    // File dello storico ordini
    public static final String STORICO_ORDINI = "storicoOrdini.json";

    // Altre costanti
    public static final String CLOSED_TRADES = "closedTrades";
}