# scripts/FCS/fcs_kwic_xml.py
import re

def kwic(text: str, query: str, window: int = 5, max_snips: int = 3):
    """
    Sehr einfacher KWIC: nimmt das erste Wort aus query und zeigt Links/Rechts-Kontext.
    Gibt Liste von (left, match, right) zurück.
    """
    if not text or not query:
        return []

    token = re.split(r"\s+", query.strip(), maxsplit=1)[0]
    if not token:
        return []

    rx = re.compile(r"(.{0,80})(" + re.escape(token) + r"\w*)(.{0,80})", re.IGNORECASE | re.UNICODE)
    snips = []
    for m in rx.finditer(text):
        snips.append((m.group(1), m.group(2), m.group(3)))
        if len(snips) >= max_snips:
            break
    return snips