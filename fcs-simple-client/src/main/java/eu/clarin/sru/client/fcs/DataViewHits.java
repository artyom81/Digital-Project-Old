/**
 * This software is copyright (c) 2012-2022 by
 *  - Leibniz-Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 * This is free software. You can redistribute it
 * and/or modify it under the terms described in
 * the GNU General Public License v3 of which you
 * should have received a copy. Otherwise you can download
 * it from
 *
 *   http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * @copyright Leibniz-Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package eu.clarin.sru.client.fcs;

/**
 * A Data View implementation that stores the content of a HITS Data View.
 */
public class DataViewHits extends DataView {
    /**
     * The MIME type for CLARIN-FCS KWIC data views.
     */
    public static final String TYPE = "application/x-clarin-fcs-hits+xml";
    private final String text;
    private final int[] offsets;
    protected final int max_offset;


    /**
     * Constructor.
     *
     * @param pid
     *            a persistent identifier or <code>null</code>
     * @param ref
     *            a reference URI or <code>null</code>
     * @param text
     *            the textual content of the hits
     * @param offsets
     *            an array of (start, end) offset pairs that indicate the part
     *            of the text, that is considered a hit
     * @param offsets_idx
     *            the largest index (= hit_count * 2) within the offsets array
     *            plus one
     * @throws NullPointerException
     *             if any mandatory argument is <code>null</code>
     * @throws IllegalArgumentException
     *             if any argument is illegal
     */
    protected DataViewHits(String pid, String ref, String text,
            int[] offsets, int offsets_idx) {
        super(TYPE, pid, ref);
        if (text == null) {
            throw new NullPointerException("text == null");
        }
        if (text.isEmpty()) {
            throw new IllegalArgumentException("text is empty");
        }
        this.text = text;
        if (offsets == null) {
            throw new NullPointerException("offsets == null");
        }
        this.offsets = offsets;
        if (offsets_idx < 0) {
            throw new IllegalArgumentException("offsets_idx < 0");
        }
        if (offsets_idx > offsets.length) {
            throw new IllegalArgumentException("offsets_idx > offsets.length");
        }
        this.max_offset = (offsets_idx / 2);
    }


    /**
     * Get the total number of hits in the result.
     * @return the number of hits
     */
    public int getHitCount() {
        return max_offset;
    }


    /**
     * Get the text content of the hit. Usually this is complete sentence.
     *
     * @return the text content of the hit
     */
    public String getText() {
        return text;
    }


    /**
     * Get the offsets pointing to range in the text content that yield the hit.
     *
     * @param idx
     *            the hit to retrieve. Must be larger than <code>0</code> and
     *            smaller than the result of {@link #getHitCount()}.
     * @return An array of two elements. The first array element is the start
     *         offset, the second array element is the end offset of the hit
     *         range.
     * @throws ArrayIndexOutOfBoundsException
     *             of the <code>idx</code> argument is out of bounds.
     */
    public int[] getHitOffsets(int idx) {
        if (idx < 0) {
            throw new ArrayIndexOutOfBoundsException("idx < 0");
        }
        if (idx < max_offset) {
            int[] result = new int[2];
            result[0] = offsets[(2 *idx)];
            result[1] = offsets[(2 * idx) + 1];
            return result;
        } else {
            throw new ArrayIndexOutOfBoundsException("idx > " +
                    (max_offset - 1));
        }
    }

} // class DataViewHits
