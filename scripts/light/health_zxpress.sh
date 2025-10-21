#!/usr/bin/env bash
# health_zxpress.sh — Struktur-/Vollständigkeitscheck für ZXPress-Korpus
# Nutzung: ./health_zxpress.sh [ROOT] [LOGDIR]
#   ROOT   : Basisordner der Magazine (Default: data/zxpress/magazines)
#   LOGDIR : Ziel für Logs (Default: logs/health)

set -euo pipefail

ROOT="${1:-data/zxpress/magazines}"
LOGDIR="${2:-logs/health}"
mkdir -p "$LOGDIR"

ISSUES_LOG="$LOGDIR/issues_missing.log"
ARTS_LOG="$LOGDIR/articles_missing.log"
PLACEHOLDER_LOG="$LOGDIR/placeholders_0000-01-01.log"
EMPTY_MAGS_LOG="$LOGDIR/magazines_empty.log"
SUMMARY_TXT="$LOGDIR/summary.txt"

# Logs zurücksetzen
: >"$ISSUES_LOG"
: >"$ARTS_LOG"
: >"$PLACEHOLDER_LOG"
: >"$EMPTY_MAGS_LOG"
: >"$SUMMARY_TXT"

echo "▶ Root: $ROOT"
if [[ ! -d "$ROOT" ]]; then
  echo "❌ ROOT nicht gefunden: $ROOT" >&2
  exit 2
fi

# Zählen Magazine (Ebene 1)
mag_count=$(find "$ROOT" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')
# Zählen Issues (Ordner unter .../issues/*)
issue_count=$(find "$ROOT" -type d -path '*/issues/*' -mindepth 1 -maxdepth 1 | wc -l | tr -d ' ')
# Zählen Artikel (Ordner unter .../issues/*/articles/*)
article_count=$(find "$ROOT" -type d -path '*/issues/*/articles/*' -mindepth 1 -maxdepth 1 | wc -l | tr -d ' ')

echo "Magazines: $mag_count" | tee -a "$SUMMARY_TXT"
echo "Issues   : $issue_count" | tee -a "$SUMMARY_TXT"
echo "Articles : $article_count" | tee -a "$SUMMARY_TXT"
echo | tee -a "$SUMMARY_TXT"

# 1) Struktur-Checks pro Issue
echo "🔍 Prüfe Issues auf fehlende Dateien/Ordner ..."
find "$ROOT" -type d -path '*/issues/*' -mindepth 1 -maxdepth 1 -print0 \
| while IFS= read -r -d '' issue; do
  has_issue_json=1
  has_listing_json=1
  has_articles_dir=1

  [[ -f "$issue/issue.json" ]]   || { echo "❌ fehlt issue.json   : $issue"   >> "$ISSUES_LOG"; has_issue_json=0; }
  [[ -f "$issue/listing.json" ]] || { echo "❌ fehlt listing.json : $issue"   >> "$ISSUES_LOG"; has_listing_json=0; }
  [[ -d "$issue/articles"    ]]  || { echo "❌ fehlt articles/    : $issue"   >> "$ISSUES_LOG"; has_articles_dir=0; }

  # Platzhalter-Datum zählen (nur wenn issue.json existiert)
  if [[ $has_issue_json -eq 1 ]]; then
    if grep -q '"issue_date_iso":[[:space:]]*"0000-01-01"' "$issue/issue.json"; then
      echo "⚠️  Platzhalter-Datum (0000-01-01): $issue" >> "$PLACEHOLDER_LOG"
    fi
  fi
done

# 2) Struktur-Checks pro Artikel
echo "🔍 Prüfe Artikel auf fehlende meta.json/text.txt ..."
find "$ROOT" -type d -path '*/issues/*/articles/*' -mindepth 1 -maxdepth 1 -print0 \
| while IFS= read -r -d '' art; do
  [[ -f "$art/meta.json" ]] || echo "❌ fehlt meta.json : $art" >> "$ARTS_LOG"
  [[ -f "$art/text.txt"  ]] || echo "❌ fehlt text.txt  : $art" >> "$ARTS_LOG"
done

# 3) Leere Magazine (issues-Verzeichnis existiert, aber keine Issue-Ordner)
echo "🔍 Suche leere Magazine ..."
find "$ROOT" -mindepth 1 -maxdepth 1 -type d -print0 \
| while IFS= read -r -d '' mag; do
  issues_dir="$mag/issues"
  if [[ -d "$issues_dir" ]]; then
    cnt=$(find "$issues_dir" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')
    if [[ "$cnt" = "0" ]]; then
      echo "0 Issues: $mag" >> "$EMPTY_MAGS_LOG"
    fi
  else
    echo "kein issues/: $mag" >> "$EMPTY_MAGS_LOG"
  fi
done

# Kurze Zusammenfassung bauen
issues_missing_count=$(wc -l < "$ISSUES_LOG" | tr -d ' ')
arts_missing_count=$(wc -l < "$ARTS_LOG" | tr -d ' ')
placeholders_count=$(wc -l < "$PLACEHOLDER_LOG" | tr -d ' ')
empty_mags_count=$(wc -l < "$EMPTY_MAGS_LOG" | tr -d ' ')

echo "=== SUMMARY ================================="        | tee -a "$SUMMARY_TXT"
echo "❗ Issues mit fehlendem issue.json/listing.json/articles/: $issues_missing_count" | tee -a "$SUMMARY_TXT"
echo "❗ Artikel mit fehlender meta.json/text.txt:           $arts_missing_count"       | tee -a "$SUMMARY_TXT"
echo "⚠️  Issues mit Platzhalter-Datum (0000-01-01):         $placeholders_count"       | tee -a "$SUMMARY_TXT"
echo "ℹ️  Leere/inkomplette Magazine:                        $empty_mags_count"         | tee -a "$SUMMARY_TXT"
echo "Logs unter: $LOGDIR"                                   | tee -a "$SUMMARY_TXT"

# Exit-Code: 0 wenn strukturell sauber (keine fehlenden Dateien/Ordner), sonst 1
if [[ "$issues_missing_count" -eq 0 && "$arts_missing_count" -eq 0 ]]; then
  echo "✅ Struktur OK."
  exit 0
else
  echo "❌ Struktur hat Lücken. Details in $LOGDIR/."
  exit 1
fi