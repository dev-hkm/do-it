CREATE TABLE IF NOT EXISTS backups (
  token_hash TEXT PRIMARY KEY,
  payload TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
