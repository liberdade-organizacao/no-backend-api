# Database Backup Refactor Plan

## Tasks

### 1. Refactor `to-recfile` in `jobs.clj`
- [ ] Change signature from `[table-name output-file]` to `[output-file & table-names]`.
- [ ] **Investigation**: Determine the most reliable way to retrieve all user-defined table names from SQLite (e.g., querying `sqlite_master`) so that the job can export all tables when no specific tables are provided.
- [ ] Implement logic to iterate through the requested `table-names` (or all detected tables if none were provided) and append them sequentially into the single `output-file`.

### 2. Refactor `from-recfile` in `jobs.clj`
- [ ] Update signature from `[table-name input-file]` to `[input-file]`.
- [ ] Implement logic to read the file, parse all table entries (handling multiple tables within one file), and perform upserts for each.

### 3. Update `makefile`
- [ ] Define `export_database` command: `lein run to-recfile <output_path>` (exports everything by default).
- [ ] Define `import_database` command: `lein run from-recfile <input_path>`.

### 4. Verification
- [ ] Verify that the new command line argument structure in `api.clj` works as intended with the refactored jobs.
