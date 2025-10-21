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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.XmlStreamReaderUtils;


/**
 * An implementation of a Data View parser that parses Advanced Data Views. This
 * parser expects input that conforms to the CLARIN-FCS specification for the
 * Advanced Data View.
 *
 * @see DataViewAdvanced
 */
public final class DataViewParserAdvanced implements DataViewParser {
    private static final String FCS_ADV_NS =
            "http://clarin.eu/fcs/dataview/advanced";
    private static final String UNIT_ITEM      = "item";
    private static final String UNIT_TIMESTAMP = "timestamp";
    private static final Logger logger =
            LoggerFactory.getLogger(DataViewParserAdvanced.class);


    @Override
    public boolean acceptType(String type) {
        return DataViewAdvanced.TYPE.equals(type);
    }


    @Override
    public int getPriority() {
        return 1000;
    }


    @Override
    public DataView parse(XMLStreamReader reader, String type, String pid,
            String ref) throws XMLStreamException, SRUClientException {
        XmlStreamReaderUtils.readStart(reader, FCS_ADV_NS, "Advanced", true, true);
        final DataViewAdvanced.Unit unit = readUnit(reader);
        logger.debug("Advanced: unit={}", unit);
        reader.next(); // skip start tag

        // Segments
        final Map<String, DataViewAdvanced.Segment> segments = new HashMap<>();
        XmlStreamReaderUtils.readStart(reader, FCS_ADV_NS, "Segments", true);
        while (XmlStreamReaderUtils.readStart(reader, FCS_ADV_NS, "Segment",
                segments.isEmpty(), true)) {
            final String id =
                    XmlStreamReaderUtils.readAttributeValue(reader, null, "id");
            final long start = readOffset(reader, "start", unit);
            final long end = readOffset(reader, "end", unit);
            final URI reference = readAttributeURI(reader, null, "ref", false);
            if (start > end) {
                throw new SRUClientException("invalid offsets: start > end");
            }
            reader.next(); // skip start element
            XmlStreamReaderUtils.readEnd(reader, FCS_ADV_NS, "Segment");

            logger.debug("segment: id={}, start={}, end={}, ref={}",
                    id, start, end, reference);
            DataViewAdvanced.Segment segment =
                    new DataViewAdvanced.Segment(id, start, end, reference);
            segments.put(id, segment);
        } // while
        XmlStreamReaderUtils.readEnd(reader, FCS_ADV_NS, "Segments");

        // Layers
        List<DataViewAdvanced.Layer> layers =
                new ArrayList<>();

        XmlStreamReaderUtils.readStart(reader, FCS_ADV_NS, "Layers", true);
        while (XmlStreamReaderUtils.readStart(reader, FCS_ADV_NS, "Layer",
                layers.isEmpty(), true)) {
            String id = XmlStreamReaderUtils.readAttributeValue(reader, null, "id");
            reader.next(); // skip start element
            logger.debug("layer: id={}", id);

            final List<DataViewAdvanced.Span> spans =
                    new ArrayList<>();
            while (XmlStreamReaderUtils.readStart(reader, FCS_ADV_NS, "Span",
                    spans.isEmpty(), true)) {
                String segment_ref = XmlStreamReaderUtils.readAttributeValue(reader, null, "ref");
                String highlight = XmlStreamReaderUtils.readAttributeValue(reader, null, "highlight", false);
                String altValue = XmlStreamReaderUtils.readAttributeValue(reader, null, "alt-value", false);
                reader.next(); // skip start element
                String content = XmlStreamReaderUtils.readString(reader, false);
                XmlStreamReaderUtils.readEnd(reader, FCS_ADV_NS, "Span");

                logger.debug("span: ref={}, highlight={}, alt-value={}, content={}",
                        segment_ref, highlight, altValue, content);
                DataViewAdvanced.Segment segment = segments.get(segment_ref);
                if (segment == null) {
                    throw new XMLStreamException("No segment with id '" +
                            segment_ref + "' found", reader.getLocation());
                }
                DataViewAdvanced.Span span =
                        new DataViewAdvanced.Span(segment, highlight, altValue, content);
                spans.add(span);
            } // while
            XmlStreamReaderUtils.readEnd(reader, FCS_ADV_NS, "Layer");

            DataViewAdvanced.Layer layer =
                    new DataViewAdvanced.Layer(id, spans);
            layers.add(layer);
        } // while
        XmlStreamReaderUtils.readEnd(reader, FCS_ADV_NS, "Layers");

        XmlStreamReaderUtils.readEnd(reader, FCS_ADV_NS, "Advanced");
        return new DataViewAdvanced(pid, ref, unit, layers);
    }


    private static final DataViewAdvanced.Unit readUnit(XMLStreamReader reader)
            throws XMLStreamException {

        final String s = XmlStreamReaderUtils.readAttributeValue(reader, null,
                "unit", true);
        if (UNIT_ITEM.equals(s)) {
            return DataViewAdvanced.Unit.ITEM;
        } else if (UNIT_TIMESTAMP.equals(s)) {
            return DataViewAdvanced.Unit.TIMESTAMP;
        } else {
            throw new XMLStreamException(
                    "Attribute 'unit' may only have values '" + UNIT_ITEM +
                            "' or '" + UNIT_TIMESTAMP + "'",
                    reader.getLocation());
        }
    }


    private static final URI readAttributeURI(XMLStreamReader reader,
            String namespaceURI, String localName, boolean required)
                    throws XMLStreamException, SRUClientException {
        final String s = XmlStreamReaderUtils.readAttributeValue(reader,
                namespaceURI, localName, required);
        if (s != null) {
            try {
                return new URI(s);
            } catch (URISyntaxException e) {
                throw new XMLStreamException("malformed URI in attribute '" +
                        new QName(namespaceURI, localName) + "'",
                        reader.getLocation(), e);
            }
        } else {
            return null;
        }
    }


    private static final long readOffset(XMLStreamReader reader,
            String localName, DataViewAdvanced.Unit unit)
                    throws XMLStreamException, SRUClientException {
        String s = XmlStreamReaderUtils.readAttributeValue(reader,
                null, localName, true);
        switch (unit) {
        case ITEM:
            try {
                long num = Long.parseLong(s);
                if (num < 0) {
                    throw new XMLStreamException("offset is smaller than '0'",
                            reader.getLocation());
                }
                return num;
            } catch (NumberFormatException e) {
                throw new XMLStreamException(
                        "invalid number in attribute '" + localName + "'",
                        reader.getLocation(), e);
            }
        case TIMESTAMP:
            throw new SRUClientException("no support for 'timestamp' offsets, yet!");
        default:
            throw new SRUClientException("internal error: invalid unit (" +
                    unit + ")");
        }
    }

} // class DataViewParserAdvanced
