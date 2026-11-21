// Finto binding D1 in memoria, basato su node:sqlite: esegue lo schema vero (schema.sql).
// Implementa solo quello che usa worker.js: prepare/bind/all/first/run e batch.
import { DatabaseSync } from "node:sqlite";
import { readFileSync } from "node:fs";

const SCHEMA = readFileSync(new URL("../schema.sql", import.meta.url), "utf8");

export function createFakeD1() {
  const db = new DatabaseSync(":memory:");
  db.exec(SCHEMA);

  const makeStatement = (sql, params = []) => ({
    bind: (...args) => makeStatement(sql, args),
    async all() {
      return { results: db.prepare(sql).all(...params), success: true };
    },
    async first() {
      return db.prepare(sql).get(...params) ?? null;
    },
    async run() {
      const r = db.prepare(sql).run(...params);
      return { success: true, meta: { changes: Number(r.changes) } };
    },
  });

  return {
    raw: db,
    prepare: (sql) => makeStatement(sql),
    // Come D1, il batch e' transazionale: se uno statement fallisce non resta niente.
    async batch(statements) {
      db.exec("BEGIN");
      try {
        const out = [];
        for (const s of statements) out.push(await s.run());
        db.exec("COMMIT");
        return out;
      } catch (err) {
        db.exec("ROLLBACK");
        throw err;
      }
    },
  };
}
