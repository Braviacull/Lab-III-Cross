package cross;

public class Sync {
    public static Object console = new Object();

    public static void printlnSync (String line) {
        synchronized (console) {
            System.out.println(line);
        }
    }
}
