# scripts/FCS/fcs_endpoint.py
import os, sys, lucene, atexit, re, yaml, traceback
from datetime import datetime, timezone
from pathlib import Path

from flask import Flask, request, Response

from java.nio.file import Paths
from org.apache.lucene.store import FSDirectory
from org.apache.lucene.index import DirectoryReader, Term
from org.apache.lucene.analysis.standard import StandardAnalyzer
from org.apache.lucene.search import IndexSearcher, BooleanQuery, BooleanClause, TermQuery, MatchAllDocsQuery
from org.apache.lucene.queryparser.classic import QueryParser, MultiFieldQueryParser
from org.apache.lucene.document import LongPoint
from org.apache.lucene.analysis.tokenattributes import CharTermAttribute
from java.io import StringReader
import os
from fcs_xml import fcs_searchretrieve_xml, fcs_explain_xml, sru_diagnostic_xml
from fcs_kwic_xml import kwic

# --- SRU Version Handling (1.2 + 2.0) --------------------------------------
SUPPORTED_SRU_VERS = {"1.2", "2.0"}

def _req_sru_version():
    """
    SRU-Version aus Query übernehmen (version oder sruVersion).
    Akzeptiert auch Formen wie VERSION_2_0, SRU_VERSION_2_0, 2, 2.0, 1.2 …
    Default: 2.0
    """
    raw = (request.args.get("version")
           or request.args.get("sruVersion")
           or request.args.get("sruversion")
           or "").strip()
    if not raw:
        return "2.0"

    up = raw.upper().strip().replace("-", "_")

    if up in ("VERSION_2_0", "SRU_VERSION_2_0", "V2", "SRU2", "2", "2.0"):
        return "2.0"
    if up in ("VERSION_1_2", "SRU_VERSION_1_2", "V1_2", "1.2", "1"):
        return "1.2"

    import re as _re
    cleaned = _re.sub(r"[^0-9.]", "", up)
    if cleaned.startswith("2"):
        return "2.0"
    if cleaned.startswith("1.2") or cleaned == "12":
        return "1.2"
    return "2.0"

def ensure_sru_20(version: str):
    """
    Enforce SRU 2.0. If a client requests anything other than 2.0,
    return a proper SRU Diagnostic (1/6: Unsupported version).
    """
    if version != "2.0":
        xml = f"""<?xml version='1.0' encoding='UTF-8'?>
<sru:diagnostics xmlns:sru="http://www.loc.gov/zing/srw/">
  <sru:diagnostic>
    <sru:uri>info:srw/diagnostic/1/6</sru:uri>
    <sru:message>Unsupported version</sru:message>
    <sru:details>{version}</sru:details>
  </sru:diagnostic>
</sru:diagnostics>"""
        resp = Response(xml, status=200, mimetype="application/sru+xml")
        resp.headers["Content-Type"] = "application/sru+xml;version=VERSION_2_0; charset=utf-8"
        resp.headers["X-Content-Type-Options"] = "nosniff"
        resp.headers["X-SRU-Version"] = "VERSION_2_0"
        return resp
    return None

# --------- Pfade ANPASSEN ---------
INDEX_DIR   = "/Users/stoia1/Desktop/Website/DigitProject/index_dir"
CONFIG_PATH = "/Users/stoia1/Desktop/Website/DigitProject/config/zxpress.yaml"
# ----------------------------------

app = Flask(__name__)
_LUCENE_READY = False

# --- FCS 2.0 Explain metadata am App-Objekt hinterlegen ---
app.explain_meta = {
    "server_info": {
        "base_url": os.environ.get("FCS_BASEURL", "https://starts-human-mph-beyond.trycloudflare.com/sru"),
        "title": "DigitProject FCS Endpoint",
        "description": "FCS 2.0 Test Endpoint (local)",
        "contact": "mailto:you@example.com",
        "version": "2.0",
    },
    "fcs_capabilities": {
        "srwVersion": "2.0",
        "fcsVersion": "2.0",
        "operations": ["explain", "searchRetrieve", "scan"],
        "maximumRecords": 50,
        "supports": {
            "queryLanguages": ["cqlfcs-2.0", "cql"],
            "dataViews": ["hits:snippet", "fcs:resource"]
        },
    },
    "indexes": [
        {"name": "cql.serverChoice", "title": "Default"},
        {"name": "dc.title", "title": "Title"},
        {"name": "text", "title": "Fulltext"},
    ],
    "resources": [
        {"pid": "corpus-1", "title": "Sample Corpus", "languages": ["de", "en"]}
    ],
}
# -----------------------------------------------------------

