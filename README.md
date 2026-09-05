# Pantry

App Android nativa (Kotlin + Jetpack Compose, interfaccia in inglese) per gestire la spesa di casa: cosa comprare, cosa hai già in dispensa, quando scadono i prodotti e idee di ricette con quello che hai a disposizione.

Pensata per sostituire il workflow "lista con spunte" di Google Keep: quando compri un prodotto lo spunti (passa in dispensa), quando lo finisci lo spunti di nuovo (torna nella lista della spesa). A differenza di Keep, i dati sono salvati in un database locale sul telefono e non si perdono per una sincronizzazione andata male.

## Funzionalità

- **Lista a griglia stile Keep**: sezione "Da comprare" e "In dispensa", articoli raggruppati per categoria. Tocco breve su un articolo → cambia stato (comprato/finito, con scadenza opzionale). Tocco lungo → modifica nome, quantità, unità, categoria, scadenza o elimina l'articolo.
- **Scadenze**: schermata dedicata con tutti i prodotti in dispensa che hanno una data di scadenza, ordinati ed evidenziati (scaduto / oggi / entro 3 giorni). Notifica push giornaliera se qualcosa sta per scadere. Quando spunti un articolo come comprato, la data di scadenza viene stimata **e applicata subito, senza chiedere conferma**, in base al tipo di alimento (es. latte ~7 giorni, pasta ~1 anno, pollo fresco ~2 giorni). Per correggere la stima basta il tocco lungo → modifica scadenza.
- **Ricette**: genera 3 idee di ricette in base a quello che hai in dispensa, usando l'API di Gemini (Google), che ha un piano gratuito. Richiede una tua API key personale, inserita nelle Impostazioni.
- **Cronologia acquisti**: ogni volta che spunti un articolo come comprato viene registrato in una cronologia acquisti separata (sopravvive anche se poi modifichi o elimini l'articolo). Le statistiche derivate (quante volte compri ogni cosa, ogni quanti giorni in media) non sono nell'app, ma su un sito compagno — vedi sotto.
- **Aggiornamenti automatici**: bottone in Impostazioni per controllare se è disponibile una versione più recente dell'app e installarla, dato che l'app non è distribuita tramite Play Store. Ogni push su questo repository pubblica automaticamente una nuova build — non serve creare tag o versioni a mano.
- Tutti i dati (lista, dispensa, scadenze, cronologia acquisti) restano **sul dispositivo**, salvati con Room/SQLite. Nessun account, nessun cloud.

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

Il workflow `.github/workflows/build-apk.yml` compila l'app e ne esegue i test su ogni push, su GitHub Actions — non nell'ambiente di sviluppo remoto usato per scrivere il codice, che non ha accesso all'Android SDK. È così che il codice viene verificato: [![Build APK](https://github.com/alessiomartini/grocery-list/actions/workflows/build-apk.yml/badge.svg)](https://github.com/alessiomartini/grocery-list/actions/workflows/build-apk.yml)

**Ogni push su un branch** (non le pull request) pubblica anche automaticamente l'APK come [Release GitHub taggata `latest`](https://github.com/alessiomartini/grocery-list/releases/tag/latest), sovrascrivendo quella precedente — nessun tag manuale da creare. `versionCode`/`versionName` vengono impostati dalla CI in base al numero di run del workflow (`APP_VERSION_CODE`/`APP_VERSION_NAME`, letti da `app/build.gradle.kts` via variabili d'ambiente), quindi ogni build pubblicata ha una versione unica e crescente.

## Configurare le ricette (API key Gemini, gratuita)

1. Crea una API key gratuita su [aistudio.google.com/apikey](https://aistudio.google.com/apikey) (basta un account Google, nessuna carta di credito).
2. Apri l'app → icona ingranaggio (Impostazioni) → incolla la chiave.
3. La chiave viene salvata **solo sul telefono**, cifrata con `EncryptedSharedPreferences` (chiave di cifratura nell'Android Keystore), ed esclusa dai backup automatici. Viene usata solo per chiamare `generativelanguage.googleapis.com` quando premi "Suggest recipes".

Il modello di default è `gemini-2.0-flash`; puoi cambiarlo nel campo "Model" nelle Impostazioni se Google ne rilascia uno più recente sul piano gratuito.

⚠️ Sul piano gratuito di Gemini, Google può usare i prompt inviati per migliorare i propri modelli (diversamente dal piano a pagamento). Per una richiesta come "questi sono gli ingredienti che ho in dispensa" non è un problema serio, ma è bene saperlo.

Se non inserisci una chiave, tutto il resto dell'app (lista, dispensa, scadenze, notifiche) funziona comunque normalmente offline.

## Pubblicare un aggiornamento

Non serve fare niente di manuale: **basta pushare un commit** su questo repository.

1. Il workflow compila l'APK, calcola una versione dal numero di run (`versionCode` cresce sempre), e pubblica/aggiorna la Release GitHub taggata `latest` con l'APK e un file `version.txt`.
2. L'app confronta il proprio `versionCode` con quello in `version.txt`: se il remoto è più recente, Impostazioni → "Controlla aggiornamenti" propone il download.

Al primo utilizzo, Android chiederà il permesso di installare app da questa sorgente (necessario perché l'app non viene dal Play Store): l'app apre automaticamente le impostazioni di sistema corrette se il permesso non è ancora stato concesso.

### Firma dell'APK

Ogni build (CI o locale) firma l'APK con il keystore committato in `app/keystore/pantry.keystore` (password/alias in `app/build.gradle.kts`). È necessario perché Android rifiuta di installare un aggiornamento se la firma non coincide con quella già installata ("package conflicts with an existing package") — con una firma fissa, tutte le versioni pubblicate da qui in poi si aggiornano correttamente le une sulle altre. Non è un keystore di produzione/Play Store: va bene per un'app a uso personale distribuita fuori dallo store, non andrebbe usato così per un'app pubblicata pubblicamente.

Le versioni precedenti a **1.2** erano firmate con una chiave di debug generata a caso a ogni build CI: se hai installato una di quelle, disinstalla l'app prima di installare la 1.2 (un'unica volta) — dopodiché gli aggiornamenti torneranno a funzionare in-place.

## Struttura del progetto

```
app/src/main/java/com/alessiomartini/dispensa/
├── data/            Entity Room, DAO, database, repository della dispensa
├── settings/         Salvataggio cifrato della API key
├── network/           Chiamata all'API Gemini per le ricette
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
