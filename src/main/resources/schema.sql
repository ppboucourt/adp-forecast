CREATE TABLE IF NOT EXISTS observation (
  reference_month TEXT    NOT NULL,
  vintage_date    TEXT    NOT NULL,
  level           INTEGER NOT NULL,
  PRIMARY KEY (reference_month, vintage_date)
);