# ---------- YAML loader (einfach & robust) ----------
def _load_yaml(path: str | Path) -> dict:
    try:
        with open(path, "r", encoding="utf-8") as fh:
            data = yaml.safe_load(fh) or {}
            return data if isinstance(data, dict) else {}
    except Exception:
        return {}

# ---------- Lucene Init + Profil-Init ----------
def _ensure_lucene():
    global _LUCENE_READY
    if not _LUCENE_READY:
        lucene.initVM(vmargs=['-Djava.awt.headless=true'])
        _LUCENE_READY = True
    lucene.getVMEnv().attachCurrentThread()
    if not hasattr(app, "reader"):
        app.reader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR)))
        app.searcher = IndexSearcher(app.reader)
    if not hasattr(app, "profile"):
        app.profile = _load_yaml(CONFIG_PATH)

@app.route("/health", methods=["GET"])
def health():
    try:
        _ensure_lucene()
        app.searcher.search(MatchAllDocsQuery(), 1)
        return {"status": "ok"}, 200
    except Exception as e:
        return {"status": "error", "detail": str(e)}, 500

# Minimal HEAD handler for /sru
@app.route("/sru", methods=["HEAD"])
def sru_head():
    resp = Response(status=200)  # HEAD: leerer Body
    resp.headers["Content-Type"] = "application/sru+xml;version=VERSION_2_0; charset=utf-8"
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-SRU-Version"] = "VERSION_2_0"
    return resp

# ---------- Query-Helfer ----------
def _safe_parse(parser, qtext: str):
    if not qtext or not qtext.strip():
        return MatchAllDocsQuery()
    try:
        return parser.parse(qtext)
    except Exception:
        try:
            esc = QueryParser.escape(qtext)
            return parser.parse(esc)
        except Exception:
            return MatchAllDocsQuery()

def _analyze_terms(text: str, analyzer) -> list[str]:
    if not text:
        return []
    ts = analyzer.tokenStream("content", StringReader(text))
    term_attr = ts.addAttribute(CharTermAttribute.class_)
    ts.reset()
    terms = []
    while ts.incrementToken():
        terms.append(term_attr.toString())
    ts.end()
    ts.close()
    return terms

def _fallback_boolean_query(terms: list[str]) -> BooleanQuery:
    b = BooleanQuery.Builder()
    for t in terms:
        sub = BooleanQuery.Builder()
        sub.add(TermQuery(Term("content", t)), BooleanClause.Occur.SHOULD)
        sub.add(TermQuery(Term("title",   t)), BooleanClause.Occur.SHOULD)
        b.add(sub.build(), BooleanClause.Occur.MUST)
    return b.build() if terms else MatchAllDocsQuery()

def iso_to_epoch_ms(date_iso):
    if not date_iso:
        return None
    parts = date_iso.split("-")
    if len(parts) == 1:
        date_iso = f"{parts[0]}-01-01"
    elif len(parts) == 2:
        date_iso = f"{parts[0]}-{parts[1]}-01"
    dt = datetime.fromisoformat(date_iso).replace(tzinfo=timezone.utc)
    return int(dt.timestamp() * 1000)

