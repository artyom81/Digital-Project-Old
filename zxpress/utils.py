# zxpress/utils.py
import os
import re
import yaml
import time
import unicodedata
from typing import Optional
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup


# --- HTTP / Parsing ---------------------------------------------------------

DEFAULT_HEADERS = {
    "User-Agent": "ZXPressScraper/1.0 (+research; contact: your@email)",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
}

def get_soup(url: str, timeout: int = 20, retries: int = 3, sleep_between: float = 0.5) -> Optional[BeautifulSoup]:
    """
    Holt eine URL und gibt BeautifulSoup zurück (UTF-8 gesetzt).
    Mit einfachen Retries, damit der Scraper robuster ist.
    """
    last_exc = None
    for _ in range(retries):
        try:
            r = requests.get(url, headers=DEFAULT_HEADERS, timeout=timeout)
            r.encoding = "utf-8"
            return BeautifulSoup(r.text, "html.parser")
        except Exception as e:
            last_exc = e
            time.sleep(sleep_between)
    print(f"⚠️ get_soup fehlgeschlagen für {url}: {last_exc}")
    return None


def absolute_url(base_url: str, href: str) -> str:
    """
    Macht aus relativen Hrefs absolute URLs.
    """
    if not href:
        return base_url
    return urljoin(base_url.rstrip("/") + "/", href.lstrip("/"))


# --- Filesystem -------------------------------------------------------------

def ensure_dir(path: str) -> str:
    """
    Stellt sicher, dass ein Verzeichnis existiert. Gibt den Pfad zurück.
    """
    os.makedirs(path, exist_ok=True)
    return path


# --- Namen/Slugs ------------------------------------------------------------

def safe_filename(name: str, max_len: int = 80) -> str:
    """
    Macht aus beliebigen Titeln sichere Dateinamen (Unicode-tauglich).
    """
    s = unicodedata.normalize("NFKC", name).strip()
    s = re.sub(r"[^\w\-]+", "_", s, flags=re.UNICODE)
    s = re.sub(r"_+", "_", s).strip("_")
    if not s:
        s = "untitled"
    if len(s) > max_len:
        s = s[:max_len].rstrip("_")
    return s

# --- Low-level HTML ---------------------------------------------------------

def fetch_html(url: str, timeout: int = 20, headers: dict = None) -> str:
    """
    Holt den Roh-HTML-Text (UTF-8). Wir nutzen sie dort, wo nur Text gebraucht wird.
    """
    hdrs = DEFAULT_HEADERS.copy()
    if headers:
        hdrs.update(headers)
    r = requests.get(url, headers=hdrs, timeout=timeout)
    r.encoding = "utf-8"
    r.raise_for_status()
    return r.text


# --- Pfade ------------------------------------------------------------------

def abspath(*parts: str) -> str:
    """
    Baut und normalisiert einen absoluten Pfad aus beliebigen Teilstücken.
    """
    return os.path.abspath(os.path.join(*parts))


# --- Datumsparser (Russisch) -----------------------------------------------

# Russisch (meist Genitiv), plus einfache Nominativ-Varianten
_RU_MONTHS = {
    # genitiv
    "января": 1, "февраля": 2, "марта": 3, "апреля": 4, "мая": 5, "июня": 6,
    "июля": 7, "августа": 8, "сентября": 9, "октября": 10, "ноября": 11, "декабря": 12,
    # nominativ (für Bereiche wie "май 1998 – март 2000")
    "январь": 1, "февраль": 2, "март": 3, "апрель": 4, "май": 5, "июнь": 6,
    "июль": 7, "август": 8, "сентябрь": 9, "октябрь": 10, "ноябрь": 11, "декабрь": 12,
}

