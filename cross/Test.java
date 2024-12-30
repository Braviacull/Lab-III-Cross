package cross;
import com.google.gson.*;
import java.io.FileWriter;
import java.io.IOException;

public class Test {
    public static void main (String[] args) {
        try {
            Gson gson = new Gson ();
            User user = new User ("Alessio", "pccuRecx");
            FileWriter writer = new FileWriter("users.json", true);
            gson.toJson(user, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
