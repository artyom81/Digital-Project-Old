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

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import eu.clarin.sru.client.SRUClientException;


/**
 * An implementation of a DataView parser that stores the content of a Data
 * Views in String representation.
 *
 * @see DataViewGenericString
 */
public class DataViewParserGenericString implements DataViewParser {
    private static final XMLOutputFactory factory =
            XMLOutputFactory.newInstance();

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
        XMLStreamWriter to = null;
        try {
            final StringWriter writer = new StringWriter();
            to = factory.createXMLStreamWriter(writer);
            copy(reader, to);
            to.close();
            return new DataViewGenericString(type, pid, ref, writer.toString());
        } finally {
            if (to != null) {
                try {
                    to.close();
                } catch (XMLStreamException e) {
                    /* IGNORE */
                }
            }
        }
    }


    public static void copy(XMLStreamReader from, XMLStreamWriter to)
            throws XMLStreamException {
        int depth = 0;
        while (from.hasNext()) {
            depth = copyEvent(from, to, depth);
            from.next();
            if (depth <= 0) {
                break;
            }
        } // while
    }


    private static int copyEvent(XMLStreamReader from, XMLStreamWriter to,
            int depth) throws XMLStreamException {
        switch (from.getEventType()) {
        case XMLStreamConstants.START_DOCUMENT:
            {
                final String version = from.getVersion();
                if ((version == null) || version.isEmpty()) {
                    to.writeStartDocument();
                } else {
                    to.writeStartDocument(from.getCharacterEncodingScheme(),
                            from.getVersion());
                }
                to.writeCharacters("\n");
            }
            return depth + 1;

        case XMLStreamConstants.END_DOCUMENT:
            to.writeCharacters("\n");
            to.writeEndDocument();
            return depth - 1;

        case XMLStreamConstants.START_ELEMENT:
            copyStartElement(from, to);
            return depth + 1;

        case XMLStreamConstants.END_ELEMENT:
            to.writeEndElement();
            return depth - 1;

        case XMLStreamConstants.SPACE:
            to.writeCharacters(from.getTextCharacters(), from.getTextStart(),
                    from.getTextLength());
            return depth;

        case XMLStreamConstants.CDATA:
            to.writeCData(from.getText());
            return depth;

        case XMLStreamConstants.CHARACTERS:
            to.writeCharacters(from.getTextCharacters(), from.getTextStart(),
                    from.getTextLength());
            return depth;

        case XMLStreamConstants.COMMENT:
            to.writeComment(from.getText());
            return depth;

        case XMLStreamConstants.PROCESSING_INSTRUCTION:
            to.writeProcessingInstruction(from.getPITarget(), from.getPIData());
            return depth;

        case XMLStreamConstants.DTD:
        case XMLStreamConstants.ENTITY_REFERENCE:
        case XMLStreamConstants.ATTRIBUTE:
        case XMLStreamConstants.NAMESPACE:
        case XMLStreamConstants.ENTITY_DECLARATION:
        case XMLStreamConstants.NOTATION_DECLARATION:
            /* FALL_TROUGH */
        } // switch
        throw new XMLStreamException("unsupported event type: " +
                from.getEventType());
    }

    private static void copyStartElement(XMLStreamReader from,
            XMLStreamWriter to) throws XMLStreamException {
        final int nsCount = from.getNamespaceCount();
        if (nsCount > 0) { // yup, got some...
            for (int i = 0; i < nsCount; ++i) {
                final String prefix = from.getNamespacePrefix(i);
                final String uri = from.getNamespaceURI(i);
                if ((prefix == null) || prefix.isEmpty()) { // default NS
                    to.setDefaultNamespace(uri);
                } else {
                    to.setPrefix(prefix, uri);
                }
            }
        }
        to.writeStartElement(from.getPrefix(), from.getLocalName(),
                from.getNamespaceURI());

        if (nsCount > 0) {
            // write namespace declarations
            for (int i = 0; i < nsCount; ++i) {
                final String prefix = from.getNamespacePrefix(i);
                final String uri = from.getNamespaceURI(i);

                if ((prefix == null) || prefix.isEmpty()) { // default NS
                    to.writeDefaultNamespace(uri);
                } else {
                    to.writeNamespace(prefix, uri);
                }
            }
        }
        final int attrCount = from.getAttributeCount();
        if (attrCount > 0) {
            for (int i = 0; i < attrCount; ++i) {
                to.writeAttribute(from.getAttributePrefix(i),
                        from.getAttributeNamespace(i),
                        from.getAttributeLocalName(i),
                        from.getAttributeValue(i));
            }
        }
    }

} // class DataViewParserGenericString
