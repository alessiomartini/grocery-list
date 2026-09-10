# pantry-api

API di sincronizzazione per l'app Pantry: un Worker Cloudflare davanti a un database D1.

Serve un solo utente, autenticato con un token condiviso. Il telefono resta la copia
principale dei dati; il server esiste per il backup e per poter lavorare sui prodotti da
remoto.

## Come funziona la sincronizzazione

Ogni riga ha un `uuid` stabile e un `updatedAt` (epoch millis). Chi scrive per ultimo vince:
la `WHERE excluded.updated_at > items.updated_at` sulla `ON CONFLICT DO UPDATE` fa scartare
in silenzio le scritture piu' vecchie di quanto il server ha gia'. Con un solo utente questo
basta, e non serve gestire i conflitti.

Le cancellazioni sono **soft** (`deleted = 1`): una riga eliminata davvero sparirebbe dalla
pull incrementale, e il device non potrebbe distinguerla da una che non ha mai visto.

Date e istanti viaggiano come interi negli stessi formati che Room usa gia' sul device
(epoch millis, epoch day). Con le stringhe ISO il confronto sarebbe lessicografico e
`...:33Z` (33.000s) risulterebbe **maggiore** di `...:33.015Z`, perche' `.` viene prima di `Z`.

## Endpoint

Tutti richiedono `Authorization: Bearer <SYNC_TOKEN>`, tranne `/api/health`.

| Metodo | Path              | Descrizione |
|--------|-------------------|-------------|
| GET    | `/api/health`     | Controllo di vita, senza autenticazione. |
| GET    | `/api/items?since=<millis>` | Prodotti cambiati dopo `since`, cancellati inclusi. |
| POST   | `/api/items`      | Upsert last-write-wins. Body: `{ "rows": [...] }`. |
| GET    | `/api/purchases?since=<millis>` | Storico acquisti cambiato dopo `since`. |
| POST   | `/api/purchases`  | Upsert last-write-wins dello storico. |

Le pull restituiscono `serverTime`, da conservare e rimandare come `since` successivo.
E' preso all'inizio della richiesta apposta: cosi' una riga scritta mentre la pull e' in
corso viene al massimo rivista una seconda volta, mai saltata. Riapplicarla e' idempotente.

Se `hasMore` e' `true` ci sono altre righe oltre il limite di 500: richiama la pull usando
l'ultimo `updatedAt` ricevuto.

Un batch in POST viene validato tutto prima di scrivere: se una riga e' invalida, la
richiesta fallisce con `400` e non viene scritto niente.

## Configurazione

Servono tre secret nel repository GitHub (Settings → Secrets and variables → Actions):

| Secret | Cos'e' |
|--------|--------|
| `CLOUDFLARE_API_TOKEN` | Token API Cloudflare con permesso *Edit Cloudflare Workers*. |
| `CLOUDFLARE_ACCOUNT_ID` | ID dell'account Cloudflare. |
| `PANTRY_SYNC_TOKEN` | Stringa casuale inventata da te; la stessa va nelle impostazioni dell'app. |

Il deploy parte da solo a ogni push che tocca `backend/`, tramite
`.github/workflows/deploy-worker.yml`, che ricarica anche `SYNC_TOKEN` come secret del
Worker. L'unica copia da tenere aggiornata e' quella nei secret di GitHub.

Il database D1 (`pantry`) e' gia' creato; `schema.sql` serve solo come riferimento o per
ricrearlo da zero.

## Test

`npm test` non e' configurato: il Worker non ha dipendenze. La logica di last-write-wins
si verifica direttamente sul database, provando a riscrivere una riga con un `updated_at`
piu' vecchio e controllando che `changes` resti `0`.
