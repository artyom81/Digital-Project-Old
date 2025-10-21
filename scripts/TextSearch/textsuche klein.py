import lucene
from java.nio.file import Paths
from org.apache.lucene.store import FSDirectory
from org.apache.lucene.index import DirectoryReader
from org.apache.lucene.search import IndexSearcher, Sort, SortField
from org.apache.lucene.queryparser.classic import QueryParser
from org.apache.lucene.analysis.standard import StandardAnalyzer

def kwic(text, term, window=3):
    words = text.split()
    res = []
    term_l = term.lower()
    for i,w in enumerate(words):
        if term_l in w.lower():
            s = max(0,i-window); e = min(len(words),i+window+1)
            res.append(" ".join(words[s:e]))
    return res[:3]

lucene.initVM()
dir = FSDirectory.open(Paths.get("/Users/stoia1/Desktop/Website/DigitProject/index_dir"))
r = DirectoryReader.open(dir)
s = IndexSearcher(r)
an = StandardAnalyzer()

q = QueryParser("content", an).parse("Covox OR Ковокс")
hits = s.search(q, 5, Sort(SortField("issue_date_epoch", SortField.Type.LONG, False))).scoreDocs
print("Treffer:", len(hits))
for h in hits:
    d = r.storedFields().document(h.doc)
    print("—", d.get("magazine"), d.get("issue_label"), d.get("issue_date_iso"), d.get("title"))
    for sn in kwic(d.get("content")[:2000], "Covox"):
        print("   ...", sn, "...")
r.close()