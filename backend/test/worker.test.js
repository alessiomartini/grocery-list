// Test del Worker di sync. Solo dati finti; nessuna chiamata di rete.
import { test, beforeEach } from "node:test";
import assert from "node:assert/strict";
import worker, { validateItem, validatePurchase } from "../src/worker.js";
import { createFakeD1 } from "./fake-d1.js";

const TOKEN = "test-token";
const U1 = "11111111-1111-4111-8111-111111111111";
const U2 = "22222222-2222-4222-8222-222222222222";

let env;
beforeEach(() => {
  env = { DB: createFakeD1(), SYNC_TOKEN: TOKEN };
});

const call = (method, path, { body, token = TOKEN, headers = {} } = {}) =>
  worker.fetch(
    new Request(`https://example.test${path}`, {
      method,
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
      body: body === undefined ? undefined : typeof body === "string" ? body : JSON.stringify(body),
    }),
    env
  );

const item = (over = {}) => ({
  uuid: U1,
  name: "test-item",
  quantity: 2,
  unit: "pz",
  category: "Other",
  status: "TO_BUY",
  updatedAt: 1000,
  ...over,
});

test("health e' pubblico", async () => {
  const res = await call("GET", "/api/health", { token: null });
  assert.equal(res.status, 200);
  assert.deepEqual(await res.json(), { ok: true });
});

test("auth: token mancante, sbagliato o non configurato -> 401", async () => {
  assert.equal((await call("GET", "/api/items", { token: null })).status, 401);
  assert.equal((await call("GET", "/api/items", { token: "wrong" })).status, 401);
  env.SYNC_TOKEN = undefined;
  assert.equal((await call("GET", "/api/items", { token: "" })).status, 401);
  assert.equal((await call("POST", "/api/items", { token: null, body: { rows: [] } })).status, 401);
});

test("auth: prefisso Bearer case-insensitive", async () => {
  const res = await call("GET", "/api/items", { token: null, headers: { Authorization: `bearer ${TOKEN}` } });
  assert.equal(res.status, 200);
});

test("rotta sconosciuta e metodo non supportato -> 404", async () => {
  assert.equal((await call("GET", "/api/nope")).status, 404);
  assert.equal((await call("DELETE", "/api/items")).status, 404);
});

test("slash finale ignorato", async () => {
  assert.equal((await call("GET", "/api/items/")).status, 200);
});

test("validateItem: payload valido con default", () => {
  const { errors, value } = validateItem({ uuid: U1, name: "  test-item ", status: "IN_PANTRY", updatedAt: "5" });
  assert.deepEqual(errors, []);
  assert.equal(value.name, "test-item");
  assert.equal(value.quantity, 1);
  assert.equal(value.category, "Other");
  assert.equal(value.addedAt, 5);
  assert.equal(value.statusChangedAt, 5);
  assert.equal(value.expiryEpochDay, null);
  assert.equal(value.deleted, 0);
});

test("validateItem: payload non validi", () => {
  assert.ok(validateItem({ ...item(), uuid: "abc" }).errors.includes("uuid non valido"));
  assert.ok(validateItem({ ...item(), name: "   " }).errors.includes("name mancante"));
  assert.ok(validateItem({ ...item(), status: "BOH" }).errors.some((e) => e.startsWith("status sconosciuto")));
  assert.ok(validateItem({ ...item(), updatedAt: undefined }).errors.length > 0);
  assert.ok(validateItem({ ...item(), updatedAt: "abc" }).errors.length > 0);
});

test("validateItem: quantity limitata, testi troncati", () => {
  assert.equal(validateItem(item({ quantity: -5 })).value.quantity, 0);
  assert.equal(validateItem(item({ quantity: 10 ** 9 })).value.quantity, 100000);
  assert.equal(validateItem(item({ name: "x".repeat(500) })).value.name.length, 200);
  assert.equal(validateItem(item({ unit: "u".repeat(500) })).value.unit.length, 60);
});

test("validatePurchase: valido e non valido", () => {
  const ok = validatePurchase({ uuid: U1, name: "test-item", purchasedAt: 7 });
  assert.deepEqual(ok.errors, []);
  assert.equal(ok.value.updatedAt, 7);
  assert.ok(validatePurchase({ uuid: U1, name: "x" }).errors.length > 0);
  assert.ok(validatePurchase({ uuid: "no", name: "", purchasedAt: 1 }).errors.length === 2);
});

test("push poi pull: round-trip degli item", async () => {
  const push = await call("POST", "/api/items", {
    body: { rows: [item(), item({ uuid: U2, name: "test-item-2", expiryEpochDay: 20000, expiryNotified: true, updatedAt: 2000 })] },
  });
  assert.equal(push.status, 200);
  const p = await push.json();
  assert.equal(p.received, 2);
  assert.equal(p.applied, 2);
  assert.equal(p.skipped, 0);

  const pulled = await (await call("GET", "/api/items")).json();
  assert.equal(pulled.count, 2);
  assert.equal(pulled.hasMore, false);
  assert.deepEqual(pulled.rows.map((r) => r.uuid), [U1, U2]);
  assert.equal(pulled.rows[1].expiryEpochDay, 20000);
  assert.equal(pulled.rows[1].expiryNotified, true);
  assert.equal(pulled.rows[0].deleted, false);

  const since = await (await call("GET", "/api/items?since=1000")).json();
  assert.deepEqual(since.rows.map((r) => r.uuid), [U2]);
});

