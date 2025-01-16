package cross;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateGMT {
    public static int convertEpochToGMT(long epochSeconds) {
        Instant instant = Instant.ofEpochSecond(epochSeconds);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMYYYY").withZone(ZoneId.of("GMT"));                                         
        return Integer.parseInt(formatter.format(instant));
    }
}