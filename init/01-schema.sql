-- Runs once, when the database is first created. Plain BigQuery SQL; statements end with ';'.
-- Tables are created in the project given by EMULATOR_INIT_PROJECT (default "local").

CREATE SCHEMA IF NOT EXISTS sample;

CREATE TABLE sample.people (
  id INT64 NOT NULL,
  name STRING,
  born DATE,
  score NUMERIC(10, 2),
  active BOOL,
  tags ARRAY<STRING>,
  created TIMESTAMP,
  profile JSON
);

CREATE TABLE sample.events (
  id INT64,
  at DATETIME,
  payload STRING
);