test("last-write-wins: l'updatedAt piu' vecchio (o uguale) non sovrascrive", async () => {
  await call("POST", "/api/items", { body: { rows: [item({ name: "new", updatedAt: 2000 })] } });
  const older = await (await call("POST", "/api/items", { body: { rows: [item({ name: "old", updatedAt: 1500 })] } })).json();
  assert.equal(older.applied, 0);
  assert.equal(older.skipped, 1);
  const same = await (await call("POST", "/api/items", { body: { rows: [item({ name: "same", updatedAt: 2000 })] } })).json();
  assert.equal(same.applied, 0);
  const rows = (await (await call("GET", "/api/items")).json()).rows;
  assert.equal(rows[0].name, "new");
});

test("last-write-wins: l'updatedAt piu' nuovo sovrascrive", async () => {
  await call("POST", "/api/items", { body: { rows: [item({ name: "old", updatedAt: 1000 })] } });
  const res = await (await call("POST", "/api/items", { body: { rows: [item({ name: "new", updatedAt: 3000 })] } })).json();
  assert.equal(res.applied, 1);
  const rows = (await (await call("GET", "/api/items")).json()).rows;
  assert.equal(rows.length, 1);
  assert.equal(rows[0].name, "new");
  assert.equal(rows[0].updatedAt, 3000);
});

test("confronto numerico: 33000 < 33015 (millisecondi)", async () => {
  await call("POST", "/api/items", { body: { rows: [item({ name: "a", updatedAt: 33015 })] } });
  const res = await (await call("POST", "/api/items", { body: { rows: [item({ name: "b", updatedAt: 33000 })] } })).json();
  assert.equal(res.applied, 0);
});

test("cancellazione soft: la tombstone viaggia nella pull e vince se piu' nuova", async () => {
  await call("POST", "/api/items", { body: { rows: [item()] } });
  await call("POST", "/api/items", { body: { rows: [item({ deleted: true, updatedAt: 2000 })] } });
  const rows = (await (await call("GET", "/api/items?since=1000")).json()).rows;
  assert.equal(rows.length, 1);
  assert.equal(rows[0].deleted, true);
  // Una modifica stantia non resuscita la riga.
  await call("POST", "/api/items", { body: { rows: [item({ deleted: false, updatedAt: 1500 })] } });
  const again = (await (await call("GET", "/api/items")).json()).rows;
  assert.equal(again[0].deleted, true);
});

test("batch con una riga non valida: 400 e nessuna scrittura", async () => {
  const res = await call("POST", "/api/items", { body: { rows: [item(), item({ uuid: "bad", updatedAt: 5 })] } });
  assert.equal(res.status, 400);
  const body = await res.json();
  assert.equal(body.rejected[0].index, 1);
  assert.equal((await (await call("GET", "/api/items")).json()).count, 0);
});

test("body non valido, rows vuote, troppe righe", async () => {
  assert.equal((await call("POST", "/api/items", { body: "non json" })).status, 400);
  assert.equal((await call("POST", "/api/items", { body: { foo: 1 } })).status, 400);
  const empty = await call("POST", "/api/items", { body: { rows: [] } });
  assert.equal(empty.status, 200);
  assert.equal((await empty.json()).received, 0);
  const many = Array.from({ length: 501 }, () => item());
  assert.equal((await call("POST", "/api/items", { body: { rows: many } })).status, 413);
});

test("pull: paginazione con hasMore a 500 righe", async () => {
  const rows = Array.from({ length: 500 }, (_, i) =>
    item({ uuid: `00000000-0000-4000-8000-${String(i).padStart(12, "0")}`, updatedAt: 1000 + i })
  );
  await call("POST", "/api/items", { body: { rows } });
  await call("POST", "/api/items", { body: { rows: [item({ uuid: U2, updatedAt: 5000 })] } });
  const first = await (await call("GET", "/api/items")).json();
  assert.equal(first.count, 500);
  assert.equal(first.hasMore, true);
  const last = first.rows.at(-1).updatedAt;
  const next = await (await call("GET", `/api/items?since=${last}`)).json();
  assert.equal(next.count, 1);
  assert.equal(next.hasMore, false);
});

test("pull: since non numerico vale 0", async () => {
  await call("POST", "/api/items", { body: { rows: [item()] } });
  const res = await (await call("GET", "/api/items?since=abc")).json();
  assert.equal(res.since, 0);
  assert.equal(res.count, 1);
});

test("purchases: round-trip e last-write-wins", async () => {
  const p = { uuid: U1, name: "test-item", category: "Other", purchasedAt: 100, updatedAt: 100 };
  const a = await (await call("POST", "/api/purchases", { body: { rows: [p] } })).json();
  assert.equal(a.applied, 1);
  const stale = await (await call("POST", "/api/purchases", { body: { rows: [{ ...p, name: "old", updatedAt: 50 }] } })).json();
  assert.equal(stale.applied, 0);
  const fresh = await (await call("POST", "/api/purchases", { body: { rows: [{ ...p, name: "new", updatedAt: 200 }] } })).json();
  assert.equal(fresh.applied, 1);
  const pulled = await (await call("GET", "/api/purchases")).json();
  assert.equal(pulled.count, 1);
  assert.equal(pulled.rows[0].name, "new");
});

test("errore interno del DB -> 500 JSON", async () => {
  env.DB = { prepare: () => { throw new Error("boom"); } };
  const res = await call("GET", "/api/items");
  assert.equal(res.status, 500);
  assert.equal((await res.json()).error, "boom");
});
