package cross;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateGMT {
    // public void main () {
    //     convertEpochToGMT(0);
    // }

    public static int convertEpochToGMT(long epochSeconds) {
        Instant instant = Instant.ofEpochSecond(epochSeconds);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMYYYY").withZone(ZoneId.of("GMT"));
        // System.out.println(formatter.format(instant));                                            
        return Integer.parseInt(formatter.format(instant));
    }
}