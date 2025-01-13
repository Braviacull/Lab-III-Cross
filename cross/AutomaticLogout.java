package cross;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;

public class AutomaticLogout implements Runnable {
    private final Object lock = new Object();
    private boolean running = true;
    private boolean timerReset = false;
    
    private final long timeout;
    private DataInputStream in;
    private DataOutputStream out;
    private Gson gson;
    private String username;
    private AtomicBoolean loggedIn;
    private ReceiveNotification receiveNotification;
    public AutomaticLogout(long timeout, DataInputStream in, DataOutputStream out, Gson gson, String username, AtomicBoolean loggedIn, ReceiveNotification receiveNotification) {
        this.timeout = timeout;
        this.in = in;
        this.out = out;
        this.gson = gson;
        this.username = username;
        this.loggedIn = loggedIn;
        this.receiveNotification = receiveNotification;
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
                            performLogout();
                            System.out.println("Automatic logout");
                            System.out.println("Possible actions: (exit, register, updateCredentials, login)");
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
        try {MyUtils.sendLine(Costants.LOGOUT, out);
            LogoutRequest logout = RequestFactory.createLogoutRequest(); // Create logout request
            String jsonLogout = gson.toJson(logout); // Convert request to JSON
            MyUtils.sendLine(jsonLogout, out); // Send JSON request to the server
            MyUtils.sendLine(username, out); // Send username separately as logout.Values is an empty object
            String response = in.readUTF(); // Read the response from the server
            ResponseStatus responseStatus = gson.fromJson(response, ResponseStatus.class); // Convert JSON response to ResponseStatus object
            int responseCode = responseStatus.getResponseCode(); // Get response code
            String errorMessage = responseStatus.getErrorMessage(); // Get error message
            System.out.println(responseCode + " - " + errorMessage);
            loggedIn.set(false);
            receiveNotification.stop();
            receiveNotification = null;
            stop(); // il thread può terminare
        } catch (IOException e) {
            System.err.println("Error performing logout: " + e.getMessage());
            e.printStackTrace();
        }
        
    }

}