def build_query(qtext, magazine=None, form=None, lang=None, year_from=None, year_to=None):
    analyzer = StandardAnalyzer()
    if qtext and qtext.strip():
        fields = ["content", "title"]
        parser = MultiFieldQueryParser(fields, analyzer)
        parser.setDefaultOperator(QueryParser.Operator.AND)
        q = _safe_parse(parser, qtext)
        if isinstance(q, MatchAllDocsQuery):
            terms = _analyze_terms(qtext, analyzer)
            q = _fallback_boolean_query(terms)
    else:
        q = MatchAllDocsQuery()

    b = BooleanQuery.Builder()
    b.add(q, BooleanClause.Occur.MUST)

    if magazine:
        b.add(TermQuery(Term("magazine", magazine)), BooleanClause.Occur.FILTER)

    if form:
        mapping = {"журнал": "Журнал", "газета": "Газета"}
        b.add(TermQuery(Term("form", mapping.get(form.lower(), form))), BooleanClause.Occur.FILTER)

    if lang:
        lang_norm = (lang or "").strip().lower()
        lang_map = {
            "ru": ["ru", "ru-ru", "russian", "русский"],
            "en": ["en", "en-us", "english"],
            "de": ["de", "de-de", "german", "deutsch"],
        }
        candidates = lang_map.get(lang_norm, [lang_norm])
        sub = BooleanQuery.Builder()
        for val in candidates:
            sub.add(TermQuery(Term("language", val)), BooleanClause.Occur.SHOULD)
            sub.add(TermQuery(Term("lang",     val)), BooleanClause.Occur.SHOULD)
        b.add(sub.build(), BooleanClause.Occur.FILTER)

    if year_from or year_to:
        start = iso_to_epoch_ms(f"{year_from}-01-01") if year_from else -2**63
        end   = iso_to_epoch_ms(f"{year_to}-12-31")   if year_to   else  2**63 - 1
        b.add(LongPoint.newRangeQuery("issue_date_epoch_ms", start, end), BooleanClause.Occur.FILTER)

    return b.build()

def _normalize_kwic(kwics):
    out = []
    for k in (kwics or []):
        if isinstance(k, dict):
            out.append((k.get("left", ""), k.get("match", ""), k.get("right", "")))
        elif isinstance(k, (list, tuple)) and len(k) >= 3:
            out.append((k[0], k[1], k[2]))
    return out

# ---------- SRU Endpoint ----------

# Helper: consistently add SRU version header to all SRU XML responses
def _xml_response(xml_str: str, sru_ver: str, status: int = 200) -> Response:
    # Build base response with correct MIME type first
    resp = Response(xml_str, status=status, mimetype="application/sru+xml")

    # Map the SRU version to the exact token expected by the CLARIN validator.
    # For SRU 2.0 it expects "VERSION_2_0" (not "2.0").
    ver_token = (sru_ver or "2.0").upper().replace("-", "_")
    if ver_token in ("2.0", "2", "V2", "SRU2", "SRU_VERSION_2_0"):
        ver_token = "VERSION_2_0"

    # Set headers explicitly and only once
    resp.headers["Content-Type"] = f"application/sru+xml;version={ver_token}; charset=utf-8"
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-SRU-Version"] = ver_token
    return resp

