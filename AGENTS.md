# AGENTS.md — BigQuery Emulator

This repository contains a local **Google Cloud BigQuery Emulator** built with **Java 25** and **Quarkus**, backed by a **PostgreSQL** database. It exposes BigQuery REST API v2 endpoints and translates BigQuery SQL (Standard SQL) into PostgreSQL SQL using **Apache Calcite**.

---

## Tech Stack

- **Language & Runtime:** Java 25
- **Framework:** Quarkus 3.37.1 (`quarkus-rest`, `quarkus-rest-jackson`, `quarkus-jdbc-postgresql`)
- **SQL Parsing & Dialect Translation:** Apache Calcite 1.42.0 (`calcite-core`)
- **Backing Database:** PostgreSQL (`AgroalDataSource` connection pool)
- **API Models:** Generated from `src/main/resources/bigquery-openapi.yaml` via `openapi-generator-maven-plugin` into package `dev.meena.bigquery.model`, serialized with Gson (`dev.meena.bigquery.JSON`) and Jackson
- **Testing:** JUnit 5 (`@QuarkusTest`), REST-Assured, Testcontainers (`postgres:18-alpine`), and the official Google Cloud BigQuery Java SDK (`com.google.cloud:google-cloud-bigquery`)

---

## Build, Run & Test Commands

Use the Maven wrapper (`./mvnw`) at the repository root:

```bash
# Run in Quarkus dev mode (default HTTP port: 9050)
./mvnw quarkus:dev

# Run unit and integration tests (automatically starts Postgres via Testcontainers)
./mvnw test

# Run a single test class
./mvnw test -Dtest=Bq2PsqlTest
./mvnw test -Dtest=TestJobsController

# Package JVM artifact
./mvnw package

# Build GraalVM native executable
./mvnw package -Dnative
```

- **Default Local Configuration (`src/main/resources/application.properties`):**
  - HTTP Port: `9050`
  - JDBC URL: `jdbc:postgresql://localhost:5432/bigquery` (`postgres` / `postgres`)

---

## Project Structure

```text
src/main/java/dev/meena/bigquery/
├── JSON.java                        # Custom Gson setup & TypeAdapters for OpenAPI BigQuery models
├── config/
│   ├── CalciteNativeReflectionConfig.java # GraalVM native image reflection config for Calcite
│   └── GzipProvider.java            # JAX-RS filter decompressing Content-Encoding: gzip requests
├── controllers/                     # One controller per REST resource
│   ├── MainController.java          # Root health check (GET /) & direct SQL translation/execution (POST /)
│   ├── QueryController.java         # /projects/{p}/queries (sync query, query results)
│   ├── JobController.java           # /projects/{p}/jobs (create, get, delete)
│   ├── DatasetController.java       # /projects/{p}/datasets (list, get, insert, delete) -> Postgres schemas
│   ├── TableController.java         # /projects/{p}/datasets/{d}/tables (list, get, insert, delete)
│   ├── TableDataController.java     # .../tables/{t}/data & insertAll
│   └── UnimplementedController.java # 501 catch-all for any other /bigquery/v2/** call
├── db/
│   ├── Catalog.java                 # Parameterized information_schema lookups (datasets, tables, columns)
│   └── Postgres.java                # JDBC query execution, DDL execution, row counting, batch insertAll
├── dialect/
│   ├── SqlSyntaxConverter.java      # Interface for BigQuery -> target SQL conversion
│   ├── Bq2Psql.java                 # Calcite-based AST parser, table identifier rewriter, DDL & insertAll translator
│   └── BqToPgDialect.java           # Custom Calcite PostgresqlSqlDialect (JSON_EXTRACT, JSON_VALUE, quoting)
├── fn/
│   ├── Fn.java                      # Schema naming (`toSchema`), identifier quoting, SQL text helpers
│   └── ByteString.java              # Base64 / hex byte wrapper
├── jobs/
│   ├── Disposition.java             # BigQuery createDisposition & writeDisposition enums
│   ├── ErrorMessage.java            # Error payload stored with a failed job
│   └── PgJobStorage.java            # Job persistence in "bq$jobs".details & query result table materialization
└── responses/                       # Builders for REST API response payloads
    ├── Errors.java                  # BigQuery error envelope (`bigQueryError`, `toError`)
    ├── QueryResponses.java          # Query/job results: schema, rows, job reference
    ├── CellValues.java              # Postgres value -> REST cell value encoding
    ├── TableResponses.java          # Table, table list and table data payloads
    ├── DatasetResponses.java        # Dataset and dataset list payloads
    ├── JobBuilder.java              # Fluent builder for BigQuery Job responses
    └── UnimplementedResponse.java   # Diagnostic response for unimplemented REST paths
```

---

## Core Architecture & Conventions

