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
 * A generic Data View implementation that stores the content of a Data View as
 * a String.
 */
public class DataViewGenericString extends DataView {
    private final String content;


    protected DataViewGenericString(String type, String pid, String ref,
            String content) {
        super(type, pid, ref);
        this.content = content;
    }


    /**
     * Get DataView content.
     *
     * @return the DataView content as String.
     */
    public String getContent() {
        return content;
    }

} // class DataViewGenericString
