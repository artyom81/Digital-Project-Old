# /Users/stoia1/Desktop/Website/DigitProject/scripts/TextSearch/full_healthcheck.py
# Vollständiger Gesundheitscheck: Lucene-Index + Datei-/Metadatenquerschnitt

import os, json, lucene, re
from collections import Counter, defaultdict
from datetime import datetime
from java.nio.file import Paths
from org.apache.lucene.store import FSDirectory
from org.apache.lucene.index import DirectoryReader

CORPUS_ROOT = "/Users/stoia1/Desktop/Website/DigitProject/data/zxpress/magazines"
INDEX_DIR   = "/Users/stoia1/Desktop/Website/DigitProject/index_dir"

def count_files():
    txt = 0
    meta = 0
    issues = 0
    mags = 0
    missing = {"meta_missing": [], "text_missing": [], "issue_missing": [], "mag_listing_missing": []}

    for mag in os.listdir(CORPUS_ROOT):
        mag_path = os.path.join(CORPUS_ROOT, mag)
        if not os.path.isdir(mag_path): continue
        mags += 1

        # magazine.json / listing.json (auf Magazin-Ebene)
        if not os.path.exists(os.path.join(mag_path, "listing.json")):
            missing["mag_listing_missing"].append(mag)

        issues_path = os.path.join(mag_path, "issues")
        if not os.path.isdir(issues_path):
            missing["issue_missing"].append(mag)
            continue

        for issue in os.listdir(issues_path):
            issue_path = os.path.join(issues_path, issue)
            if not os.path.isdir(issue_path): continue
            issues += 1

            articles_path = os.path.join(issue_path, "articles")
            if not os.path.isdir(articles_path): continue

            for art in os.listdir(articles_path):
                art_path = os.path.join(articles_path, art)
                if not os.path.isdir(art_path): continue

                mp = os.path.join(art_path, "meta.json")
                tp = os.path.join(art_path, "text.txt")
                if os.path.exists(mp):
                    meta += 1
                else:
                    missing["meta_missing"].append(art_path)
                if os.path.exists(tp):
                    txt += 1
                else:
                    missing["text_missing"].append(art_path)

    return {
        "magazines": mags, "issues": issues,
        "articles_text": txt, "articles_meta": meta,
        "missing": missing
    }

def parse_year(iso):
    # iso kann None oder "0000-01-01" sein
    if not iso or not re.match(r"^\d{4}-\d{2}-\d{2}$", iso): return None
    try:
        return int(iso[:4])
    except:
        return None

def human_epoch(ms):
    try:
        return datetime.utcfromtimestamp(ms/1000).strftime("%Y-%m-%d")
    except:
        return None

def audit_index():
    lucene.initVM()
    r = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR)))
    n = r.numDocs()

    # Feldabdeckung
    fields = ["content", "magazine", "magazine_id_s", "form", "language", "city", "country",
              "issue_label", "issue_date_iso", "issue_date_epoch_ms", "article_id_s", "title", "article_url",
              "print_url"]

    coverage = Counter()
    empty_issue_date = 0
    placeholder_date = 0  # 0000-01-01
    years = Counter()
    forms = Counter()
    langs = Counter()
    mags = Counter()
    cities = Counter()
    countries = Counter()
    epoch_min, epoch_max = None, None

    # Stichprobenlisten (klein halten)
    missing_city_country_examples = []
    placeholder_examples = []
    missing_title_examples = []

    for doc_id in range(r.maxDoc()):
        d = r.storedFields().document(doc_id)
        for f in fields:
            if d.get(f): coverage[f] += 1

        mag = d.get("magazine")
        if mag: mags[mag] += 1

        f = d.get("form")
        if f: forms[f] += 1

        lg = d.get("language")
        if lg: langs[lg] += 1

        ct = d.get("city"); co = d.get("country")
        if ct: cities[ct] += 1
        if co: countries[co] += 1
        if not ct or not co:
            if len(missing_city_country_examples) < 10:
                missing_city_country_examples.append(d.get("title") or d.get("filename") or f"doc:{doc_id}")

        iso = d.get("issue_date_iso")
        if not iso: empty_issue_date += 1
        else:
            if iso == "0000-01-01":
                placeholder_date += 1
                if len(placeholder_examples) < 10:
                    placeholder_examples.append(d.get("title") or d.get("filename") or f"doc:{doc_id}")
            yr = parse_year(iso)
            if yr: years[yr] += 1

        epoch = d.get("issue_date_epoch_ms")
        if epoch:
            try:
                ms = int(epoch)
                epoch_min = ms if epoch_min is None else min(epoch_min, ms)
                epoch_max = ms if epoch_max is None else max(epoch_max, ms)
            except:
                pass

        if not d.get("title") and len(missing_title_examples) < 10:
            missing_title_examples.append(d.get("filename") or f"doc:{doc_id}")

    r.close()

    return {
        "docs": n,
        "coverage": {k: f"{coverage[k]}/{n}" for k in fields},
        "missing_issue_date_count": empty_issue_date,
        "placeholder_date_count": placeholder_date,
        "years_top": years.most_common(12),
        "forms": forms.most_common(),
        "languages": langs.most_common(),
        "magazines_top": mags.most_common(15),
        "cities_top": cities.most_common(15),
        "countries_top": countries.most_common(15),
        "epoch_span": {
            "min_epoch": epoch_min, "min_human": human_epoch(epoch_min) if epoch_min else None,
            "max_epoch": epoch_max, "max_human": human_epoch(epoch_max) if epoch_max else None,
        },
        "examples": {
            "missing_city_or_country": missing_city_country_examples,
            "placeholder_date": placeholder_examples,
            "missing_title": missing_title_examples,
        }
    }

