# Sehr einfacher SRU/FCS 1.2 "searchRetrieve" Endpoint (nur searchRetrieve)
# Minimallösung für lokale Tests/Validator. "explain"/"scan" später ergänzen.

import os, sys, lucene
import atexit
from datetime import datetime, timezone
from flask import Flask, request, Response
from java.nio.file import Paths
from org.apache.lucene.store import FSDirectory
from org.apache.lucene.index import DirectoryReader
from org.apache.lucene.analysis.standard import StandardAnalyzer
from org.apache.lucene.search import IndexSearcher, BooleanQuery, BooleanClause, TermQuery, MatchAllDocsQuery
from org.apache.lucene.index import Term
from org.apache.lucene.queryparser.classic import QueryParser, MultiFieldQueryParser
from org.apache.lucene.document import LongPoint

# Falls Modul-Suche nötig ist (hier normalerweise nicht):
THIS_DIR = os.path.dirname(__file__)
PARENT   = os.path.abspath(os.path.join(THIS_DIR, ".."))
if PARENT not in sys.path:
    sys.path.insert(0, PARENT)

# Renderer & KWIC
from fcs_kwic_xml import fcs_searchretrieve_xml, kwic

INDEX_DIR = "/Users/stoia1/Desktop/Website/DigitProject/index_dir"
app = Flask(__name__)

# --- Lazy Init (kompatibel mit Flask >= 3.0) -------------------------------
_LUCENE_READY = False
def _ensure_lucene():
    """Startet die JVM einmalig und attached den aktuellen Thread (erforderlich
    in jedem Flask-Request-Thread), initialisiert bei Bedarf Reader/Searcher."""
    global _LUCENE_READY
    if not _LUCENE_READY:
        lucene.initVM(vmargs=['-Djava.awt.headless=true'])
        _LUCENE_READY = True
    lucene.getVMEnv().attachCurrentThread()
    if not hasattr(app, "reader"):
        app.reader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR)))
        app.searcher = IndexSearcher(app.reader)

@app.route("/health", methods=["GET"])
def health():
    try:
        _ensure_lucene()
        # einfacher Probe-Call
        app.searcher.search(MatchAllDocsQuery(), 1)
        return {"status": "ok"}, 200
    except Exception as e:
        return {"status": "error", "detail": str(e)}, 500

def _safe_parse(parser, qtext: str):
    if not qtext or not qtext.strip():
        return MatchAllDocsQuery()
    try:
        return parser.parse(qtext)
    except Exception:
        # Fallback: escape special chars and parse again
        try:
            esc = QueryParser.escape(qtext)
            return parser.parse(esc)
        except Exception:
            return MatchAllDocsQuery()

# --- Hilfsfunktionen --------------------------------------------------------
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

def build_query(qtext, magazine=None, form=None, lang="ru", year_from=None, year_to=None):
    analyzer = StandardAnalyzer()
    # Volltext: content ODER title (MultiField + Boost)
    if qtext and qtext.strip():
        fields = ["content", "title"]
        boosts = {"content": 1.0, "title": 2.0}  # Titel etwas höher gewichten
        mparser = MultiFieldQueryParser(fields, analyzer, boosts)
        q = _safe_parse(mparser, qtext)
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
        b.add(TermQuery(Term("language", lang)), BooleanClause.Occur.FILTER)

    if year_from or year_to:
        start = iso_to_epoch_ms(f"{year_from}-01-01") if year_from else -2**63
        end   = iso_to_epoch_ms(f"{year_to}-12-31")   if year_to   else  2**63 - 1
        b.add(LongPoint.newRangeQuery("issue_date_epoch_ms", start, end), BooleanClause.Occur.FILTER)

    return b.build()

# --- SRU Route --------------------------------------------------------------
@app.route("/sru", methods=["GET"])
def sru_search():
    _ensure_lucene()
    op = request.args.get("operation", "searchRetrieve")
    if op != "searchRetrieve":
        return Response("Only searchRetrieve supported in this minimal endpoint", status=400)

    query = request.args.get("query", "")

    # startRecord parsing + cap
    try:
        start = int(request.args.get("startRecord", "1"))
    except ValueError:
        start = 1
    if start < 1:
        start = 1

    # maximumRecords parsing + soft cap
    try:
        maxre = int(request.args.get("maximumRecords", "10"))
    except ValueError:
        maxre = 10
    if maxre < 0:
        maxre = 0
    maxre = min(maxre, 50)  # Soft-Limit

    # optionale Filter
    magazine = request.args.get("magazine")
    form     = request.args.get("form")
    lang     = request.args.get("lang", "ru")
    yfrom    = request.args.get("yearFrom")
    yto      = request.args.get("yearTo")
    yfrom    = int(yfrom) if yfrom else None
    yto      = int(yto)   if yto   else None

    qry = build_query(query, magazine, form, lang, yfrom, yto)

    # Defensive: Wenn maxre == 0, NICHT search(numHits=0) aufrufen.
    if maxre == 0:
        total = app.searcher.count(qry)  # nur zählen, keine Treffer ziehen
        xml = fcs_searchretrieve_xml(
            records=[],
            total=total,
            start_record=start,
            maximum_records=0,
            query_str=query
        )
        return Response(xml, status=200, mimetype="application/xml; charset=utf-8")

    # Normale Suche + Paginierung
    top   = app.searcher.search(qry, start - 1 + maxre)
    sdocs = top.scoreDocs[start - 1 : start - 1 + maxre]

    th = top.totalHits
    try:
        total = th.value() if callable(getattr(th, "value", None)) else th.value
    except Exception:
        total = len(top.scoreDocs)

    # >>> hier den Fix oder die Query-Erweiterung einsetzen <<<

    records = []
    for sd in sdocs:
        d = app.reader.storedFields().document(sd.doc)
        title   = d.get("title") or d.get("filename") or ""
        mag     = d.get("magazine") or ""
        label   = d.get("issue_label") or ""
        dateiso = d.get("issue_date_iso") or ""
        url     = d.get("article_url") or ""
        txt     = d.get("content") or ""
        kwics   = kwic(txt, query, window=5, max_snips=3)
        records.append({
            "id": url or f"urn:zxpress:doc:{sd.doc}",
            "title": title,
            "magazine": mag,
            "issue_label": label,
            "issue_date_iso": dateiso,
            "url": url,
            "kwic_list": kwics
        })

    xml = fcs_searchretrieve_xml(
        records=records,
        total=total,
        start_record=start,
        maximum_records=maxre,
        query_str=query
    )
    return Response(xml, status=200, mimetype="application/xml; charset=utf-8")

def _close_lucene():
    try:
        if hasattr(app, "reader"):
            app.reader.close()
    except Exception:
        pass

atexit.register(_close_lucene)

# --- Start ------------------------------------------------------------------
if __name__ == "__main__":
    _ensure_lucene()  # optionales Vorwärmen
    app.run(host="127.0.0.1", port=8088, debug=True, use_reloader=False, threaded=True)