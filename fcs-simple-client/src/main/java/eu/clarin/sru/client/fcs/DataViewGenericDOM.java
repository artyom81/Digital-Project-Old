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

import org.w3c.dom.Document;


/**
 * A generic Data View implementation that stores the content of the Data View
 * as a DOM document.
 */
public class DataViewGenericDOM extends DataView {
    private final Document document;


    protected DataViewGenericDOM(String type, String pid, String ref,
            Document document) {
        super(type, pid, ref);
        this.document = document;
    }


    /**
     * Get the DataView content.
     *
     * @return the DataView content as DOM document.
     */
    public Document getDocument() {
        return document;
    }

} // class GenericDataView
