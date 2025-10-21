# python scripts/light/scrape_issue_listing_light.py --mag "#Z80" --url "https://zxpress.ru/issue.php?id=1" --out data/zxpress/magazines/Z80
import argparse, os, re
from utils_light import get_soup, ensure_dir, dump_json, abs_url, parse_ru_single_date, parse_ru_year_span

DATE_RE = re.compile(r"\b(19|20)\d{2}\b")
DATE_STRICT_RE = re.compile(r'(\d{1,2})\s+([А-Яа-яA-Za-z]+)\s+(19|20)\d{2}')

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mag", required=True, help="Magazinname (z.B. #Z80)")
    ap.add_argument("--url", required=True, help="Magazinseite (issue.php?id=...)")
    ap.add_argument("--out", required=True, help="Zielordner für Magazin")
    args = ap.parse_args()

    mag_dir = ensure_dir(args.out)
    issues_dir = ensure_dir(os.path.join(mag_dir, "issues"))

    soup = get_soup(args.url)
    if not soup:
        raise SystemExit("Seite nicht ladbar")

    left = soup.find("div", class_="col-left") or soup

    # Magazin-Metadaten
    # Form, Name, Stadt/Land, Zeitraum, Anzahl Ausgaben
    form = (left.find("span") or {}).get_text(strip=True) if left.find("span") else None
    h1 = left.find("h1", class_="h1")
    mag_name = h1.get_text(strip=True) if h1 else args.mag
    info_div = left.find("div", style=lambda v: v and "font-size: 13pt" in v)
    city = country = years = issues_count = None
    if info_div:
        txt = info_div.get_text("\n", strip=True)
        # heuristisch
        lines = [l.strip() for l in txt.splitlines() if l.strip()]
        # Beispiel: "Пермь (Россия)" | "май 1998 – март 2000" | "6 выпусков"
        for l in lines:
            if "(" in l and ")" in l:
                city = l
            elif "выпуск" in l:
                m = re.search(r"(\d+)", l); issues_count = int(m.group(1)) if m else None
            elif any(m in l for m in ("январ", "феврал", "март", "апрел", "май", "июн", "июл", "август", "сентябр", "октябр", "ноябр", "декабр")):
                years = l

    dump_json(os.path.join(mag_dir, "magazine.json"), {
        "magazine_name": mag_name,
        "magazine_url": args.url,
        "form": form,
        "city_country": city,
        "years_human": years,
        "years_iso": (parse_ru_year_span(years) if years else {"start": "0000-01-01", "end": "0000-01-01"}),
        "issues_count": issues_count
    })

    issues = {}  # label -> {date_human, date_iso, articles: []}
    all_issues_meta = []  # für zentrales magazine/listing.json
    current_label = None

    # Durchlaufen in DOM-Reihenfolge, damit Artikel-Reihenfolge stimmt
    for node in left.descendants:
        name = getattr(node, "name", None)
        if name == "a" and node.has_attr("name"):
            current_label = node["name"].strip()
            issues[current_label] = {"date_human": None, "date_iso": None, "articles": []}
        elif name == "div":
            # Robuste Datumserkennung:
            # 1) bevorzugt die kleine Datumszeile mit Stil "font: 10pt; color: #312C12"
            # 2) Fallback: strikte Regex auf DD <russ. Monat> YYYY
            st = node.get("style") or ""
            text = node.get_text(" ", strip=True)
            if current_label and not issues[current_label]["date_human"]:
                picked = None

                # gezielter Stil-Match (auf ZXPress typisch für die Datumszeile)
                if "font: 10pt" in st and "color: #312C12" in st:
                    m = DATE_STRICT_RE.search(text)
                    if m:
                        picked = m.group(0)

                # Fallback: strikte Regex unabhängig vom Stil
                if not picked:
                    m = DATE_STRICT_RE.search(text)
                    if m:
                        picked = m.group(0)

                if picked:
                    issues[current_label]["date_human"] = picked
                    issues[current_label]["date_iso"] = parse_ru_single_date(picked)
        elif name == "a" and node.has_attr("href"):
            href = node["href"]
            if "article.php?id=" in href and current_label:
                url = abs_url(href)
                aid = int(href.split("=")[-1])
                title = node.get_text(" ", strip=True)
                issues[current_label]["articles"].append({
                    "article_id": aid,
                    "article_url": url,
                    "print_url": abs_url(f"print.php?id={aid}"),
                    "title_link": title
                })

    # schreiben
    for label, info in issues.items():
        iso = info["date_iso"] or "0000-01-01"
        issue_path = ensure_dir(os.path.join(issues_dir, f"{label}_{iso}"))
        dump_json(os.path.join(issue_path, "issue.json"), {
            "issue_label": label,
            "issue_date_human": info["date_human"],
            "issue_date_iso": info["date_iso"],
            "articles_count": len(info["articles"])
        })
        dump_json(os.path.join(issue_path, "listing.json"), info["articles"])
        # für zentrales Listing sammeln
        all_issues_meta.append({
            "issue_label": label,
            "issue_date_iso": info["date_iso"] or "0000-01-01",
            "articles_count": len(info["articles"])
        })

    # Zentrales Magazin-Listing schreiben (chronologisch nach Issue-Label, falls numerisch)
    def _label_sort_key(item):
        lbl = str(item.get("issue_label", "")).strip()
        if lbl.isdigit():
            return (0, int(lbl))
        return (1, lbl)

    all_issues_meta_sorted = sorted(all_issues_meta, key=lambda it: (_label_sort_key(it), it.get("issue_date_iso") or "9999-12-31"))
    dump_json(os.path.join(mag_dir, "listing.json"), all_issues_meta_sorted)
    if not all_issues_meta_sorted:
        with open(os.path.join(mag_dir, "EMPTY.txt"), "w", encoding="utf-8") as f:
            f.write("Magazin ohne Issues – laut Katalog vorhanden, Issueliste jedoch leer.")
    print(f"📄 Magazin-Listing gespeichert: {os.path.join(mag_dir, 'listing.json')}")

    print(f"✅ Fertig: {args.out}")

if __name__ == "__main__":
    main()