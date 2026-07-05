#!/usr/bin/env bash

export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:3306/bigquery
export QUARKUS_DATASOURCE_USERNAME=mde-gsm-user
export QUARKUS_DATASOURCE_PASSWORD=mde-gsm-password
export QUARKUS_BANNER_ENABLED=false

mvn quarkus:dev
