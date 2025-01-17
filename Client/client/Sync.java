package client;

public class Sync {
    public static Object console = new Object();

    // serve a condividere la console del client tra i vari thread che la usano
    public static void printlnSync (String line) {
        synchronized (console) {
            System.out.println(line);
        }
    }
}
