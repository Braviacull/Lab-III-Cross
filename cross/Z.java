package cross;
import com.google.gson.*;
import java.io.FileWriter;
import java.io.IOException;

public class Z {
    public static void main (String[] args) {
        try {
            Gson gson = new Gson();
            RegistrationRequest r =  RequestFactory.createRegistrationRequest("Alessio", "pccuRecx");
            String json = gson.toJson(r);
            System.out.println(json);
            RegistrationRequest.Values v = r.getValues();
            String json2 = gson.toJson(v);
            System.out.println(json2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
