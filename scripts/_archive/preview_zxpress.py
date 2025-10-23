# scripts/preview_zxpress.py
import argparse
import json
import re
from collections import defaultdict
from datetime import datetime

import yaml
import requests
from bs4 import BeautifulSoup

DEFAULT_HEADERS = {
    "User-Agent": "DigitProjectBot/1.0 (+github.com/your-org) Python-Requests",
    "Accept-Language": "ru,en;q=0.8,de;q=0.7"
}

def session():
    s = requests.Session()
    s.headers.update(DEFAULT_HEADERS)
    return s

def get_soup(url: str, sess=None):
    sess = sess or session()
    r = sess.get(url, timeout=30)
    r.encoding = "utf-8"
    return BeautifulSoup(r.text, "html.parser")

def absolute_url(base: str, href: str) -> str:
    if href.startswith("http"):
        return href
    return base.rstrip("/") + "/" + href.lstrip("/")

def parse_catalog(cfg):
    base = cfg["site"]["base_url"]
    start_url = cfg["site"]["start_url"]
    soup = get_soup(start_url)

    table = soup.select_one(cfg["site"]["selectors"]["catalog_table"]) or soup
    magazines = []
    for tr in table.find_all("tr"):
        tds = tr.find_all("td")
        if len(tds) < 5:
            continue
        name_td = tds[1]
        city_td = tds[2]
        form_td = tds[3]
        years_td = tds[4]

        a = name_td.find("a", href=True)
        if not a or "issue.php?id=" not in a["href"]:
            continue

        name = a.get_text(strip=True)
        mag_id = int(a["href"].split("=")[-1])
        magazines.append({
            "id": mag_id,
            "name": name,
            "issue_url": absolute_url(base, a["href"]),
            "city": city_td.get_text(" ", strip=True),
            "form": form_td.get_text(strip=True),
            "years": years_td.get_text(strip=True),
        })
    return magazines

def nearest_issue_label(a_tag):
    anchor = a_tag.find_all_previous("a", attrs={"name": True}, limit=1)
    if anchor:
        return anchor[0].get("name", "").strip()
    return ""

RU_MONTHS = {
    "января": "01", "февраля": "02", "марта": "03", "апреля": "04", "мая": "05", "июня": "06",
    "июля": "07", "августа": "08", "сентября": "09", "октября": "10", "ноября": "11", "декабря": "12"
}
DATE_RE = re.compile(r"(?P<d>\d{1,2})\s+(?P<m>[А-Яа-яёЁ]+)\s+(?P<y>\d{4})")

def parse_ru_date(text: str):
    if not text:
        return None
    m = DATE_RE.search(text.lower())
    if not m:
        return None
    d = int(m.group("d"))
    mru = m.group("m")
    y = int(m.group("y"))
    mm = RU_MONTHS.get(mru)
    if not mm:
        return None
    return f"{y:04d}-{mm}-{d:02d}"

def count_magazine(cfg, mag):
    """Zählt Ausgaben & Artikel für ein Magazin – ohne zu speichern."""
    base = cfg["site"]["base_url"]
    soup = get_soup(mag["issue_url"])
    left = soup.find("div", class_="col-left") or soup

    issue_map = defaultdict(lambda: {"date_human": None, "date_iso": None, "count": 0, "sample": []})

    for a in left.find_all("a", href=True):
        href = a["href"]
        if "article.php?id=" not in href:
            continue
        href = absolute_url(base, href)
        label = nearest_issue_label(a) or "unknown"

        # Datum in Nähe suchen (wie im Scraper)
        # Nimm das nächste vorherige DIV mit einer Jahreszahl
        divs = a.find_all_previous("div", string=re.compile(r"\d{4}"), limit=3)
        date_human = divs[0].get_text(strip=True) if divs else None
        date_iso = parse_ru_date(date_human) if date_human else None

        grp = issue_map[label]
        if grp["date_human"] is None:
            grp["date_human"] = date_human
        if grp["date_iso"] is None:
            grp["date_iso"] = date_iso

        grp["count"] += 1
        if len(grp["sample"]) < 3:
            grp["sample"].append(href)

    total_articles = sum(v["count"] for v in issue_map.values())
    return {
        "magazine_id": mag["id"],
        "magazine_name": mag["name"],
        "issues": [
            {
                "label": k,
                "date_human": v["date_human"],
                "date_iso": v["date_iso"],
                "articles": v["count"],
                "sample": v["sample"],
            }
            for k, v in sorted(
                issue_map.items(),
                key=lambda kv: -(int(re.sub(r"\D", "", kv[0]) or 0))
            )
        ],
        "issues_count": len(issue_map),
        "articles_count": total_articles,
    }

def main():
    ap = argparse.ArgumentParser(description="ZXPress Dry-Run/Vorzähler (ohne Speicherung)")
    ap.add_argument("--config", default="config/zxpress.yaml", help="Pfad zur YAML-Konfig")
    ap.add_argument("--mode", choices=["seeds", "all"], default="seeds", help="Seeds aus YAML oder kompletter Katalog")
    ap.add_argument("--json-out", default="", help="Optional: JSON-Summary in Datei schreiben")
    ap.add_argument("--show-samples", action="store_true", help="Beispiel-Links pro Ausgabe mit anzeigen")
    args = ap.parse_args()

    with open(args.config, "r", encoding="utf-8") as f:
        cfg = yaml.safe_load(f)

    # 1) Magazine bestimmen
    if args.mode == "all":
        magazines = parse_catalog(cfg)
    else:
        magazines = []
        for s in cfg.get("seeds", []):
            magazines.append({
                "id": s["magazine_id"],
                "name": s["magazine_name"],
                "issue_url": s["issue_url"],
                "city": None, "form": None, "years": None
            })

    # 2) Zählen
    grand_total_articles = 0
    grand_total_issues = 0
    results = []
    print(f"\n🧪 Dry-Run / Vorzähler – Modus: {args.mode}")
    print(f"Gefundene Magazine: {len(magazines)}\n")

    for mag in magazines:
        res = count_magazine(cfg, mag)
        results.append(res)
        grand_total_articles += res["articles_count"]
        grand_total_issues += res["issues_count"]

        print(f"📔 {res['magazine_name']}  (id={res['magazine_id']})")
        print(f"   Ausgaben: {res['issues_count']}  |  Artikel: {res['articles_count']}")
        for issue in res["issues"]:
            label = issue["label"]
            when = issue["date_iso"] or (issue["date_human"] or "")
            print(f"     • Ausgabe {label}  ({when})  – Artikel: {issue['articles']}")
            if args.show_samples and issue["sample"]:
                for s in issue["sample"]:
                    print(f"         → {s}")
        print()

    print("═" * 60)
    print(f"Σ Magazine: {len(magazines)}")
    print(f"Σ Ausgaben: {grand_total_issues}")
    print(f"Σ Artikel:  {grand_total_articles}")
    print("═" * 60)

    if args.json_out:
        payload = {
            "generated_at": datetime.utcnow().isoformat(),
            "mode": args.mode,
            "magazines_count": len(magazines),
            "issues_total": grand_total_issues,
            "articles_total": grand_total_articles,
            "magazines": results,
        }
        with open(args.json_out, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, indent=2)
        print(f"💾 JSON-Summary geschrieben nach: {args.json_out}")

if __name__ == "__main__":
    main()