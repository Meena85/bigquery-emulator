# Big Query Emulator

## Links

- [Google Discovery API](https://bigquery.googleapis.com/$discovery/rest?version=v2)
- [OpenAPI Spec](https://github.com/APIs-guru/openapi-directory/blob/main/APIs/googleapis.com/bigquery/v2/openapi.yaml)
- 

## Run with Docker Compose

```bash
docker compose up --build    # Postgres + the emulator as a native image (the first build takes a while)
docker compose down -v       # stop and delete the database
```

BigQuery clients connect to `http://localhost:9050` with any project id and no credentials.

Put BigQuery `CREATE SCHEMA` / `CREATE TABLE` / `INSERT` scripts (`*.sql`) and table data (`dataset.table.csv`) in
[`init/`](init/README.md). They are loaded once, when the database is created.
