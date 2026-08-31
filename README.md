# Dispensa

App Android nativa (Kotlin + Jetpack Compose) per gestire la spesa di casa: cosa comprare, cosa hai già in dispensa, quando scadono i prodotti e idee di ricette con quello che hai a disposizione.

Pensata per sostituire il workflow "lista con spunte" di Google Keep: quando compri un prodotto lo spunti (passa in dispensa), quando lo finisci lo spunti di nuovo (torna nella lista della spesa). A differenza di Keep, i dati sono salvati in un database locale sul telefono e non si perdono per una sincronizzazione andata male.

## Funzionalità

- **Lista unica stile Keep**: sezione "Da comprare" e "In dispensa", con checkbox. Comprato → in dispensa (con scadenza opzionale). Finito → torna da comprare.
- **Scadenze**: schermata dedicata con tutti i prodotti in dispensa che hanno una data di scadenza, ordinati ed evidenziati (scaduto / oggi / entro 3 giorni). Notifica push giornaliera se qualcosa sta per scadere.
- **Ricette**: genera 3 idee di ricette in base a quello che hai in dispensa, usando l'API di Claude (Anthropic). Richiede una tua API key personale, inserita nelle Impostazioni.
- **Aggiornamenti**: bottone in Impostazioni per controllare se è disponibile una versione più recente dell'app (pubblicata come Release su GitHub) e installarla, dato che l'app non è distribuita tramite Play Store.
- Tutti i dati (lista, dispensa, scadenze) restano **sul dispositivo**, salvati con Room/SQLite. Nessun account, nessun cloud.

## Come compilare

1. Apri la cartella del progetto con **Android Studio** (versione Koala/2024.1 o più recente).
2. Lascia che Android Studio scarichi le dipendenze Gradle al primo avvio (serve una connessione internet verso `dl.google.com` e Maven Central, che questo ambiente di sviluppo remoto non ha).
3. Esegui su un dispositivo/emulatore con **Android 8.0 (API 26)** o superiore.

In alternativa da riga di comando, una volta installato l'Android SDK:

```
./gradlew assembleDebug
```

L'APK generato si trova in `app/build/outputs/apk/debug/`.

### Build automatica (CI)

Il workflow `.github/workflows/build-apk.yml` compila l'app e ne esegue i test su ogni push (su GitHub Actions, non nell'ambiente di sviluppo remoto usato per scrivere il codice, che non ha accesso all'Android SDK). Controlla la tab **Actions** del repository per lo stato della build.

Quando fai push di un **tag `v*`** (es. `v1.1`), il workflow compila anche l'APK e lo pubblica automaticamente come GitHub Release con quel tag — è lo stesso meccanismo che alimenta il bottone "Controlla aggiornamenti" dell'app (vedi sotto).

## Configurare le ricette (API key Claude)

1. Crea una API key su [console.anthropic.com](https://console.anthropic.com).
2. Apri l'app → icona ingranaggio (Impostazioni) → incolla la chiave.
3. La chiave viene salvata **solo sul telefono**, cifrata con `EncryptedSharedPreferences` (chiave di cifratura nell'Android Keystore), ed esclusa dai backup automatici. Viene usata solo per chiamare `api.anthropic.com` quando premi "Suggerisci ricette".

Se non inserisci una chiave, tutto il resto dell'app (lista, dispensa, scadenze, notifiche) funziona comunque normalmente offline.

## Pubblicare un aggiornamento

Il bottone "Controlla aggiornamenti" nelle Impostazioni legge le [Release GitHub](https://github.com/alessiomartini/grocery-list/releases) di questo repository. Per pubblicare una nuova versione:

1. Alza `versionName` (e `versionCode`) in `app/build.gradle.kts`.
2. Genera l'APK firmato (`./gradlew assembleRelease`, oppure Build → Generate Signed App Bundle/APK in Android Studio).
3. Su GitHub, crea una nuova Release con **tag uguale al nuovo `versionName`** (es. `v1.1`) e allega il file `.apk` come asset della release.
4. Chi ha già installato l'app vedrà l'aggiornamento disponibile aprendo Impostazioni → "Controlla aggiornamenti".

Al primo utilizzo, Android chiederà il permesso di installare app da questa sorgente (necessario perché l'app non viene dal Play Store): l'app apre automaticamente le impostazioni di sistema corrette se il permesso non è ancora stato concesso.

## Struttura del progetto

```
app/src/main/java/com/alessiomartini/dispensa/
├── data/            Entity Room, DAO, database, repository della dispensa
├── settings/         Salvataggio cifrato della API key
├── network/           Chiamata all'API Claude per le ricette
├── notifications/     Worker giornaliero + notifiche di scadenza
└── ui/
    ├── list/          Schermata "Lista" (checklist da comprare / in dispensa)
    ├── expiry/         Schermata "Scadenze"
    ├── recipes/        Schermata "Ricette"
    ├── settings/       Schermata "Impostazioni"
    └── theme/          Tema Material 3
```

## Possibili miglioramenti futuri

- Scansione codice a barre / scontrino per aggiungere prodotti più velocemente.
- Backup/ripristino della dispensa su Google Drive o simili (per non perderla come è successo con Keep).
- Quantità minime per categoria ("avvisami quando il latte finisce due volte di seguito").
- Widget nella home screen per la lista della spesa.
