DROP TABLE IF EXISTS "test$public".integration_test;

CREATE SCHEMA IF NOT EXISTS "test$public";
ALTER SCHEMA "test$public" OWNER TO tester;

CREATE SCHEMA IF NOT EXISTS "bq$jobs";
ALTER SCHEMA "bq$jobs" OWNER TO tester;

CREATE TABLE "test$public".integration_test
(
    id SERIAL
        CONSTRAINT integration_test_pk
            PRIMARY KEY,
    text VARCHAR,
    date DATE,
    data jsonb
);

CREATE TABLE "bq$jobs".details (
  id UUID PRIMARY KEY,
  project_id TEXT NOT NULL,
  job_id TEXT NOT NULL,
  location TEXT NOT NULL DEFAULT 'US',
  job_definition JSONB NOT NULL,
  destination_project_id TEXT NOT NULL,
  destination_dataset_id TEXT NOT NULL,
  destination_table_id TEXT NOT NULL,
  error JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at TIMESTAMPTZ,

  UNIQUE (project_id, job_id)
);

COMMENT ON TABLE "test$public".integration_test IS 'for bigquery-emulator tests';

ALTER TABLE "test$public".integration_test OWNER TO tester;

INSERT INTO "test$public".integration_test (text, date, data) VALUES ('item 1', '2026-06-05', '{"message": "ok","obj": { "a": "a-1", "b": 123 }}');
INSERT INTO "test$public".integration_test (text, date, data) VALUES ('item 2', '2026-06-06', '{"message": "ok","obj": { "a": "a-2", "b": 234 }}');
INSERT INTO "test$public".integration_test (text, date, data) VALUES ('item 3', '2026-06-07', '{"message": "ok","obj": { "a": "a-3", "b": 345 }}');
