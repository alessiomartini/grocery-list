#!/usr/bin/env python3
"""CLI per la lista della spesa dal computer: scrive sullo stesso D1 con cui l'app sincronizza.

    PANTRY_URL=https://pantry-api.<sottodominio>.workers.dev PANTRY_TOKEN=... python tools/pantry.py list
    python tools/pantry.py add "Latte" [--category "Dairy and eggs"]
    python tools/pantry.py remove "Latte"

URL e token sono quelli di Impostazioni > Sync & backup nell'app (o in tools/.env, ignorato da git:
righe PANTRY_URL=... e PANTRY_TOKEN=...). L'app li raccoglie con la sync oraria: nessun update da installare.
"""
import argparse, json, os, sys, time, urllib.request, uuid
from pathlib import Path

env = Path(__file__).with_name(".env")
if env.exists():
    for line in env.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip())
URL, TOKEN = os.environ.get("PANTRY_URL", "").rstrip("/"), os.environ.get("PANTRY_TOKEN", "")
if not URL or not TOKEN:
    sys.exit("Imposta PANTRY_URL e PANTRY_TOKEN (vedi docstring).")


def call(path, body=None):
    req = urllib.request.Request(
        URL + path, data=None if body is None else json.dumps(body).encode(),
        headers={"Authorization": "Bearer " + TOKEN, "Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def all_items():
    rows, since = [], 0
    while True:
        page = call(f"/api/items?since={since}")
        rows += page["rows"]
        if not page["hasMore"]:
            return [r for r in rows if not r["deleted"]]
        since = page["rows"][-1]["updatedAt"]


def push(row):
    call("/api/items", {"rows": [row]})


p = argparse.ArgumentParser()
sub = p.add_subparsers(dest="cmd", required=True)
sub.add_parser("list")
a = sub.add_parser("add"); a.add_argument("name"); a.add_argument("--category", default="Other")
r = sub.add_parser("remove"); r.add_argument("name")
args = p.parse_args()
now = int(time.time() * 1000)

if args.cmd == "list":
    for i in sorted(all_items(), key=lambda i: (i["status"], i["category"], i["name"])):
        print(f'{i["status"]:9} {i["category"]:22} {i["name"]}')
elif args.cmd == "add":
    push({"uuid": str(uuid.uuid4()), "name": args.name, "quantity": 1, "unit": "", "category": args.category,
          "status": "TO_BUY", "expiryEpochDay": None, "addedAt": now, "statusChangedAt": now,
          "expiryNotified": False, "deleted": False, "updatedAt": now})
    print("aggiunto:", args.name)
else:
    hits = [i for i in all_items() if i["name"].lower() == args.name.lower()]
    if not hits:
        sys.exit("nessun prodotto con questo nome")
    for i in hits:
        push({**i, "deleted": True, "updatedAt": now})
    print("rimosso:", args.name, f"({len(hits)})")