def parse_ru_date(s: str) -> str:
    """
    Parsed typische Datumsangaben der Seite zu ISO-String.
    Beispiele:
      "09 марта 2000"      -> "2000-03-09"
      "21 ноября 1998"     -> "1998-11-21"
      "май 1998"           -> "1998-05-01"
      "май 1998 – март 2000" (Bereich) -> gibt Start "1998-05-01" zurück
    Rückgabe: ISO "YYYY-MM-DD" (bei Monatsangabe: Tag = 01).
    """
    if not s:
        return ""

    t = unicodedata.normalize("NFKC", s).strip().lower()
    # Datumsbereich? -> nimm die erste Komponente als Startdatum
    if "–" in t:
        t = t.split("–", 1)[0].strip()
    if "-" in t and " – " not in s:
        # manchmal normaler Bindestrich
        parts = [p.strip() for p in t.split("-")]
        if len(parts) == 3 and parts[0].isdigit() and parts[2].isdigit():
            #  dd-mm-yyyy  (selten)
            dd, mm, yyyy = parts
            return f"{int(yyyy):04d}-{int(mm):02d}-{int(dd):02d}"

    # Formate:
    # 1) "DD <monat> YYYY"
    tokens = t.replace(",", " ").split()
    try:
        if len(tokens) >= 3 and tokens[0].isdigit() and tokens[2].isdigit():
            day = int(tokens[0])
            mon = _RU_MONTHS.get(tokens[1], None)
            year = int(tokens[2])
            if mon:
                return f"{year:04d}-{mon:02d}-{day:02d}"
        # 2) "<monat> YYYY"
        if len(tokens) >= 2 and tokens[1].isdigit():
            mon = _RU_MONTHS.get(tokens[0], None)
            year = int(tokens[1])
            if mon:
                return f"{year:04d}-{mon:02d}-01"
        # 3) Nur Jahr
        if len(tokens) == 1 and tokens[0].isdigit():
            year = int(tokens[0])
            return f"{year:04d}-01-01"
    except Exception:
        pass
    # Fallback: leer lassen statt falsches Datum zu erfinden
    return ""



def load_yaml(path: str):
    """
    Lädt eine YAML-Datei und gibt sie als dict zurück.
    """
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)

# --- HTTP Session mit Retries ------------------------------------------------
import json
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

BASE_URL = "https://zxpress.ru"

def make_session(cfg: dict | None = None) -> requests.Session:
    """
    Erstellt eine Requests-Session mit User-Agent und Retries.
    """
    sess = requests.Session()
    headers = DEFAULT_HEADERS.copy()
    if cfg and isinstance(cfg, dict):
        ua = (cfg.get("http") or {}).get("user_agent")
        if ua:
            headers["User-Agent"] = ua
    sess.headers.update(headers)

    retry = Retry(
        total=3,
        backoff_factor=0.3,
        status_forcelist=(429, 500, 502, 503, 504),
        allowed_methods=["HEAD", "GET", "OPTIONS"]
    )
    adapter = HTTPAdapter(max_retries=retry)
    sess.mount("http://", adapter)
    sess.mount("https://", adapter)
    return sess

def get_soup_session(url: str, session: requests.Session, timeout: int = 20) -> Optional[BeautifulSoup]:
    try:
        r = session.get(url, timeout=timeout)
        r.encoding = "utf-8"
        return BeautifulSoup(r.text, "html.parser")
    except Exception as e:
        print(f"⚠️ get_soup_session fehlgeschlagen für {url}: {e}")
        return None

# --- JSON Helper -------------------------------------------------------------
def dump_json(path: str, data) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

# --- Russisches Datum -> (human, iso) ---------------------------------------
_RU_MONTHS = {
    "января": 1, "февраля": 2, "марта": 3, "апреля": 4, "мая": 5, "июня": 6,
    "июля": 7, "августа": 8, "сентября": 9, "октября": 10, "ноября": 11, "декабря": 12,
    "январь": 1, "февраль": 2, "март": 3, "апрель": 4, "май": 5, "июнь": 6,
    "июль": 7, "август": 8, "сентябрь": 9, "октябрь": 10, "ноябрь": 11, "декабрь": 12,
}

def parse_ru_date(s: str) -> tuple[str | None, str | None]:
    """
    Liefert (human, iso). 'human' ist der Original-Text getrimmt,
    'iso' ist YYYY-MM-DD (bei Monatsangabe Tag=01). Bei Range 'май 1998 – март 2000'
    wird die linke Seite verwendet.
    """
    if not s:
        return None, None
    human = unicodedata.normalize("NFKC", s).strip()
    t = human.lower()

    if "–" in t:
        t = t.split("–", 1)[0].strip()

    tokens = t.replace(",", " ").split()
    try:
        if len(tokens) >= 3 and tokens[0].isdigit() and tokens[2].isdigit():
            day = int(tokens[0])
            mon = _RU_MONTHS.get(tokens[1], None)
            year = int(tokens[2])
            if mon:
                return human, f"{year:04d}-{mon:02d}-{day:02d}"
        if len(tokens) >= 2 and tokens[1].isdigit():
            mon = _RU_MONTHS.get(tokens[0], None)
            year = int(tokens[1])
            if mon:
                return human, f"{year:04d}-{mon:02d}-01"
        if len(tokens) == 1 and tokens[0].isdigit():
            year = int(tokens[0])
            return human, f"{year:04d}-01-01"
    except Exception:
        pass
    return human, None