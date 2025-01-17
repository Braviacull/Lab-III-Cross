package cross;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateGMT {
    // Metodo che converte un timestamp epoch in una data GMT formattata come MMYYYY
    public static int EpochToGMT(long epochSeconds) {
        // Crea un oggetto Instant a partire dai secondi epoch
        Instant instant = Instant.ofEpochSecond(epochSeconds);
        // Crea un formatter per formattare la data come MMYYYY nella zona GMT
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMYYYY").withZone(ZoneId.of("GMT"));                                         
        // Converte l'Instant in una stringa formattata e poi in un intero
        return Integer.parseInt(formatter.format(instant));
    }
}