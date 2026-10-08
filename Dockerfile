# Stage 1: Build native Quarkus executable using Mandrel (GraalVM for JDK 25)
FROM quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25 AS build

WORKDIR /code
USER quarkus

COPY --chown=quarkus:quarkus mvnw /code/mvnw
COPY --chown=quarkus:quarkus .mvn /code/.mvn
COPY --chown=quarkus:quarkus pom.xml /code/

# Pre-fetch dependencies for faster incremental builds
RUN ./mvnw dependency:go-offline -B

COPY --chown=quarkus:quarkus src /code/src

RUN ./mvnw package -Dnative -DskipTests -B

# Stage 2: Minimal runtime container
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.8

WORKDIR /work/
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

COPY --chown=1001:root --chmod=0755 --from=build /code/target/*-runner /work/application

ENV QUARKUS_HTTP_HOST=0.0.0.0 \
    QUARKUS_HTTP_PORT=9050 \
    QUARKUS_ANALYTICS_DISABLED=true \
    QUARKUS_HTTP_TEST_TIMEOUT=3s \
    QUARKUS_JACKSON_SERIALIZATION_INCLUSION=non-empty \
    QUARKUS_DATASOURCE_DB_KIND=postgresql \
    QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/bigquery \
    QUARKUS_DATASOURCE_USERNAME=postgres \
    QUARKUS_DATASOURCE_PASSWORD=postgres \
    QUARKUS_DATASOURCE_JDBC_MIN_SIZE=0 \
    QUARKUS_DATASOURCE_JDBC_MAX_SIZE=10 \
    QUARKUS_DATASOURCE_JDBC_INITIAL_SIZE=1 \
    QUARKUS_DATASOURCE_JDBC_ACQUISITION_TIMEOUT=5S

EXPOSE 9050
USER 1001

ENTRYPOINT ["./application"]

