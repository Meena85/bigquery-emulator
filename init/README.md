# Initial data

Everything in this directory is loaded **once**, when the database is created, by the emulator at startup
(`EMULATOR_INIT_DIR`, mounted at `/init` by `docker-compose.yml`). Files are processed in name order, so prefix
them with numbers (`01-`, `02-`, ...).

| File | What it does |
|---|---|
| `*.sql` | BigQuery SQL script: `CREATE SCHEMA` (a dataset), `CREATE TABLE`, `INSERT`, `UPDATE`, `DELETE`. Statements are separated by `;`, comments (`--`, `#`, `/* */`) are allowed. Column types are BigQuery types (`INT64`, `STRING`, `ARRAY<...>`, ...). |
| `dataset.table.csv` or `project.dataset.table.csv` | Rows for an existing table (create it in an earlier `.sql` file). The first line holds the column names. An empty field is `NULL`, `""` is an empty string. Array columns are written as JSON (`"[""a"",""b""]"`), JSON columns as JSON text, `BYTES` as base64. |

Unqualified names belong to the project `EMULATOR_INIT_PROJECT` (`BQ_PROJECT` for compose, default `local`).

The whole load is one transaction: if any statement or row fails, nothing is created, the emulator stops with the
error and the load is attempted again at the next start. Once it has succeeded, a marker in the database
(`"bq$jobs".init_state`) prevents loading again, even if you change the files. To load again, recreate the database:

```bash
docker compose down -v && docker compose up
```
