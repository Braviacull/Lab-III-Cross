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
                        synchronized (Sync.timeOutSync){
                            System.out.println("Automatic logout");
                            System.out.println("Possible actions: (exit, register, updateCredentials, login)");
                            performLogout();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("timer terminato");
    }

    private void performLogout(){
        clientMain.handleLogout();
    }

}