package client;

// La classe AutomaticLogout implementa Runnable per eseguire il logout automatico dopo un timeout
public class AutomaticLogout implements Runnable {
    // Oggetto lock per la sincronizzazione
    private final Object lock = new Object();
    // Variabile per controllare se il thread è in esecuzione
    private boolean running = true;
    // Variabile per controllare se il timer è stato resettato
    private boolean timerReset = false;
    
    // Timeout per il logout automatico
    private final long timeout;
    // Riferimento a ClientMain
    private ClientMain clientMain;

    public AutomaticLogout(long timeout, ClientMain clientMain) {
        this.timeout = timeout;
        this.clientMain = clientMain;
    }

    public void resetTimer() {
        synchronized (lock) {
            timerReset = true;
            lock.notify();
        }
    }

    public void stop() {
        running = false;
        resetTimer();
    }

    public void run() {
        while (running) {
            synchronized (lock) {
                try {
                    if (timerReset) timerReset = false;
                    lock.wait(timeout);
                    if (!timerReset && running) { // viene saltato se si resetta il timer
                        synchronized (clientMain){
                            if (clientMain.getIsServerOnline().get()) {
                                Sync.printlnSync("Automatic logout\n" + Costants.LOGGED_OUT_POSSIBLE_ACTIONS);
                                performLogout();
                            } else {
                                running = false;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void performLogout(){
        clientMain.handleLogout();
    }

}