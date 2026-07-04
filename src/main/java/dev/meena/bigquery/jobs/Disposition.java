package dev.meena.bigquery.jobs;

public enum Disposition {
    CREATE_NEVER,
    CREATE_IF_NEEDED,
    WRITE_EMPTY,
    WRITE_TRUNCATE,
    WRITE_APPEND;

    public static Disposition from(String value, Disposition defaultValue) {
        if (value == null || value.isBlank())
            return defaultValue;

        return switch (value.trim().toUpperCase()) {
            case "CREATE_NEVER" -> CREATE_NEVER;
            case "CREATE_IF_NEEDED" -> CREATE_IF_NEEDED;
            case "WRITE_EMPTY" -> WRITE_EMPTY;
            case "WRITE_TRUNCATE" -> WRITE_TRUNCATE;
            case "WRITE_APPEND" -> WRITE_APPEND;
            default -> defaultValue;
        };
    }

    public static String toString(Disposition deposition) {
        if (deposition == null) return "";
        return deposition.name();
    }
}
