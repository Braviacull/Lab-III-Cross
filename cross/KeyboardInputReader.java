package cross;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class KeyboardInputReader implements Runnable {
    private volatile boolean running = true;
    private BufferedReader reader;
    private InputHandler inputHandler;

    public interface InputHandler {
        void handleInput(String input);
    }

    public KeyboardInputReader(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (reader.ready()) {
                    String line = reader.readLine();
                    if (line != null) {
                        inputHandler.handleInput(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}