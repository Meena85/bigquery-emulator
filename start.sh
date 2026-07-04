#!/usr/bin/env bash

EXPORT QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:3306/bigquery
EXPORT QUARKUS_DATASOURCE_USERNAME=mde-gsm-user
EXPORT QUARKUS_DATASOURCE_PASSWORD=mde-gsm-password

mvn quarkus:dev
