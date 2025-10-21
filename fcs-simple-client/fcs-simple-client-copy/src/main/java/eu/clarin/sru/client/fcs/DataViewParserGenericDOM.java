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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.XmlStreamReaderUtils;


/**
 * An implementation of a Data View parser that stores the content of a Data
 * View in DOM representation.
 *
 * @see DataViewGenericDOM
 */
public class DataViewParserGenericDOM implements DataViewParser {

    @Override
    public boolean acceptType(String type) {
        return true;
    }


    @Override
    public int getPriority() {
        return Integer.MIN_VALUE;
    }


    @Override
    public DataView parse(XMLStreamReader reader, String type, String pid,
            String ref) throws XMLStreamException, SRUClientException {
        final Document document = XmlStreamReaderUtils.parseToDocument(reader);
        final NodeList children = document.getChildNodes();
        if ((children != null) && (children.getLength() > 0)) {
            return new DataViewGenericDOM(type, pid, ref, document);
        } else {
            throw new SRUClientException("element <DataView> does not "
                    + "contain any nested elements");
        }
    }

} // class DataViewParserGenericDOM
