#!/usr/bin/env python3
r"""Importa lo storico acquisti Lidl (items.csv di life-log/lidl.py) in purchase_history sul D1.

    python tools/import_lidl.py "E:\Alessio\Dati-personali\lidl\items.csv"          # anteprima
    python tools/import_lidl.py "E:\Alessio\Dati-personali\lidl\items.csv" --apply  # scrive

Stesso PANTRY_URL / PANTRY_TOKEN di pantry.py. Gli uuid sono deterministici (scontrino+riga):
rilanciarlo non crea doppioni. Solo cibo e non-food, niente deposito (statiegeld).
"""
import csv, json, os, sys, urllib.request, uuid
from datetime import datetime, timezone
from pathlib import Path

env = Path(__file__).with_name(".env")
if env.exists():
    for line in env.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip())

NS = uuid.UUID("5c1f6a52-0d0e-4b0a-9a53-1d1d11d1d1d1")
CATEGORIA = {"cibo": "Other", "non-food": "Household and hygiene", "alcol": "Beverages"}  # ponytail: il catalogo dell'app raffina per nome

rows = []
for i, r in enumerate(csv.DictReader(open(sys.argv[1], encoding="utf-8"))):
    if r["cat"] not in CATEGORIA:
        continue
    ms = int(datetime.fromisoformat(r["data"]).replace(hour=12, tzinfo=timezone.utc).timestamp() * 1000)
    rows.append({"uuid": str(uuid.uuid5(NS, f'{r["scontrino"]}/{i}')), "name": r["nome"], "category": CATEGORIA[r["cat"]],
                 "purchasedAt": ms, "updatedAt": ms})
print(len(rows), "righe da importare;", rows[0]["name"], "...", rows[-1]["name"])
if "--apply" in sys.argv:
    url, token = os.environ["PANTRY_URL"].rstrip("/"), os.environ["PANTRY_TOKEN"]
    for k in range(0, len(rows), 200):
        req = urllib.request.Request(url + "/api/purchases", data=json.dumps({"rows": rows[k:k + 200]}).encode(),
                                     headers={"Authorization": "Bearer " + token, "Content-Type": "application/json", "User-Agent": "pantry-cli/1"})
        urllib.request.urlopen(req).read()
    print("scritto")
