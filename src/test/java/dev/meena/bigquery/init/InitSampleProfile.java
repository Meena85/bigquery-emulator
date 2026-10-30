package dev.meena.bigquery.init;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/** Starts the app with the sample init directory configured. */
public class InitSampleProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("emulator.init.dir", "src/test/resources/init-sample", "emulator.init.project", "loadertest");
    }
}
