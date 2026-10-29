package dev.meena.bigquery.config;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(
        classNames = {
                "org.apache.calcite.jdbc.CalciteJdbc41Factory",
                "org.apache.calcite.avatica.AvaticaJdbc41Factory"
        }
)
public class CalciteNativeReflectionConfig {
    // This class remains empty. It serves only as a placeholder
    // to host the @RegisterForReflection annotation for 3rd-party classes.
}