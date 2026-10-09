# Future architecture & open ideas

This file collects ideas that are not implemented yet, things left half-done, and
explicit "not doing this" decisions with their reasoning — so a future session
(human or Claude Code) doesn't have to rediscover the context.

## Ideas from the README, expanded

These were listed inline in `README.md` under "Possible future improvements"; kept
here with a bit more detail so they don't get lost.

- **Barcode/receipt scanning** to add products faster. Would need a camera
  permission + a barcode/OCR library (e.g. ML Kit) and a lookup step (barcode →
  product name), which is the hard part since there's no product database wired
  up yet.
- **Pantry backup/restore to Google Drive** or similar, so data survives a phone
  reset/loss the way it didn't with Google Keep. The sync backend (`backend/`)
  already gives a server-side copy, but it's not framed as a restorable backup
  from the UI (no "restore from server" flow after a fresh install).
- **Minimum quantities per category** ("tell me when milk runs out two times in a
  row") — needs tracking consumption streaks per category, not just per item.
- **Home screen widget** for the shopping list — a Glance/AppWidget target,
  independent of the rest of the app.

None of these are started; there's no partial branch or scaffolding for them.

## Left half-done / worth double-checking

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

## Local verification limitations (this dev environment)

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

## Public feedback / notes-from-users feature — intentionally not added

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
