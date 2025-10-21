package eu.clarin.sru.client.fcs;

/**
 * A Data View implementation that stores the content of a (Lex)HITS Data View.
 * 
 * The HITS Data View with optional <code>kind</code> attributes.
 * 
 * @see DataViewHits
 */
public class DataViewHitsWithLexAnnotations extends DataViewHits {

    public static final String TYPE = DataViewHits.TYPE;

    private final String[] hitKinds;

    protected DataViewHitsWithLexAnnotations(String pid, String ref, String text, int[] offsets, int offsets_idx,
            String[] hitKinds) {
        super(pid, ref, text, offsets, offsets_idx);

        if (hitKinds == null) {
            throw new NullPointerException("hitKinds == null");
        }
        if ((offsets_idx / 2) > hitKinds.length) {
            throw new IllegalArgumentException("(offsets_idx / 2) > hitKinds.length");
        }
        this.hitKinds = hitKinds;
    }

    public String getHitKind(int idx) {
        if (idx < 0) {
            throw new ArrayIndexOutOfBoundsException("idx < 0");
        }
        if (idx < max_offset) {
            return hitKinds[idx];
        } else {
            throw new ArrayIndexOutOfBoundsException("idx > " + (max_offset - 1));
        }
    }
}
