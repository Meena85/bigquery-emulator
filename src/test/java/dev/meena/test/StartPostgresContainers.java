package dev.meena.test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class StartPostgresContainers implements QuarkusTestResourceLifecycleManager {

    private PostgreSQLContainer postgreSQLContainer;

    @Override
    public Map<String, String> start() {
        // Run against an existing Postgres instead of a container:
        //   -Dtest.postgres.url=jdbc:postgresql://localhost:3306/bigquery -Dtest.postgres.user=.. -Dtest.postgres.password=..
        String existingUrl = System.getProperty("test.postgres.url");
        if (existingUrl != null) {
            return Map.of(
                    "quarkus.analytics.disabled", "true",
                    "quarkus.http.test-timeout", "3s",
                    "quarkus.datasource.db-kind", "postgresql",
                    "quarkus.datasource.jdbc.url", existingUrl,
                    "quarkus.datasource.username", System.getProperty("test.postgres.user", "postgres"),
                    "quarkus.datasource.password", System.getProperty("test.postgres.password", "postgres")
            );
        }

        Network network = Network.newNetwork();
        String networkAlias = "gcp-emulator-tests";

        postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
                .withExposedPorts(5432)
                .withDatabaseName("tests")
                .withUsername("tester")
                .withPassword("tester-pwd")
                .withNetwork(network)
                .withInitScripts("migration/init.sql")
                .withNetworkAliases(networkAlias);
        postgreSQLContainer.start();

        return Map.of(
                "quarkus.analytics.disabled", "true",
                "quarkus.http.test-timeout", "3s",
                "quarkus.datasource.db-kind", "postgresql",
                "quarkus.datasource.jdbc.url", postgreSQLContainer.getJdbcUrl(),
                "quarkus.datasource.username", postgreSQLContainer.getUsername(),
                "quarkus.datasource.password", postgreSQLContainer.getPassword()
        );
    }

    @Override
    public void stop() {
        if (postgreSQLContainer != null) {
            postgreSQLContainer.close();
        }
    }
}
