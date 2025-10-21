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
 * A Data View implementation that stores the content of a KWIC Data View.
 *
 * @deprecated This class should not be used anymore. Use DataViewParserKWIC in
 *             non legacy mode instead.
 */
@Deprecated
public final class LegacyDataViewKWIC extends DataView {
    /**
     * The MIME type for CLARIN-FCS KWIC data views.
     */
    public static final String TYPE = "application/x-clarin-fcs-kwic+xml";
    private final String left;
    private final String keyword;
    private final String right;


    /**
     * Constructor.
     *
     * @param pid
     *            a persistent identifier or <code>null</code>
     * @param ref
     *            a reference URI or <code>null</code>
     * @param left
     *            the left KWIC context
     * @param keyword
     *            the matched KWIC context
     * @param right
     *            the right KWIC context
     */
    LegacyDataViewKWIC(String pid, String ref, String left, String keyword,
            String right) {
        super(TYPE, pid, ref);
        this.left = (left != null) ? left : "";
        this.keyword = (keyword != null) ? keyword : "";
        this.right = (right != null) ? right : "";
    }


    /**
     * Get the left KWIC context.
     *
     * @return the left KWIC context
     */
    public String getLeft() {
        return left;
    }


    /**
     * Get the matched KWIC context.
     *
     * @return the matched KWIC context
     */
    public String getKeyword() {
        return keyword;
    }


    /**
     * Get the right KWIC context.
     *
     * @return the right KWIC context
     */
    public String getRight() {
        return right;
    }

} // class DataViewKWIC
