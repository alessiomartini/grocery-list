-- Schema del database D1 "pantry". Gia' applicato in produzione; tenuto qui come
-- riferimento e per poter ricreare il database da zero.
--
-- Gli istanti sono epoch millis e le scadenze epoch day, gli stessi formati che Room usa
-- gia' sul device: nessuna conversione, e il confronto last-write-wins resta numerico.

CREATE TABLE IF NOT EXISTS items (
  uuid TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 1,
  unit TEXT NOT NULL DEFAULT '',
  category TEXT NOT NULL DEFAULT 'Other',
  status TEXT NOT NULL,
  expiry_epoch_day INTEGER,
  added_at INTEGER NOT NULL,
  status_changed_at INTEGER NOT NULL,
  -- Cancellazione soft: una riga rimossa deve restare visibile alla sincronizzazione,
  -- altrimenti il device non puo' distinguerla da una che non ha mai visto.
  expiry_notified INTEGER NOT NULL DEFAULT 0,
  deleted INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_items_updated_at ON items(updated_at);

CREATE TABLE IF NOT EXISTS purchase_history (
  uuid TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  category TEXT NOT NULL,
  purchased_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_purchases_updated_at ON purchase_history(updated_at);
