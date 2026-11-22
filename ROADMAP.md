# Roadmap

<!--
Single file for todos, ideas, decisions and diary, read by Projects Overview
(https://alessiomartini.github.io/projects-overview/). Protocol: templates/ROADMAP.md of projects-overview.
-->

## Ideas
- [ ] From the morning brief: if Google Tasks has a task like "add food item to the shopping list", the brief reads it and updates the list in the app. Alessio's words: "voglio implementare la funzione che se c'e' qualcosa nei tasks che dice "aggiungi alimento alla liste della spesa" allora il brief prende l'istruzione e modifica la lista nell'app Pantry" (from google-tasks, task "Fare la spesa e compilare la lista della spesa", 2026-11-07)
- [ ] Barcode / receipt scanning
- [ ] Pantry backup to Google Drive
- [ ] Minimum quantities per category
- [ ] Home screen widget
- [ ] Pull-to-refresh: pulling down at the top of the list reloads it and starts the sync
- [ ] Automatic sync when the app opens
- [ ] Food names with a capital first letter
- [ ] Food names in English

### Ideas from the README, expanded

These were listed inline in `README.md` under "Possible future improvements"; kept
here with a bit more detail so they don't get lost.

- [ ] **Barcode/receipt scanning** to add products faster. Would need a camera
  permission + a barcode/OCR library (e.g. ML Kit) and a lookup step (barcode →
  product name), which is the hard part since there's no product database wired
  up yet.
- [ ] **Pantry backup/restore to Google Drive** or similar, so data survives a phone
  reset/loss the way it didn't with Google Keep. The sync backend (`backend/`)
  already gives a server-side copy, but it's not framed as a restorable backup
  from the UI (no "restore from server" flow after a fresh install).
- [ ] **Minimum quantities per category** ("tell me when milk runs out two times in a
  row") — needs tracking consumption streaks per category, not just per item.
- [ ] **Home screen widget** for the shopping list — a Glance/AppWidget target,
  independent of the rest of the app.

None of these are started; there's no partial branch or scaffolding for them.

## To do
- [x] Catalog matching also for plurals and gender ("pomodori" → pomodoro, "mele" → mela): idea from PR #8, closed because superseded by #18 (which only ignores case, accents and spaces); to do after trying #18 on the phone
- [x] Food catalog check (names, icons, translations) and a guidance box in Recipes (2026-11-15)
- [x] Switch to turn multilingual on/off and option to have three languages (2026-11-14; preferences only, no migration; the catalog already has en/it/nl, so no missing data)
- [x] Sync bug: some food items are not synced (name typed by hand, no translation, no capital letter, no icon): find the cause and fix it (note 2026-11-13). Cause: sync carries the name as-is and the link to the catalog is only the name, compared exactly (case yes, but accents and double spaces no); rows from `pantry.py`/Worker stayed in "Other" with the raw name (recategorized only when the app restarted) and non-catalog names had no capital letter. Fix: `FoodInfo.tidyName` (comparison ignoring accents/spaces, catalog name or initial capital), `linkToCatalog` applied to every downloaded row and to every add/edit, initial capital also in the display; no migration, Worker unchanged. To verify on the phone with a real sync (no local build)
- [ ] Verify on the phone that food items added by `pantry.py` arrive with the catalog's name, icon, category and translations
- [x] (catalog extended 2026-11-15; history import: script ready, to be applied) (data ready 2026-11-15: 346 distinct products, frequencies and intervals in `E:\Alessio\Dati-personali\lidl\statistiche.md` and `items.csv`, from `life-log/lidl.py`; what remains is comparing them with `FoodCatalog.kt` and importing the history as `purchase_history`) Collect the receipts and give them to Claude to add the missing food items to the catalog and get new items suggested (note 2026-11-13)
- [ ] Misleading icons (mozzarella with the Gruyère, pasta with a plate of pasta): use images that recall the product on the shelf (note 2026-11-13)
- [x] Removed the quantity per product (2026-11-09; `quantity`/`unit` columns stay in Room and in the sync, no migration)
- [x] Seasonality tag for fruit/vegetables (monthly Netherlands table in `FoodInfo.kt`)
- [x] Names in two languages (it + nl by default, configurable in Settings) for catalog products
- [x] Editing the list from the computer: `tools/pantry.py` (list/add/remove) on the shared D1; the app syncs by itself
- [ ] Seasonality: where you live → grown nearby or far away (postponed: needs a country in Settings and an origin table per product)
- [ ] Product name in two languages for non-catalog products (needs a new column in Room + D1 + sync; today catalog only)
- [x] Verify the seasonal table against the updated Milieu Centraal calendar (2010 PDF incomplete, table rounded to months)
  - still not verifiable: potato (absent from the calendar, left year-round) and berries (mixed group, no label)
- [ ] Icons: today they are emoji in the catalog (no icon set in the repo); redraw the launcher icon only if Alessio specifies one
- [ ] Categories and catalog from the server instead of in the code: today `Categories.SUGGESTED` and `FoodCatalog` (category, icon, shelf life, aliases of each food item) are written in the APK, so a new category or a moved food item requires an app update. Product changes in the D1 already arrive with the sync; the goal is that a sync is enough also for categories and catalog (e.g. `categories`/`catalog` tables in the D1 downloaded by the Worker, with the catalog in the APK as an offline fallback)

## To verify
- [x] Sync backend tests (`npm test` in `backend/`, 20 tests with a fake D1 on `node:sqlite`)
- [ ] End-to-end sync from the phone (Settings → "Sync & backup" → Sync now) against the real Worker: so far only migration on SQLite with fake data and JSON format against `validateItem` have been verified
- [ ] The fallback Gemini models (`gemini-2.5-flash`, `gemini-2.5-flash-lite`, `gemini-2.0-flash`) still exist for the key in use: a retired one is skipped (404), but if all of them were, recipes would fail

## Decisions and things tried

This file collects ideas that are not implemented yet, things left half-done, and
explicit "not doing this" decisions with their reasoning — so a future session
(human or Claude Code) doesn't have to rediscover the context.

- 2026-11-21: seasonality verified against the Milieu Centraal calendar (PDF "Groente- en fruitkalender" linked from milieucentraal.nl/eten-en-drinken/milieubewust-eten/groente-en-fruit/, Dec 2023, Velt/Bionext data; the old site groentefruit.milieucentraal.nl no longer responds). Rule: a month is in season = product listed in that month's column (grown in the Netherlands, imports excluded). 10 products out of 17 corrected (e.g. tomato 5-10 → 6-10, strawberry 5-9 → 4-8, cucumber 5-10 → 7-10, leek 1-4+7-12 → year-round, cauliflower 6-11 → 3-11, corn 8-10 → 8-9), added chicory and fennel; potato not verifiable.
### Left half-done / worth double-checking

- The app now syncs with the backend (`network/SyncRepository.kt`, Settings →
  "Sync & backup", hourly `sync/SyncWorker.kt`). Restoring after a fresh install
  works by entering the same URL/token and syncing — the first pull starts from
  watermark 0 — but there's no dedicated "Restore from server" button or
  wording; it's discoverable only via the README. That probably covers the Google
  Drive backup idea above without needing Drive.
- Soft-deleted rows (`deleted = 1`) are never purged, locally or on D1. Harmless
  at personal scale; a cleanup would have to wait until every device has pulled
  the deletion, which a single-user app can't really know.
- Every sync pushes *all* local rows (no push watermark, by design — see the
  comment on `SyncRepository`). Fine for hundreds to low thousands of rows; revisit
  only if the purchase history ever grows far beyond that.
- `backend/README.md` says `npm test` isn't configured and the last-write-wins
  logic is verified manually against the D1 database. There's no automated test
  for `backend/src/worker.js` — worth adding a lightweight test (e.g. against
  `wrangler dev` + a local D1) if the sync logic changes again.

### Database version must never go down between published builds

- Every push to *any* branch republishes the single `latest` release, and the app
  installs whatever has the higher versionCode. On 2026-10-19 a push to `main`
  (Room database still v2) was published after the phone already had a build
  with database v3: Room found a v3 database with no 3→2 migration and the app
  crashed on every launch. Fixed by republishing from the v3 branch and then
  fast-forwarding `main` to it.
- `fallbackToDestructiveMigrationOnDowngrade()` was deliberately **not** added:
  it would "fix" the crash by wiping the pantry. Keep the published database
  version monotonic instead — never push to a branch whose Room version is lower
  than what's already been published.

### Local verification limitations (this dev environment)

- `./gradlew.bat help` fails here with just `25.0.2` as the error (Gradle/AGP
  8.5.2 doesn't support running on JDK 25 — it needs JDK 17, per the Android
  Gradle Plugin compatibility table). This environment only has JDK 25 installed
  and no Android SDK, so **no Gradle task can be run locally from this dev
  environment**, not even `help`. This isn't new: the README already documents
  that the real verification happens in CI (`.github/workflows/build-apk.yml`),
  since the remote development environment has no access to `dl.google.com` /
  Maven Central either. Don't try to "fix" this by changing the Gradle/AGP/Kotlin
  versions in `gradle/libs.versions.toml` — that's an unrelated, riskier change;
  if local Gradle access is ever needed, the fix is installing a JDK 17/21 and
  the Android SDK alongside the JDK 25 already on this machine, not downgrading
  the project.
- `backend/` has no dependencies (`backend/package.json` has no `dependencies` /
  `devDependencies` fields — it only uses `npx wrangler` from CI), so there was
  nothing to run `npm outdated` against.

### Public feedback / notes-from-users feature — intentionally not added

This project's docs mention a general idea (used as a template across repos) of
adding a lightweight public feedback feature — e.g. site visitors leaving notes
in a shared database. **This does not fit Pantry and is not planned:**

- Pantry is a single-user, personal/household app. It has no public visitors —
  it's installed by one person (Alessio) via a private GitHub Release APK, not
  distributed through the Play Store or any public listing. There's no audience
  to collect feedback *from*.
- The existing backend (`backend/`) is already scoped narrowly on purpose: it's
  a sync API for one authenticated user (shared bearer token, see
  `backend/README.md`), not a general-purpose public API. Bolting a public
  feedback/notes table onto it would widen its attack surface and blur its
  single responsibility for no real benefit.
- If Alessio ever wants a personal "notes to self about the app" feature (ideas,
  bugs, ✨future improvements✨ jotted down from the phone), the lowest-effort
  option — should it ever become worth doing — would be a `notes` table synced
  through the *existing* `backend/` sync API with the *existing* single-user
  token, mirroring how `items`/`purchases` already work. That's an optional,
  very-low-priority idea, not a task on any roadmap; there is currently no
  `notes` table, endpoint, or UI screen for it, and nothing here should be read
  as a plan to build one soon.

## Log
- 2026-11-22: 🤖 Deploy Worker workflow now runs only on pushes to main (before, test branches touching backend/ also deployed to production).
- 2026-11-21: 🤖 catalog matching also for plurals and gender (Italian rules -i/-e/-chi/-ghi/-che/-ghe, uova/uovo, English plurals, also in reverse) only as a fallback after the direct match, with tests; closed PR #8 (superseded by #18), plural matching noted as to do; 🤖 verified the NL seasonal table against the Milieu Centraal calendar (10 products corrected, added chicory and fennel, source and rule cited in the code, tests updated); added an automated test suite for the sync Worker (`node:test` + fake D1 on `node:sqlite`, 20 tests, no dependencies) and the CI workflow `backend-test.yml`; deploy no longer starts for changes to tests/README/package.json; fixed the sync bug on food items not linked to the catalog (raw name, no icon): name and category re-linked to the catalog on every downloaded row and on add/edit, with unit tests; 🤖 ROADMAP translated to English (language rule)
- 2026-11-15: added pine nuts (catalog + in the pantry)
- 2026-11-15: "in season / out of season" label replaced by a colored dot (green / brown) at the top right of the card
- 2026-11-15: names and categories of 36 hand-entered food items fixed in the database (English catalog names, capitals, wrong categories); left as they were "Baking mix - biscottini rotondi" and the duplicate "Ice cream"
- 2026-11-15: catalog check on the real list: +38 food items (spices, sauces, cheeses, etc.), icons and translations fixed; in Recipes a box to give Gemini guidance before pressing Snack/Meal (not verified locally, CI only)
- 2026-11-15: archiving of food items (`ARCHIVED` state, no Room migration; the Worker accepts the new state: backend deploy needed)
- 2026-11-15: catalog extended with the Dutch names from the Lidl receipts (PR #10, `FoodCatalogTest` test); `tools/import_lidl.py` imports the history (1,056 rows) into `purchase_history`, preview by default, `--apply` to be run by Alessio
- 2026-11-14: 🤖 multilingual switch and third-language option in Settings (preferences only); notes from the site brought into the ROADMAP
- 2026-11-09: from Google tasks: removed the quantity, NL seasonality tag, it/nl names, `tools/pantry.py` for the list from the computer (icons and "where you live" postponed)
- 2026-10-19: todo: categories and catalog synced from the server; new category "Herbs and spices" (salt, pepper, curry, parsley, mint and dried spices in the catalog); manually fixed the categories of 4 products in the D1; app↔backend sync (Room v3 with uuid/updatedAt/soft delete), retry and fallback of the Gemini models on 429/503, automatic recategorization of products in "Other", CI fix (setup-android `tools` package); fixed the crash from the database downgrade and aligned `main` with the branch; first successful sync (91 products on the server) and catalog extended so that it recognizes them all
- 2026-10-18: FUTURE-ARCHITECTURE.md merged into ROADMAP.md (single protocol)
- 2026-10-15: roadmap created for Projects Overview
