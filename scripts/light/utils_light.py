# utils_light.py — DATUMSFIX
import re
import unicodedata
from datetime import datetime
import os, json, time
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

RU_MONTHS = {
    "января": 1, "февраля": 2, "марта": 3, "апреля": 4, "мая": 5, "июня": 6,
    "июля": 7, "августа": 8, "сентября": 9, "октября": 10, "ноября": 11, "декабря": 12,
    "январь": 1, "февраль": 2, "март": 3, "апрель": 4, "май": 5, "июнь": 6,
    "июль": 7, "август": 8, "сентябрь": 9, "октябрь": 10, "ноябрь": 11, "декабрь": 12
}

def _norm(s: str) -> str:
    return unicodedata.normalize("NFKC", (s or "")).strip()

def parse_ru_single_date(human: str) -> str:
    """
    '09 марта 2000' -> '2000-03-09'
    'март 2000'     -> '2000-03-01'
    '2000'          -> '2000-01-01'
    """
    s = _norm(human)
    # 1) Tag + Monat + Jahr
    m = re.search(r'(\d{1,2})\s+([А-Яа-яA-Za-z]+)\s+((?:18|19|20)\d{2})', s)
    if m:
        day = int(m.group(1))
        mon = RU_MONTHS.get(m.group(2).lower())
        year = int(m.group(3))
        if mon:
            return f"{year:04d}-{mon:02d}-{day:02d}"
    # 2) Monat + Jahr
    m = re.search(r'([А-Яа-яA-Za-z]+)\s+((?:18|19|20)\d{2})', s)
    if m:
        mon = RU_MONTHS.get(m.group(1).lower())
        year = int(m.group(2))
        if mon:
            return f"{year:04d}-{mon:02d}-01"
    # 3) Nur Jahr
    m = re.search(r'((?:18|19|20)\d{2})', s)
    if m:
        year = int(m.group(1))
        return f"{year:04d}-01-01"
    return "0000-01-01"

def parse_ru_year_span(human_range: str):
    """
    'ноябрь 1993 – июль 1997' -> {'start':'1993-11-01','end':'1997-07-01'}
    '1998–2000'               -> {'start':'1998-01-01','end':'2000-01-01'}
    """
    s = _norm(human_range)
    # splitter: '–' oder '-'
    parts = re.split(r'\s*[–-]\s*', s)
    if len(parts) == 2:
        start = parse_ru_single_date(parts[0])
        end   = parse_ru_single_date(parts[1])
        if start != "0000-01-01" or end != "0000-01-01":
            return {"start": start, "end": end}
    # wenn kein echter Bereich erkannt wurde, versuche ein Jahr herauszufischen
    year = re.search(r'((?:18|19|20)\d{2})', s)
    if year:
        y = int(year.group(1))
        return {"start": f"{y:04d}-01-01", "end": f"{y:04d}-12-31"}
    return {"start":"0000-01-01","end":"0000-01-01"}

# === Netzwerk/IO-Helfer ======================================================


BASE_URL = "https://zxpress.ru"
UA = "ZXPressScraperLight/1.0 (+noncommercial research; contact: you@example.org)"

def ensure_dir(p: str) -> str:
    os.makedirs(p, exist_ok=True)
    return p

def dump_json(path: str, obj) -> None:
    ensure_dir(os.path.dirname(path))
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)

def get_soup(url: str, timeout: int = 20, retries: int = 3, sleep: float = 0.3) -> BeautifulSoup | None:
    """
    Holt HTML (UTF-8) und gibt BeautifulSoup zurück. Gibt None zurück, wenn alle Versuche scheitern.
    """
    headers = {"User-Agent": UA, "Accept": "text/html,*/*;q=0.8"}
    last_err = None
    for _ in range(retries):
        try:
            r = requests.get(url, headers=headers, timeout=timeout)
            r.encoding = "utf-8"
            return BeautifulSoup(r.text, "html.parser")
        except Exception as e:
            last_err = e
            time.sleep(sleep)
    print(f"⚠️ get_soup fehlgeschlagen: {url} ({last_err})")
    return None

def abs_url(href: str) -> str:
    """
    Macht aus relativen ZXPress-Links absolute URLs.
    """
    if not href:
        return BASE_URL + "/"
    return urljoin(BASE_URL + "/", href.lstrip("/"))