package cross;

public class PeriodicPing implements Runnable{
    private final Object lock = new Object();
    private boolean running = true;
    
    private final long timeout;
    private ClientMain clientMain;
    public PeriodicPing (long timeout, ClientMain clientMain) {
        this.timeout = timeout;
        this.clientMain = clientMain;
    }

    public void stop() {
        synchronized (lock) {
            running = false;
            lock.notify();
        }
    }

    public void run() {
        while (running) {
            synchronized (lock) {
                try {
                    lock.wait(timeout);
                    if (running) {
                        synchronized (clientMain){
                            if (!clientMain.isServerOnline()) {
                                clientMain.getIsServerOnline().set(false);
                                System.out.println("Server Offline, scrivi qualsiasi cosa per terminare");
                                running = false;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("Periodic Ping terminato");
    }
}