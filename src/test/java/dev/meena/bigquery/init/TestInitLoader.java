package dev.meena.bigquery.init;

import dev.meena.bigquery.db.Postgres;
import dev.meena.test.StartPostgresContainers;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The sample directory in src/test/resources/init-sample is loaded when the app starts (see InitSampleProfile).
 */
@QuarkusTest
@TestProfile(InitSampleProfile.class)
@QuarkusTestResource(StartPostgresContainers.class)
public class TestInitLoader {

    private static final Path SAMPLE = Path.of("src/test/resources/init-sample");

    @Inject
    InitLoader loader;

    @Inject
    Postgres postgres;

    @Inject
    AgroalDataSource dataSource;

    private Object scalar(String sql) {
        return postgres.query(sql).rows().getFirst().getFirst();
    }

    @Test
    void sqlFilesCreateTablesAndInsertRows() {
        // 2 people from 02-data.sql + 3 from the csv
        assertEquals(5L, scalar("SELECT COUNT(*) FROM \"loadertest$app\".people"));
        assertEquals("Ada; Lovelace", scalar("SELECT name FROM \"loadertest$app\".people WHERE id = 1"));
        assertEquals("Alan # Turing", scalar("SELECT name FROM \"loadertest$app\".people WHERE id = 2"));
        assertEquals(new java.math.BigDecimal("7.00"), scalar("SELECT score FROM \"loadertest$app\".people WHERE id = 2"));
    }

    @Test
    void tablesGetPostgresTypesFromBigQueryDdl() {
        List<List<Object>> columns = postgres.query("""
                SELECT column_name, data_type FROM information_schema.columns
                WHERE table_schema = 'loadertest$app' AND table_name = 'people' ORDER BY ordinal_position
                """).rows();
        assertEquals(List.of("id", "bigint"), columns.get(0));
        assertEquals(List.of("score", "numeric"), columns.get(3));
        assertEquals(List.of("tags", "ARRAY"), columns.get(5));
        assertEquals(List.of("created", "timestamp with time zone"), columns.get(6));
        assertEquals(List.of("profile", "jsonb"), columns.get(7));
    }

    @Test
    void dmlStatementsRunInOrder() {
        // INSERT, UPDATE, then INSERT ... SELECT duplicates the updated row with id + 1
        assertEquals(2L, scalar("SELECT COUNT(*) FROM \"loadertest$app\".events"));
        assertEquals("updated", scalar("SELECT payload FROM \"loadertest$app\".events WHERE id = 2"));
    }

    @Test
    void csvFileLoadsQuotedMultilineArraysJsonAndNulls() {
        assertEquals("Grace \"Amazing\" Hopper, Rear Admiral", scalar("SELECT name FROM \"loadertest$app\".people WHERE id = 3"));
        assertEquals("navy,cobol", scalar("SELECT array_to_string(tags, ',') FROM \"loadertest$app\".people WHERE id = 3"));
        assertEquals("1", scalar("SELECT profile #>> '{k,0}' FROM \"loadertest$app\".people WHERE id = 3"));
        assertEquals("2021-06-01T08:00:00", scalar("SELECT to_char(created AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS') FROM \"loadertest$app\".people WHERE id = 3"));
        assertEquals("multi\nline", scalar("SELECT name FROM \"loadertest$app\".people WHERE id = 5"));
        assertNull(scalar("SELECT name FROM \"loadertest$app\".people WHERE id = 4"));
        assertEquals(false, scalar("SELECT active FROM \"loadertest$app\".people WHERE id = 5"));
    }

    @Test
    void anInitializedDatabaseIsNotLoadedAgain() {
        assertFalse(loader.load(SAMPLE));
        assertEquals(5L, scalar("SELECT COUNT(*) FROM \"loadertest$app\".people"));
    }

    @Test
    void failedLoadChangesNothing(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("01.sql"), "CREATE SCHEMA failing; CREATE TABLE failing.t (a INT64); INSERT INTO failing.t VALUES (1);");
        Files.writeString(dir.resolve("02.sql"), "INSERT INTO failing.missing VALUES (1);");

        // Pretend this is a fresh database for the duration of the test
        execute("ALTER TABLE \"bq$jobs\".init_state RENAME TO init_state_saved");
        try {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> loader.load(dir));
            assertTrue(e.getMessage().contains("02.sql"), e.getMessage());
            assertEquals(0L, scalar("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'loadertest$failing'"));
        } finally {
            execute("DROP TABLE IF EXISTS \"bq$jobs\".init_state");
            execute("ALTER TABLE \"bq$jobs\".init_state_saved RENAME TO init_state");
        }
    }

    @Test
    void csvForUnknownTableOrColumnIsRejected(@TempDir Path dir) throws Exception {
        execute("ALTER TABLE \"bq$jobs\".init_state RENAME TO init_state_saved2");
        try {
            Files.writeString(dir.resolve("app.nope.csv"), "a\n1\n");
            assertTrue(assertThrows(IllegalStateException.class, () -> loader.load(dir)).getMessage().contains("does not exist"));

            Files.delete(dir.resolve("app.nope.csv"));
            Files.writeString(dir.resolve("app.events.csv"), "id,bogus\n1,2\n");
            assertTrue(assertThrows(IllegalStateException.class, () -> loader.load(dir)).getMessage().contains("no column 'bogus'"));
        } finally {
            execute("DROP TABLE IF EXISTS \"bq$jobs\".init_state");
            execute("ALTER TABLE \"bq$jobs\".init_state_saved2 RENAME TO init_state");
        }
    }

    @Test
    void theShippedSampleDirectoryLoads() throws Exception {
        execute("ALTER TABLE \"bq$jobs\".init_state RENAME TO init_state_saved3");
        try {
            assertTrue(loader.load(Path.of("init")));
            assertEquals(4L, scalar("SELECT COUNT(*) FROM \"loadertest$sample\".people"));
            assertEquals(1L, scalar("SELECT COUNT(*) FROM \"loadertest$sample\".events"));
            assertEquals("navy,cobol", scalar("SELECT array_to_string(tags, ',') FROM \"loadertest$sample\".people WHERE id = 3"));
        } finally {
            execute("DROP TABLE IF EXISTS \"bq$jobs\".init_state");
            execute("ALTER TABLE \"bq$jobs\".init_state_saved3 RENAME TO init_state");
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection con = dataSource.getConnection(); Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }
}
