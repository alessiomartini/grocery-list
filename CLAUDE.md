# Claude Code instructions

## Project

Android grocery-list app ("Pantry" / `Dispensa`), Kotlin + Jetpack Compose,
single-user, with a small Cloudflare Worker + D1 backend (`backend/`) for sync.
Keep app, backend, and shared data responsibilities separate. See `README.md`
for features/build/publish, and `ROADMAP.md` for open ideas and
things intentionally not built.

## Verification

- Read `README.md` and the affected module before editing.
- Use the checked-in Gradle wrapper: `.\gradlew.bat <task>`.
- Run the narrowest relevant tests/checks, then build the affected module.
- Keep credentials, local configuration, keystores, and generated builds out of git.
- **This dev environment usually cannot run Gradle at all** (no Android SDK, and
  typically no JDK 17/21 — check with `java -version`; AGP 8.5.2 here needs JDK
  17, not whatever newer JDK may be installed). If `.\gradlew.bat help` fails
  immediately with just a bare version number as the error, that's this known
  toolchain gap, not a code problem — don't try to "fix" it by touching
  `gradle/libs.versions.toml`. Real verification happens in CI
  (`.github/workflows/build-apk.yml`), which runs on every push and has the SDK.
  State clearly in your report when you couldn't verify locally for this reason.
- `backend/` has no npm dependencies (see `backend/package.json`) — nothing to
  install or update there; CI calls `npx wrangler` directly. If you touch
  `backend/src/worker.js`, at minimum re-read `backend/README.md`'s "Test"
  section (manual last-write-wins check against D1) since there's no automated
  test suite for it yet.

## Workflow

- Preserve persistence and migration behavior (Room entities/DAOs in
  `app/src/main/java/.../data/`).
- Use fake/seed data for local verification; never use real personal data
  (this app stores real household grocery/pantry data for Alessio).
- Inspect `git diff` and report the exact Gradle tasks run before committing.
- Top-level docs (`README.md`, this file, `ROADMAP.md`) are in
  English; `backend/` docs and code comments are in Italian — match whichever
  file you're editing, don't convert one to the other.
- Don't add a public feedback/user-notes feature to this app or its backend —
  see the "Public feedback" section in `ROADMAP.md` for why.

## Roadmap, idee e progressi

`ROADMAP.md` nella root è l'**unico** file per todo, idee, decisioni e diario del progetto
(niente `FUTURE-ARCHITECTURE.md`, `IDEE.md`, `TODO.md`). È letto da https://alessiomartini.github.io/projects-overview/.
Sezioni: `## <fase>` (checkbox, contano nella percentuale), `## Idee` (checkbox, non contano),
`## Decisioni e cose provate` (prosa: scartato/rimandato e perché, provato e annullato, limiti e bug noti), `## Log`.

- Alla fine di ogni sessione di lavoro (prima del commit finale), aggiorna `ROADMAP.md`:
  - spunta (`- [x]`) i punti completati in questa sessione;
  - aggiungi come `- [ ]` i nuovi compiti nella fase giusta, e le idee non decise in `## Idee`;
  - annota in `## Decisioni e cose provate` ciò che è stato scartato, rimandato o annullato, col perché;
  - aggiungi in cima a `## Log` una riga `- AAAA-MM-GG: <cosa è stato fatto, in una frase>`
    (se esiste già una riga con la data di oggi, estendila invece di crearne un'altra).
- Non riscrivere né riordinare le voci esistenti senza motivo; non cancellare le voci completate.
- Se `ROADMAP.md` non esiste, crealo da `templates/ROADMAP.md` di projects-overview.
- Includi l'aggiornamento di `ROADMAP.md` nello stesso commit del lavoro.
