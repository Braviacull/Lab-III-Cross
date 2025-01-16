package cross;
public class AutomaticLogout implements Runnable {
    private final Object lock = new Object();
    private boolean running = true;
    private boolean timerReset = false;
    
    private final long timeout;
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
                    timerReset = false;
                    lock.wait(timeout);
                    if (!timerReset && running) {
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
        Sync.printlnSync("timer terminato");
    }

    private void performLogout(){
        clientMain.handleLogout();
    }

}