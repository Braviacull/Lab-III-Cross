package cross;
import com.google.gson.*;

public class Test {
    public static void main (String[] args) {
        User obj = new User ("Alessio", "password");
        Gson gson = new Gson();

        String myJson = gson.toJson(obj);
        System.out.println(myJson);
    }
}
