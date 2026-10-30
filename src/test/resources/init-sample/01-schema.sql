-- Datasets and tables in BigQuery syntax
CREATE SCHEMA IF NOT EXISTS app OPTIONS(location = 'US');

CREATE TABLE app.people (
  id INT64 NOT NULL,
  name STRING,
  born DATE,
  score NUMERIC(10, 2),
  active BOOL,
  tags ARRAY<STRING>,
  created TIMESTAMP,
  profile JSON
)
OPTIONS(description = 'people; with a semicolon');

/* block comment; with semicolon */
CREATE TABLE app.events (id INT64, payload STRING);
