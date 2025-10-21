# scripts/TextSearch/feldabdeckung_all.py
import lucene
from java.nio.file import Paths
from org.apache.lucene.store import FSDirectory
from org.apache.lucene.index import DirectoryReader

INDEX_DIR = "/Users/stoia1/Desktop/Website/DigitProject/index_dir"

def main():
    lucene.initVM()
    r = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR)))
    N = r.numDocs()
    fields = ["form","language","city","country","issue_date_iso","issue_date_epoch_ms","title","article_url","print_url","magazine","magazine_id_s","issue_label"]
    counts = {f:0 for f in fields}
    missing_examples = {f:[] for f in fields}

    for doc_id in range(r.maxDoc()):
        d = r.storedFields().document(doc_id)
        if d is None:  # defensiv
            continue
        for f in fields:
            if d.get(f) is not None:
                counts[f] += 1
            elif len(missing_examples[f]) < 5:
                # kleine Beispiele einfangen
                missing_examples[f].append(d.get("title") or d.get("filename") or str(doc_id))

    print("Docs (sichtbar):", N)
    for f in fields:
        print(f"{f:20s} {counts[f]}/{N}")

    print("\nBeispiele fehlender Felder (max 5 je Feld):")
    for f, ex in missing_examples.items():
        if ex:
            print(f"  {f}: {ex}")

    r.close()

if __name__ == "__main__":
    main()