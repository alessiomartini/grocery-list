# Pantry

Native Android app (Kotlin + Jetpack Compose) to manage household groceries: what to buy, what you already have in your pantry, when products expire, and recipe ideas based on what you have available.

Built to replace the "checklist" workflow of Google Keep: when you buy a product you check it off (it moves into the pantry), when you finish it you check it off again (it goes back to the shopping list). Unlike Keep, data is saved in a local database on the phone and doesn't get lost to a failed sync.

## Features

- **Keep-style grid list**: separate "To buy" and "In pantry" tabs (swipe left/right between all 4 tabs, or use the bottom nav), items grouped by category. Short tap on an item → changes status (bought/finished, with optional expiry date). Long press → edit name, quantity, unit, category, expiry date, or delete the item.
- **Expiry**: dedicated screen with all pantry products that have an expiry date, sorted and highlighted (expired / today / within 3 days). Daily push notification if something is about to expire. When you check an item off as bought, the expiry date is estimated **and applied immediately, with no confirmation step**, based on the type of food (e.g. milk ~7 days, pasta ~1 year, fresh chicken ~2 days). To correct the estimate, just long-press → edit expiry.
- **Recipes**: generates 3 recipe ideas based on what you have in your pantry, using Google's Gemini API, which has a free tier. Requires your own personal API key, entered in Settings.
- **Purchase history**: every time you check an item off as bought it's recorded in a separate purchase history (kept even if you later edit or delete the item). The stats derived from it (how many times you buy each thing, roughly how often) aren't in the app, but on a companion site — see below.
- **Automatic updates**: since the app isn't distributed through the Play Store, it checks its own GitHub build in the background (at most once every ~20h) and downloads + prompts to install newer builds on its own — Android still requires you to tap "Install" on the final confirmation, that step can't be skipped. Can be turned off in Settings, which also has a manual "Check for updates" button. Every push to this repository automatically publishes a new build — no need to create tags or versions by hand.
- All data (list, pantry, expiry dates, purchase history) stays **on the device**, saved with Room/SQLite. No account, no cloud.

## How to build

1. Open the project folder with **Android Studio** (Koala/2024.1 or newer).
2. Let Android Studio download the Gradle dependencies on first launch (needs an internet connection to `dl.google.com` and Maven Central, which this remote development environment doesn't have).
3. Run on a device/emulator with **Android 8.0 (API 26)** or higher.

Alternatively from the command line, once the Android SDK is installed:

```
./gradlew assembleDebug
```

The generated APK is located at `app/build/outputs/apk/debug/`.

### Automatic build (CI)

The `.github/workflows/build-apk.yml` workflow builds the app and runs its tests on every push, on GitHub Actions — not in the remote development environment used to write the code, which has no access to the Android SDK. This is how the code gets verified: [![Build APK](https://github.com/alessiomartini/grocery-list/actions/workflows/build-apk.yml/badge.svg)](https://github.com/alessiomartini/grocery-list/actions/workflows/build-apk.yml)

**Every push to a branch** (not pull requests) also automatically publishes the APK as a [GitHub Release tagged `latest`](https://github.com/alessiomartini/grocery-list/releases/tag/latest), overwriting the previous one — no manual tag to create. `versionCode`/`versionName` are set by the CI based on the workflow's run number (`APP_VERSION_CODE`/`APP_VERSION_NAME`, read from `app/build.gradle.kts` via environment variables), so every published build has a unique, increasing version.

## Setting up recipes (free Gemini API key)

1. Create a free API key at [aistudio.google.com/apikey](https://aistudio.google.com/apikey) (just needs a Google account, no credit card).
2. Open the app → gear icon (Settings) → paste the key.
3. The key is saved **only on the phone**, encrypted with `EncryptedSharedPreferences` (encryption key in the Android Keystore), and excluded from automatic backups. It's only used to call `generativelanguage.googleapis.com` when you tap "Suggest recipes".

The default model is `gemini-2.0-flash`; you can change it in the "Model" field in Settings if Google releases a newer one on the free tier.

⚠️ On Gemini's free tier, Google may use the prompts you send to improve their models (unlike the paid tier). For a request like "these are the ingredients I have in my pantry" this isn't a serious concern, but it's worth knowing.

If you don't enter a key, the rest of the app (list, pantry, expiry dates, notifications) still works normally offline.

## Publishing an update

No manual steps needed: **just push a commit** to this repository.

1. The workflow builds the APK, computes a version from the run number (`versionCode` always increases), and publishes/updates the GitHub Release tagged `latest` with the APK and a `version.txt` file.
2. The app compares its own `versionCode` against the one in `version.txt`: if the remote one is newer, it downloads it and prompts to install automatically in the background (checked at most once every ~20h), or you can trigger it right away from Settings → "Check for updates".

On first use, Android will ask for permission to install apps from this source (necessary because the app doesn't come from the Play Store): the app automatically opens the correct system settings if the permission hasn't been granted yet.

### APK signing

Every build (CI or local) signs the APK with the keystore committed at `app/keystore/pantry.keystore` (password/alias in `app/build.gradle.kts`). This is necessary because Android refuses to install an update if the signature doesn't match the one already installed ("package conflicts with an existing package") — with a fixed signature, every version published from now on updates correctly over the previous ones. This isn't a production/Play Store keystore: it's fine for a personal-use app distributed outside the store, but shouldn't be used this way for a publicly published app.

Versions before **1.2** were signed with a randomly generated debug key on every CI build: if you installed one of those, uninstall the app before installing 1.2 (just once) — after that, updates will go back to working in-place.

## Project structure

```
app/src/main/java/com/alessiomartini/dispensa/
├── data/              Room entities, DAO, database, pantry repository
├── settings/          Encrypted storage of the API key
├── network/           Call to the Gemini API for recipes
├── notifications/     Daily worker + expiry notifications
└── ui/
    ├── list/          "To buy" and "In pantry" screens (share the same grid UI)
    ├── expiry/        "Expiry" screen
    ├── recipes/       "Recipes" screen
    ├── settings/      "Settings" screen
    └── theme/         Material 3 theme
```

## Possible future improvements

- Barcode/receipt scanning to add products faster.
- Pantry backup/restore to Google Drive or similar (so it doesn't get lost like it did with Keep).
- Minimum quantities per category ("tell me when milk runs out two times in a row").
- Home screen widget for the shopping list.
