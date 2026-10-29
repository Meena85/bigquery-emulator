package dev.meena.bigquery.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.*;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts the JSON values BigQuery clients send (strings, numbers, booleans, lists, maps) into
 * objects the Postgres JDBC driver can bind for a column of a given Postgres type
 * (the {@code TYPE_NAME} JDBC reports: {@code int8}, {@code timestamptz}, {@code _text}, ...).
 */
public final class PgValues {

    private static final ObjectMapper JSON = new ObjectMapper();

    // 2020-01-01[T| ]12:34[:56[.123456]] [Z|UTC|+08|+08:00|+0800]
    private static final Pattern TEMPORAL = Pattern.compile(
            "^(\\d{4}-\\d{1,2}-\\d{1,2})(?:[T ]+(\\d{1,2}:\\d{2}(?::\\d{2}(?:\\.\\d+)?)?))?\\s*(Z|z|UTC|[+-]\\d{2}(?::?\\d{2})?)?$");
    // BigQuery canonical interval: [-]Y-M D [-]H:M:S[.F]
    private static final Pattern BQ_INTERVAL = Pattern.compile(
            "^(-?)(\\d+)-(\\d+) (-?\\d+) (-?)(\\d+):(\\d+):(\\d+)(\\.\\d+)?$");

    private PgValues() {
    }

    /** Returns the value to pass to {@code PreparedStatement.setObject}; {@code null} must be bound with setNull. */
    public static Object toParameter(Object value, String pgType) {
        String type = pgType == null ? "" : pgType.toLowerCase(Locale.ROOT);

        if (type.startsWith("_"))
            return array(value, type);

        return switch (type) {
            case "int2", "int4", "int8", "serial", "bigserial", "smallserial", "oid" -> toLong(value);
            case "float4", "float8" -> toDouble(value);
            case "numeric", "decimal" -> toBigDecimal(value);
            case "bool", "boolean", "bit" -> value instanceof Boolean ? value : Boolean.parseBoolean(String.valueOf(value).trim());
            case "date" -> toDate(value);
            case "time", "timetz" -> toTime(value);
            case "timestamp" -> toOffsetDateTime(value).toLocalDateTime();
            case "timestamptz" -> toOffsetDateTime(value);
            case "bytea" -> toBytes(value);
            case "json", "jsonb" -> pgObject(type, jsonText(value));
            case "interval" -> pgObject(type, intervalText(String.valueOf(value)));
            case "daterange", "tsrange", "tstzrange", "int4range", "int8range", "numrange" ->
                    pgObject(type, rangeText(String.valueOf(value)));
            case "text", "varchar", "bpchar", "name", "" -> text(value);
            default -> pgObject(type, text(value));
        };
    }

    // ---- scalars