### 1. BigQuery to PostgreSQL Namespace Mapping
PostgreSQL does not support 3-part table references (`project.dataset.table`) in the same way BigQuery does. The emulator maps BigQuery `projectId` and `datasetId` into a single PostgreSQL schema named:
```text
"<project_id_lowercase>$<dataset_id_lowercase>"
```
- **3-part identifier:** `` `myProj`.`myDataset`.`myTable` `` $\rightarrow$ `"myproj$mydataset".myTable`
- **2-part identifier (with default project `myProj`):** `` `myDataset`.`myTable` `` $\rightarrow$ `"myproj$mydataset".myTable`
- Always use `Fn.toSchema(projectId, datasetId)` when constructing PostgreSQL schema names in Java code.

### 2. SQL Translation Pipeline (`dev.meena.bigquery.dialect`)
When adding or fixing SQL translation features:
1. **DDL Pre-Pass (`Bq2Psql.java`):**
   - `DROP TABLE [IF EXISTS]` and `CREATE [OR REPLACE] TABLE [IF NOT EXISTS]` statements are matched via regex before Calcite parsing so their target table identifiers can be rewritten to `"<project>$<dataset>".<table>`.
2. **Calcite AST Parsing & Rewriting (`Bq2Psql.java`):**
   - Parsed using `SqlParser.config().withConformance(SqlConformanceEnum.BIG_QUERY).withLex(Lex.BIG_QUERY).withCaseSensitive(false)`.
   - `SqlSelectShuttle` walks the AST (`FROM`, `JOIN`, subqueries, aliases) and rewrites 2-part and 3-part `SqlIdentifier` table references into `"<project>$<dataset>"` schema notation while preserving table aliases.
3. **Dialect Unparsing (`BqToPgDialect.java`):**
   - Extends Calcite's `PostgresqlSqlDialect`.
   - Translates BigQuery functions/operators to PostgreSQL equivalents during `unparseCall`:
     - `JSON_EXTRACT(col, '$.a.b[0]')` $\rightarrow$ `col::jsonb #> '{a, b, 0}'`
     - `JSON_VALUE(col, '$.a.b[0]')` $\rightarrow$ `col::jsonb #>> '{a, b, 0}'`
   - Prefer adding function and operator rewrites in `BqToPgDialect.unparseCall` whenever Calcite can parse the syntax into a `SqlCall`, and reserve regex pre-processing in `Bq2Psql` only for statements Calcite's `parseQuery()` cannot parse directly.

### 3. Job Execution & Materialization (`PgJobStorage.java`)
- Job metadata is persisted in PostgreSQL table `"bq$jobs".details`.
- Query jobs materialize their output into a destination table (either user-supplied in `JobConfigurationQuery.destinationTable` or auto-generated under `"bq$jobs".<jobId>`).
- Respect BigQuery dispositions (`Disposition.java`):
  - **Create Dispositions:** `CREATE_IF_NEEDED` (default), `CREATE_NEVER`
  - **Write Dispositions:** `WRITE_TRUNCATE`, `WRITE_APPEND`, `WRITE_EMPTY` (default)

### 4. OpenAPI Models & Serialization
- Models in `dev.meena.bigquery.model.*` are generated from `src/main/resources/bigquery-openapi.yaml` and **committed** under `src/main/java/dev/meena/bigquery/model/`. They are not regenerated by a normal build. After changing the spec run `./mvnw generate-sources -Pgenerate-models`, then register any new model's `CustomTypeAdapterFactory` in `JSON.java`. **Do not hand-edit the generated model files.**
- Every generated model carries `@RegisterForReflection(ignoreNested = false)` (via `additionalModelTypeAnnotations`) so Jackson/Gson work in the GraalVM native image. DTO records returned from controllers need the same annotation (use `ignoreNested = false` when they have nested records).
- BigQuery API payloads use `JSON.getGson()` for serialization/deserialization to properly handle OpenAPI nullable fields, date formats, and custom type adapters.

---

## Guidelines for Making Changes

1. **Test Both Unit Translation and End-to-End SDK Behavior:**
   - Add fast unit tests in `src/test/java/dev/meena/bigquery/dialect/Bq2PsqlTest.java` for any SQL syntax, identifier, or function translation changes.
   - Add integration tests in `TestJobsController` (queries & jobs), `TestDatasetController` or `TestTableController` using the official `com.google.cloud.bigquery.BigQuery` client against the Testcontainers PostgreSQL instance.
   - Test database seed data lives in `src/test/resources/migration/init.sql` (default test project: `test`, dataset: `public` $\rightarrow$ Postgres schema `"test$public"`).
2. **Error Handling:**
   - Return errors matching BigQuery's standard error envelope (`{"error": {code, message, status, errors[]}}`) using `Errors.toError(Exception)` / `Errors.bigQueryError(status, message)` so the Google Cloud BigQuery SDK parses status codes and messages accurately.
3. **Parameterization & Quoting:**
   - Always quote dynamic schema, table, and column identifiers safely when constructing SQL strings (`Fn.quoteIdentifier` / `Bq2Psql.quoteIdentifier`).
   - Use parameterized `PreparedStatement` batches for data insertion (see `Bq2Psql.insertAll` and `Postgres.executeInsertAll`).
4. **Native Image Compatibility:**
   - If introducing classes accessed reflectively by Calcite or Gson, register them in `CalciteNativeReflectionConfig.java` or `JSON.java`.
