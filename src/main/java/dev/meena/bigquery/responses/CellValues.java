package dev.meena.bigquery.responses;

import dev.meena.bigquery.dialect.BqTypes;
import dev.meena.bigquery.model.TableCell;
import org.postgresql.util.PGInterval;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Encodes values read from Postgres as BigQuery REST API cell values.
 */
public final class CellValues {

    private CellValues() {
    }

    /**
     * Encodes a value read from Postgres the way the BigQuery REST API writes it: everything is a string
     * (INTEGER, FLOAT, NUMERIC, BOOLEAN, JSON, ...), TIMESTAMP is epoch seconds, BYTES is base64 and
     * repeated columns are lists of cells.
     */
    public static Object encode(Object value, BqTypes.BqType type) {
        return switch (value) {
            case null -> null;
            case byte[] bytes -> Base64.getEncoder().encodeToString(bytes);
            case java.sql.Array array ->
                    throw new IllegalStateException("Arrays must be read with getArray(): " + array);
            case Object array when array.getClass().isArray() -> {
                var element = new BqTypes.BqType(type.type(), false);
                List<TableCell> cells = new ArrayList<>();
                for (int i = 0; i < Array.getLength(array); i++)
                    cells.add(new TableCell().v(encode(Array.get(array, i), element)));
                yield cells;
            }
            case OffsetDateTime dateTime -> epochSeconds(dateTime.toInstant());
            case java.sql.Timestamp timestamp -> "TIMESTAMP".equals(type.type())
                    ? epochSeconds(timestamp.toInstant())
                    : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timestamp.toLocalDateTime());
            case LocalDateTime dateTime -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
            case java.sql.Time time -> DateTimeFormatter.ISO_LOCAL_TIME.format(time.toLocalTime());
            case LocalTime time -> DateTimeFormatter.ISO_LOCAL_TIME.format(time);
            case BigDecimal decimal -> decimal.toPlainString();
            case PGInterval interval -> interval(interval);
            default -> String.valueOf(value);
        };
    }

    /**
     * BigQuery's canonical interval: {@code [-]Y-M [-]D [-]H:M:S[.F]}.
     */
    private static String interval(PGInterval i) {
        boolean negativeMonths = i.getYears() < 0 || i.getMonths() < 0;
        boolean negativeTime = i.getHours() < 0 || i.getMinutes() < 0 || i.getSeconds() < 0;
        return (negativeMonths ? "-" : "") + Math.abs(i.getYears()) + "-" + Math.abs(i.getMonths())
                + " " + i.getDays()
                + " " + (negativeTime ? "-" : "") + Math.abs(i.getHours()) + ":" + Math.abs(i.getMinutes()) + ":"
                + BigDecimal.valueOf(Math.abs(i.getSeconds())).stripTrailingZeros().toPlainString();
    }

    /**
     * Epoch seconds with microsecond precision, e.g. {@code 1577836800.123456}.
     */
    private static String epochSeconds(Instant instant) {
        BigDecimal seconds = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano() / 1_000, 6));
        return seconds.stripTrailingZeros().toPlainString();
    }
}
