import lucene, random
from java.nio.file import Paths
from org.apache.lucene.store import FSDirectory
from org.apache.lucene.index import DirectoryReader

lucene.initVM()
r = DirectoryReader.open(FSDirectory.open(Paths.get("/Users/stoia1/Desktop/Website/DigitProject/index_dir")))
print("Docs (sichtbar):", r.numDocs())

# Stichprobe
ids = random.sample(range(r.maxDoc()), 5)
for doc_id in ids:
    d = r.storedFields().document(doc_id)
    missing = [f for f in ("content","magazine","issue_label","issue_date_iso") if d.get(f) is None]
    print(doc_id, "OK" if not missing else f"FEHLT: {missing}")
r.close()