@app.route("/sru", methods=["GET"])
def sru_search():
    _ensure_lucene()
    sru_ver = "2.0"  # default for error paths
    try:
        op_raw = request.args.get("operation", "searchRetrieve")
        op = (op_raw or "").strip().lower()
        sru_ver = _req_sru_version()

        # --- CLARIN autodetector probe: answer EXPLAIN using the requested SRU version
        # if the x-fcs-endpoint-description parameter is present (value may be empty).
        x_fcs_param = request.args.get("x-fcs-endpoint-description", None)
        if x_fcs_param is not None:
            # If the CLARIN autodetector probes with this flag, we MUST return the
            # Explain record using the SAME SRU version that was requested.
            # (It often calls: operation=explain&amp;version=1.2&amp;x-fcs-endpoint-description=true)
            val = x_fcs_param.strip().lower() if isinstance(x_fcs_param, str) else ""
            if val in ("", "true", "1", "yes"):
                probe_ver = _req_sru_version() or "2.0"
                xml = fcs_explain_xml(app.explain_meta, version=probe_ver)
                return _xml_response(xml, probe_ver, 200)
        # ---------------------------------------------------------------------

        # Enforce SRU 2.0 for the rest (non-autodetect calls)
        diag_resp = ensure_sru_20(sru_ver)
        if diag_resp:
            return diag_resp

        app.logger.info("SRU %s called: args=%s -> responding version=%s", op_raw, dict(request.args), sru_ver)


        if op == "explain":
            xml = fcs_explain_xml(app.explain_meta, version=sru_ver)
            return _xml_response(xml, sru_ver, 200)

        if op != "searchretrieve":
            diag = sru_diagnostic_xml(
                code="7",
                message="Unsupported operation",
                details=f"operation={op_raw}",
                version=sru_ver
            )
            return _xml_response(diag, sru_ver, 200)

        # ---- Query parsing (SRU / CQL-FCS) ----
        raw_query = request.args.get("query") or ""
        raw_query = raw_query.strip()

        query = raw_query  # echoed string for XML
        qtext = ""

        # Accept cql.serverChoice="..."
        m = re.match(r'^cql\.serverChoice\s*=\s*"(?P<q>.*)"$', raw_query)
        if m:
            qtext = m.group("q")
        else:
            # If it's already a plain term or empty, just use as-is
            qtext = raw_query

        # Build Lucene query (falls back to MatchAll if empty)
        qry = build_query(qtext)

        # --- SRU 2.0: maximumRecords (Default 10; 0 allowed) ---
        maxre_raw = request.args.get('maximumRecords') or request.args.get('maximumrecords')
        try:
            maxre = int(maxre_raw) if maxre_raw is not None else 10
        except (TypeError, ValueError):
            maxre = 10
        if maxre < 0:
            maxre = 0

        # --- SRU: startRecord (Default 1; minimal 1) ---
        start_raw = request.args.get('startRecord') or request.args.get('startrecord')
        try:
            start = int(start_raw) if start_raw is not None else 1
        except (TypeError, ValueError):
            start = 1
        if start < 1:
            start = 1

        # Count first
        total = app.searcher.count(qry)

        # maximumRecords == 0 - return only numberOfRecords
        if maxre == 0:
            xml = fcs_searchretrieve_xml(
                records=[],
                total=total,
                start_record=start,
                maximum_records=0,
                query_str=query,
                version=sru_ver,
            )
            return _xml_response(xml, sru_ver, 200)

        # Perform search and page
        # Lucene has no offset; fetch up to start-1+maxre and then slice.
        fetch = start - 1 + maxre
        if fetch <= 0:
            fetch = maxre
        # Hard safety cap
        if fetch > 1000:
            fetch = 1000

        top = app.searcher.search(qry, fetch)
        hits = top.scoreDocs

        slice_from = max(0, start - 1)
        slice_to = min(len(hits), slice_from + maxre)
        window = hits[slice_from:slice_to]

        records = []
        for sd in window:
            # Lucene 9+ way to read stored fields
            doc = app.reader.storedFields().document(sd.doc)
            title = doc.get("title") or f"doc-{sd.doc}"
            rec = {
                "id": str(sd.doc),
                "title": title,
                "kwic": [{"left": "", "match": title, "right": ""}],
            }
            records.append(rec)

        xml = fcs_searchretrieve_xml(
            records=records,
            total=total,
            start_record=start,
            maximum_records=maxre,
            query_str=query,
            version=sru_ver,
        )
        return _xml_response(xml, sru_ver, 200)

    except Exception:
        err = traceback.format_exc()
        app.logger.error("SRU error: %s", err)
        diag = sru_diagnostic_xml(
            code="1",
            message="System error",
            details=err,
            version=sru_ver,
        )
        return _xml_response(diag, sru_ver, 200)

def _close_lucene():
    try:
        if hasattr(app, "reader"):
            app.reader.close()
    except Exception:
        pass

atexit.register(_close_lucene)

if __name__ == "__main__":
    _ensure_lucene()
    app.run(host="127.0.0.1", port=8088, debug=True, use_reloader=False, threaded=True)