def main():
    fs = count_files()
    ix = audit_index()

    print("=== FULL HEALTHCHECK ===============================")
    print(f"Magazines (dirs):       {fs['magazines']}")
    print(f"Issues (dirs):          {fs['issues']}")
    print(f"Articles text.txt:      {fs['articles_text']}")
    print(f"Articles meta.json:     {fs['articles_meta']}")
    print(f"Index docs (visible):   {ix['docs']}")
    print("— Datei/Index Konsistenz:",
          "OK" if fs['articles_text'] == ix['docs'] else f"DIFF (txt={fs['articles_text']} vs index={ix['docs']})")

    print("\n[Abdeckung gespeicherter Felder]")
    for k,v in ix["coverage"].items():
        print(f"  {k:18s} {v}")

    print("\n[Datum]")
    print(f"  ohne issue_date_iso:  {ix['missing_issue_date_count']}")
    print(f"  Platzhalter 0000-01-01: {ix['placeholder_date_count']}")
    print(f"  epoch-Spanne:          {ix['epoch_span']}")

    print("\n[Verteilung]")
    print("  Formen:", ix["forms"])
    print("  Sprachen:", ix["languages"])
    print("  Top-Magazine:", ix["magazines_top"][:10])
    print("  Top-Städte:", ix["cities_top"][:10])
    print("  Top-Länder:", ix["countries_top"][:10])
    print("  Top-Jahre:", ix["years_top"])

    # kurze Problemlisten
    miss = fs["missing"]
    any_missing = any(miss[k] for k in miss)
    print("\n[Fehlende Strukturen]")
    print(f"  Issues-Verzeichnis fehlt (Magazin): {len(miss['issue_missing'])}")
    print(f"  listing.json auf Magazin-Ebene fehlt: {len(miss['mag_listing_missing'])}")
    print(f"  meta.json fehlt (Artikel-Verz.): {len(miss['meta_missing'])}")
    print(f"  text.txt fehlt (Artikel-Verz.):  {len(miss['text_missing'])}")

    if any_missing:
        print("  Beispiele:")
        if miss['issue_missing'][:3]: print("   - issue_missing:", miss['issue_missing'][:3])
        if miss['mag_listing_missing'][:3]: print("   - mag_listing_missing:", miss['mag_listing_missing'][:3])
        if miss['meta_missing'][:3]: print("   - meta_missing:", miss['meta_missing'][:3])
        if miss['text_missing'][:3]: print("   - text_missing:", miss['text_missing'][:3])

    print("\n[Beispiele inhaltlicher Lücken]")
    ex = ix["examples"]
    print("  ohne city/country (bis 10):", ex["missing_city_or_country"])
    print("  Platzhalter-Datum (bis 10):", ex["placeholder_date"])
    print("  ohne title (bis 10):       ", ex["missing_title"])

if __name__ == "__main__":
    main()