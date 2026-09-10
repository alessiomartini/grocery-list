/**
 * API di sincronizzazione per Pantry.
 *
 * Serve un solo utente (l'app Android), autenticato con un token condiviso.
 *
 * Le date viaggiano nello stesso formato che Room usa gia' sul device - epoch millis per
 * gli istanti, epoch day per le scadenze - invece che come stringhe ISO. Cosi' il confronto
 * last-write-wins e' numerico: con le stringhe ISO "...:33Z" (33.000s) risulterebbe maggiore
 * di "...:33.015Z", perche' '.' viene prima di 'Z' nell'ordinamento lessicografico.
 */

const MAX_ROWS_PER_REQUEST = 500;
const MAX_NAME = 200;
const MAX_SHORT_TEXT = 60;
const STATUSES = new Set(["TO_BUY", "IN_PANTRY"]);
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const json = (data, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });

function authorized(request, env) {
  const token = (request.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  return Boolean(env.SYNC_TOKEN) && token === env.SYNC_TOKEN;
}

async function readBody(request) {
  try {
    const body = await request.json();
    return body && typeof body === "object" ? body : null;
  } catch {
    return null;
  }
}

/** Numero intero, oppure `fallback` se il valore non e' un numero finito. */
const int = (value, fallback = null) => {
  const n = Number(value);
  return Number.isFinite(n) ? Math.trunc(n) : fallback;
};

const text = (value, max) => String(value ?? "").trim().slice(0, max);

function validateItem(raw) {
  const errors = [];

  const uuid = text(raw.uuid, 36);
  if (!UUID_RE.test(uuid)) errors.push("uuid non valido");

  const name = text(raw.name, MAX_NAME);
  if (!name) errors.push("name mancante");

  const status = String(raw.status ?? "");
  if (!STATUSES.has(status)) errors.push(`status sconosciuto: ${status}`);

  const updatedAt = int(raw.updatedAt);
  if (updatedAt === null) errors.push("updatedAt mancante o non numerico");

  const addedAt = int(raw.addedAt, updatedAt);
  const statusChangedAt = int(raw.statusChangedAt, updatedAt);

  const quantity = Math.max(0, Math.min(int(raw.quantity, 1) ?? 1, 100000));

  return {
    errors,
    value: {
      uuid,
      name,
      quantity,
      unit: text(raw.unit, MAX_SHORT_TEXT),
      category: text(raw.category, MAX_SHORT_TEXT) || "Other",
      status,
      expiryEpochDay: raw.expiryEpochDay == null ? null : int(raw.expiryEpochDay),
      addedAt,
      statusChangedAt,
      expiryNotified: raw.expiryNotified ? 1 : 0,
      deleted: raw.deleted ? 1 : 0,
      updatedAt,
    },
  };
}

function validatePurchase(raw) {
  const errors = [];

  const uuid = text(raw.uuid, 36);
  if (!UUID_RE.test(uuid)) errors.push("uuid non valido");

  const name = text(raw.name, MAX_NAME);
  if (!name) errors.push("name mancante");

  const purchasedAt = int(raw.purchasedAt);
  if (purchasedAt === null) errors.push("purchasedAt mancante o non numerico");

  return {
    errors,
    value: {
      uuid,
      name,
      category: text(raw.category, MAX_SHORT_TEXT) || "Other",
      purchasedAt,
      updatedAt: int(raw.updatedAt, purchasedAt),
    },
  };
}

const itemRow = (row) => ({
  uuid: row.uuid,
  name: row.name,
  quantity: row.quantity,
  unit: row.unit,
  category: row.category,
  status: row.status,
  expiryEpochDay: row.expiry_epoch_day,
  addedAt: row.added_at,
  statusChangedAt: row.status_changed_at,
  expiryNotified: row.expiry_notified === 1,
  deleted: row.deleted === 1,
  updatedAt: row.updated_at,
});

const purchaseRow = (row) => ({
  uuid: row.uuid,
  name: row.name,
  category: row.category,
  purchasedAt: row.purchased_at,
  updatedAt: row.updated_at,
});

/**
 * Righe cambiate dopo `since`. Le cancellazioni sono soft (deleted = 1) proprio per poter
 * viaggiare qui: una riga sparita dalla tabella non sarebbe distinguibile da una mai vista.
 */
async function pull(env, url, table, mapRow, serverTime) {
  const since = int(url.searchParams.get("since"), 0);
  const { results } = await env.DB.prepare(
    `SELECT * FROM ${table} WHERE updated_at > ? ORDER BY updated_at ASC LIMIT ?`
  )
    .bind(since, MAX_ROWS_PER_REQUEST)
    .all();

  const rows = results ?? [];
  return json({
    serverTime,
    since,
    count: rows.length,
    // Con LIMIT raggiunto il client deve richiamare la pull usando l'ultimo updated_at.
    hasMore: rows.length === MAX_ROWS_PER_REQUEST,
    rows: rows.map(mapRow),
  });
}

/**
 * Upsert last-write-wins: la `WHERE` sulla DO UPDATE fa scartare le righe piu' vecchie di
 * quelle gia' sul server, quindi un client con dati stantii non sovrascrive nulla.
 */
async function pushItems(env, rows, serverTime) {
  const statements = rows.map((item) =>
    env.DB.prepare(
      `INSERT INTO items
         (uuid, name, quantity, unit, category, status, expiry_epoch_day,
          added_at, status_changed_at, expiry_notified, deleted, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(uuid) DO UPDATE SET
         name = excluded.name,
         quantity = excluded.quantity,
         unit = excluded.unit,
         category = excluded.category,
         status = excluded.status,
         expiry_epoch_day = excluded.expiry_epoch_day,
         added_at = excluded.added_at,
         status_changed_at = excluded.status_changed_at,
         expiry_notified = excluded.expiry_notified,
         deleted = excluded.deleted,
         updated_at = excluded.updated_at
       WHERE excluded.updated_at > items.updated_at`
    ).bind(
      item.uuid,
      item.name,
      item.quantity,
      item.unit,
      item.category,
      item.status,
      item.expiryEpochDay,
      item.addedAt,
      item.statusChangedAt,
      item.expiryNotified,
      item.deleted,
      item.updatedAt
    )
  );

  const results = await env.DB.batch(statements);
  const applied = results.reduce((sum, r) => sum + (r.meta?.changes ?? 0), 0);
  return json({ ok: true, received: rows.length, applied, skipped: rows.length - applied, serverTime });
}

async function pushPurchases(env, rows, serverTime) {
  const statements = rows.map((p) =>
    env.DB.prepare(
      `INSERT INTO purchase_history (uuid, name, category, purchased_at, updated_at)
       VALUES (?, ?, ?, ?, ?)
       ON CONFLICT(uuid) DO UPDATE SET
         name = excluded.name,
         category = excluded.category,
         purchased_at = excluded.purchased_at,
         updated_at = excluded.updated_at
       WHERE excluded.updated_at > purchase_history.updated_at`
    ).bind(p.uuid, p.name, p.category, p.purchasedAt, p.updatedAt)
  );

  const results = await env.DB.batch(statements);
  const applied = results.reduce((sum, r) => sum + (r.meta?.changes ?? 0), 0);
  return json({ ok: true, received: rows.length, applied, skipped: rows.length - applied, serverTime });
}

/** Valida l'intero batch prima di scrivere: o passa tutto, o non si scrive niente. */
async function handlePush(request, env, validate, write, serverTime) {
  const body = await readBody(request);
  if (!body || !Array.isArray(body.rows)) return json({ error: "serve un array 'rows'" }, 400);
  if (body.rows.length === 0) return json({ ok: true, received: 0, applied: 0, skipped: 0, serverTime });
  if (body.rows.length > MAX_ROWS_PER_REQUEST) {
    return json({ error: `massimo ${MAX_ROWS_PER_REQUEST} righe per richiesta` }, 413);
  }

  const validated = body.rows.map(validate);
  const rejected = validated
    .map(({ errors }, index) => (errors.length ? { index, errors } : null))
    .filter(Boolean);
  if (rejected.length) return json({ error: "righe non valide", rejected }, 400);

  return write(env, validated.map((v) => v.value), serverTime);
}

async function handleRequest(request, env) {
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";

  if (path === "/api/health") return json({ ok: true });

  if (!authorized(request, env)) return json({ error: "non autorizzato" }, 401);

  // serverTime viene preso prima di leggere, cosi' il client puo' usarlo come prossimo
  // watermark senza perdere righe scritte durante la richiesta: al massimo ne rivede
  // qualcuna, e riapplicarla e' idempotente.
  const serverTime = Date.now();

  if (path === "/api/items") {
    if (request.method === "GET") return pull(env, url, "items", itemRow, serverTime);
    if (request.method === "POST") return handlePush(request, env, validateItem, pushItems, serverTime);
  }

  if (path === "/api/purchases") {
    if (request.method === "GET") return pull(env, url, "purchase_history", purchaseRow, serverTime);
    if (request.method === "POST") {
      return handlePush(request, env, validatePurchase, pushPurchases, serverTime);
    }
  }

  return json({ error: "not found" }, 404);
}

export default {
  fetch: (request, env) =>
    handleRequest(request, env).catch((err) => json({ error: err.message }, 500)),
};

export { handleRequest, validateItem, validatePurchase };
