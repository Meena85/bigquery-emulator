package dev.meena.bigquery.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.meena.bigquery.model.TableCell;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Singleton;

@Singleton
public class JacksonConfig implements ObjectMapperCustomizer {

    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public abstract static class TableCellMixIn {
        @JsonInclude(JsonInclude.Include.ALWAYS)
        public abstract Object getV();
    }

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.addMixIn(TableCell.class, TableCellMixIn.class);
    }
}