    static Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return new BigDecimal(String.valueOf(value).trim()).longValueExact();
    }

    static Double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return switch (String.valueOf(value).trim().toLowerCase(Locale.ROOT)) {
            case "nan" -> Double.NaN;
            case "inf", "+inf", "infinity", "+infinity" -> Double.POSITIVE_INFINITY;
            case "-inf", "-infinity" -> Double.NEGATIVE_INFINITY;
            case String s -> Double.parseDouble(s);
        };
    }

    static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal b) return b;
        return new BigDecimal(String.valueOf(value).trim());
    }

    static LocalDate toDate(Object value) {
        return switch (value) {
            case LocalDate d -> d;
            case java.sql.Date d -> d.toLocalDate();
            default -> {
                String text = String.valueOf(value).trim();
                Matcher m = TEMPORAL.matcher(text);
                yield LocalDate.parse(m.matches() ? pad(m.group(1)) : text);
            }
        };
    }

    static LocalTime toTime(Object value) {
        return switch (value) {
            case LocalTime t -> t;
            case java.sql.Time t -> t.toLocalTime();
            default -> LocalTime.parse(padTime(String.valueOf(value).trim()));
        };
    }

    /** Parses TIMESTAMP/DATETIME input: epoch seconds, ISO-8601 or BigQuery's space separated form. Zone-less input is UTC. */
    static OffsetDateTime toOffsetDateTime(Object value) {
        switch (value) {
            case OffsetDateTime o -> {
                return o;
            }
            case LocalDateTime l -> {
                return l.atOffset(ZoneOffset.UTC);
            }
            case java.sql.Timestamp t -> {
                return t.toInstant().atOffset(ZoneOffset.UTC);
            }
            case Number n -> {
                return epoch(new BigDecimal(n.toString()));
            }
            default -> {
            }
        }

        String text = String.valueOf(value).trim();
        if (text.matches("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$"))
            return epoch(new BigDecimal(text));

        Matcher m = TEMPORAL.matcher(text);
        if (!m.matches())
            throw new IllegalArgumentException("Invalid timestamp: " + text);

        LocalDate date = LocalDate.parse(pad(m.group(1)));
        LocalTime time = m.group(2) == null ? LocalTime.MIDNIGHT : LocalTime.parse(padTime(m.group(2)));
        return OffsetDateTime.of(date, time, zone(m.group(3)));
    }

    private static OffsetDateTime epoch(BigDecimal seconds) {
        long whole = seconds.setScale(0, RoundingMode.FLOOR).longValueExact();
        int nanos = seconds.subtract(BigDecimal.valueOf(whole)).movePointRight(9).intValue();
        return Instant.ofEpochSecond(whole, nanos).atOffset(ZoneOffset.UTC);
    }

    private static ZoneOffset zone(String zone) {
        if (zone == null || zone.equalsIgnoreCase("Z") || zone.equalsIgnoreCase("UTC"))
            return ZoneOffset.UTC;

        String digits = zone.substring(1).replace(":", "");
        String hh = digits.substring(0, 2);
        String mm = digits.length() > 2 ? digits.substring(2) : "00";
        return ZoneOffset.of(zone.charAt(0) + hh + ":" + mm);
    }

    private static String pad(String date) {
        String[] parts = date.split("-");
        return parts[0] + "-" + two(parts[1]) + "-" + two(parts[2]);
    }

    private static String two(String number) {
        return number.length() == 1 ? "0" + number : number;
    }

    private static String padTime(String time) {
        return time.length() < 5 || time.indexOf(':') == 2 ? time : "0" + time;
    }

    static byte[] toBytes(Object value) {
        if (value instanceof byte[] b) return b;
        return Base64.getMimeDecoder().decode(String.valueOf(value).trim());
    }

    static String text(Object value) {
        if (value instanceof Map<?, ?> || value instanceof List<?>) return jsonText(value);
        return String.valueOf(value);
    }

    static String jsonText(Object value) {
        if (value instanceof String s) return s;
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON value: " + e.getMessage(), e);
        }
    }

    /** BigQuery's {@code Y-M D H:M:S[.F]} to Postgres interval syntax; anything else (ISO 8601, ...) passes through. */
    static String intervalText(String interval) {
        Matcher m = BQ_INTERVAL.matcher(interval.trim());
        if (!m.matches()) return interval.trim();

        String yearSign = m.group(1);
        String timeSign = m.group(5);
        return yearSign + m.group(2) + " years " + yearSign + m.group(3) + " mons " + m.group(4) + " days "
                + timeSign + m.group(6) + ":" + m.group(7) + ":" + m.group(8) + (m.group(9) == null ? "" : m.group(9));
    }

    /** BigQuery ranges write open ends as UNBOUNDED/NULL; Postgres leaves them empty. */
    static String rangeText(String range) {
        return range.replaceAll("(?i)\\bUNBOUNDED\\b|\\bNULL\\b", "").replaceAll("\\s+", "");
    }

    // ---- arrays

    private static Object array(Object value, String arrayType) {
        String elementType = arrayType.substring(1);
        List<?> elements = value instanceof List<?> l ? l : List.of(value);

        String literal = elements.stream()
                .map(e -> e == null ? "NULL" : quoteElement(elementText(e, elementType)))
                .collect(Collectors.joining(",", "{", "}"));
        return pgObject(arrayType, literal);
    }

    private static String elementText(Object element, String elementType) {
        Object converted = toParameter(element, elementType);
        return switch (converted) {
            case byte[] bytes -> "\\x" + HexFormat.of().formatHex(bytes);
            case PGobject o -> o.getValue();
            case Boolean b -> b ? "t" : "f";
            default -> converted.toString();
        };
    }

    private static String quoteElement(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static PGobject pgObject(String type, String value) {
        try {
            PGobject object = new PGobject();
            object.setType(type);
            object.setValue(value);
            return object;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Invalid " + type + " value: " + value, e);
        }
    }
}
