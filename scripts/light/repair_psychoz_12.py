# scripts/light/repair_psychoz_12.py
import os, json, re, unicodedata, sys

MAG = "data/zxpress/magazines/Psychoz"
A = os.path.join(MAG, "issues", "12_0000-01-01")
B = os.path.join(MAG, "issues", "12", "12_0000-01-01")

def J(p):
    with open(p, "r", encoding="utf-8") as f:
        return json.load(f)

def W(p, obj):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)

def slugify(text, maxlen=60):
    if not text: return "item"
    s = unicodedata.normalize("NFKD", text)
    s = "".join(ch for ch in s if ch.isalnum() or ch in (" ","_","-"))
    s = re.sub(r"\s+","_",s).strip("_")
    if len(s) > maxlen: s = s[:maxlen].rstrip("_")
    return s or "item"

if not (os.path.isdir(A) and os.path.isdir(B)):
    print("Nichts zu tun (A oder B fehlt).")
    sys.exit(0)

la = J(os.path.join(A, "listing.json"))
lb = J(os.path.join(B, "listing.json"))
if not isinstance(la, list) or not isinstance(lb, list):
    print("Listing-Struktur unerwartet.")
    sys.exit(1)

# Orders in A neu 1..n vergeben
for i, it in enumerate(la, 1):
    it["order"] = i
    it["issue_label"] = "12"
    if not it.get("short_slug") and it.get("title_link"):
        it["short_slug"] = slugify(it["title_link"], 60)

# B hinten anhängen, Orders fortlaufend
start = len(la)
for j, it in enumerate(lb, 1):
    it["order"] = start + j
    it["issue_label"] = "12"
    if not it.get("short_slug") and it.get("title_link"):
        it["short_slug"] = slugify(it["title_link"], 60)
    la.append(it)

# Issue-JSON von A anpassen
ij = J(os.path.join(A, "issue.json"))
ij["issue_label"] = "12"
ij.setdefault("issue_date_iso", "0000-01-01")
ij["articles_count"] = len(la)
ij["issue_slug"] = f'12_{ij.get("issue_date_iso") or "0000-01-01"}'

# Schreiben
W(os.path.join(A, "listing.json"), la)
W(os.path.join(A, "issue.json"), ij)

# Leeren B-Ordner entfernen (nur die untere Ebene, und parent löschen, wenn leer)
import shutil
shutil.rmtree(B, ignore_errors=True)
parent = os.path.dirname(B)
try:
    if os.path.isdir(parent) and not os.listdir(parent):
        os.rmdir(parent)
except Exception:
    pass

# Sicherstellen, dass articles/-Ordner existiert
os.makedirs(os.path.join(A, "articles"), exist_ok=True)

print(f"✅ Merge ok: {A} (articles_count={len(la)})")