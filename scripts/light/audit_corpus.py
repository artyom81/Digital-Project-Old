import os, json, argparse, re, csv
from datetime import datetime

def load_json(p):
    try:
        with open(p, "r", encoding="utf-8") as f: return json.load(f)
    except Exception:
        return None

def count_article_dirs(issue_dir):
    arts = os.path.join(issue_dir, "articles")
    if not os.path.isdir(arts): return None
    return sum(1 for d in os.listdir(arts) if os.path.isdir(os.path.join(arts, d)))

def newest_log_for_mag(log_dir, mag_name):
    if not os.path.isdir(log_dir): return None
    # Logs sind von uns als validate_<MAG>_YYYYMMDD_HHMMSS.txt benannt
    prefix = f"validate_{mag_name}_"
    candidates = [f for f in os.listdir(log_dir) if f.startswith(prefix) and f.endswith(".txt")]
    if not candidates:
        # Fallback: irgendein validate_*.txt, später per Inhalt prüfen
        candidates = [f for f in os.listdir(log_dir) if f.startswith("validate_") and f.endswith(".txt")]
        if not candidates: return None
    # Neueste per mtime
    candidates = sorted(candidates, key=lambda fn: os.path.getmtime(os.path.join(log_dir, fn)), reverse=True)
    return os.path.join(log_dir, candidates[0])

def parse_validator_status(path):
    if not path or not os.path.exists(path):
        return ("N/A", [])
    status = "UNKNOWN"
    warnings = []
    try:
        with open(path, "r", encoding="utf-8") as f:
            txt = f.read()
        if "❌ Validierung: FEHLER" in txt:
            status = "FEHLER"
        elif "✅ Validierung: OK" in txt:
            status = "OK"
        if "(mit Warnungen)" in txt:
            status = "OK+WARN"
        # Einzelne Warnzeilen extrahieren (optional)
        for line in txt.splitlines():
            if "WARN" in line or "Platzhalter '0000-01-01'" in line:
                warnings.append(line.strip())
    except Exception:
        pass
    return (status, warnings)

def main():
    ap = argparse.ArgumentParser(description="Audit ZXPress-Korpus")
    ap.add_argument("--root", required=True, help="data/zxpress/magazines")
    ap.add_argument("--logs", required=False, help="logs/validation")
    ap.add_argument("--out", required=False, help="CSV-Ausgabe")
    args = ap.parse_args()

    rows = []
    total_mags = total_issues = total_articles = 0
    mags = [os.path.join(args.root, d) for d in os.listdir(args.root) if os.path.isdir(os.path.join(args.root, d))]

    for mag_dir in sorted(mags):
        mag_name = os.path.basename(mag_dir)
        mag_json = load_json(os.path.join(mag_dir, "magazine.json")) or {}
        issues_dir = os.path.join(mag_dir, "issues")
        has_listing = os.path.isfile(os.path.join(mag_dir, "listing.json"))

        issue_folders = []
        if os.path.isdir(issues_dir):
            issue_folders = sorted([d for d in os.listdir(issues_dir) if os.path.isdir(os.path.join(issues_dir, d))])

        mag_issue_cnt = 0
        mag_article_cnt = 0
        issues_with_zero_articles = 0
        issues_missing_articles_dir = 0

        for folder in issue_folders:
            mag_issue_cnt += 1
            iid = os.path.join(issues_dir, folder)
            n = count_article_dirs(iid)
            if n is None:
                issues_missing_articles_dir += 1
            else:
                mag_article_cnt += n
                if n == 0:
                    issues_with_zero_articles += 1

        total_mags += 1
        total_issues += mag_issue_cnt
        total_articles += mag_article_cnt

        log_path = newest_log_for_mag(args.logs, mag_name) if args.logs else None
        vstatus, warns = parse_validator_status(log_path)

        rows.append({
            "magazine": mag_name,
            "magazine_id": mag_json.get("magazine_id"),
            "issues": mag_issue_cnt,
            "articles": mag_article_cnt,
            "issues_missing_articles_dir": issues_missing_articles_dir,
            "issues_zero_articles": issues_with_zero_articles,
            "has_mag_listing_json": int(has_listing),
            "validator_status": vstatus,
            "validator_log": os.path.basename(log_path) if log_path else "",
        })

    # CSV ausgeben (optional)
    if args.out:
        os.makedirs(os.path.dirname(args.out), exist_ok=True)
        with open(args.out, "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
            w.writeheader()
            w.writerows(rows)

    # Konsole zusammenfassen
    print(f"Magazine: {total_mags} | Issues: {total_issues} | Artikel: {total_articles}")
    print("\n⚠️  Kandidaten für manuelle Sichtung (Top 20 nach Problemen):")
    bad = sorted(rows, key=lambda r: (r["validator_status"]!="OK" and r["validator_status"]!="OK+WARN",
                                      r["issues_zero_articles"]+r["issues_missing_articles_dir"]), reverse=True)
    for r in bad[:20]:
        if r["validator_status"]!="OK" or r["issues_zero_articles"] or r["issues_missing_articles_dir"]:
            print(f"- {r['magazine']}: v={r['validator_status']}, zero={r['issues_zero_articles']}, missArtsDir={r['issues_missing_articles_dir']}, issues={r['issues']}, arts={r['articles']} (log: {r['validator_log']})")

if __name__ == "__main__":
    main()