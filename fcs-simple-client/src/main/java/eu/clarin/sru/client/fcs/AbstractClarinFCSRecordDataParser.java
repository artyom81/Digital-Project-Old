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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRURecordData;
import eu.clarin.sru.client.SRURecordDataParser;
import eu.clarin.sru.client.XmlStreamReaderUtils;


/**
 * An abstract base class for CLARIN-FCS record data parsers.
 */
abstract class AbstractClarinFCSRecordDataParser implements SRURecordDataParser {
    protected static final Logger logger =
            LoggerFactory.getLogger(AbstractClarinFCSRecordDataParser.class);
    protected final List<DataViewParser> parsers;


    /**
     * Constructor.
     *
     * @param parsers
     *            the list of data view parsers to be used by this record data
     *            parser. This list should contain one
     *            {@link DataViewParserGenericDOM} or
     *            {@link DataViewParserGenericString} instance.
     * @throws NullPointerException
     *             if parsers is <code>null</code>
     * @throws IllegalArgumentException
     *             if parsers is empty or contains duplicate entries
     */
    protected AbstractClarinFCSRecordDataParser(List<DataViewParser> parsers) {
        if (parsers == null) {
            throw new NullPointerException("parsers == null");
        }
        if (parsers.isEmpty()) {
            throw new IllegalArgumentException("parsers is empty");
        }

        ArrayList<DataViewParser> list = new ArrayList<>();
        boolean foundGeneric = false;
        for (DataViewParser parser : parsers) {
            if ((parser instanceof DataViewParserGenericDOM) ||
                    (parser instanceof DataViewParserGenericString)) {
                if (foundGeneric) {
                    throw new IllegalAccessError("parser list should contain " +
                            "only one instance of DataViewParserGenericDOM " +
                            "or DataViewParserGenericString");
                }
                foundGeneric = true;
            }
            if (list.contains(parser)) {
                throw new IllegalArgumentException("parser list should not " +
                        "contain douplicates: " + parser.getClass().getName());
            }
            list.add(parser);
        }

        if (!foundGeneric) {
            logger.warn("No generic type data view parser found. You should " +
                    "make sure that the parser list contains one " +
                    "DataViewParserGenericDOM or DataViewParserGenericString " +
                    "instance");
        }

        this.parsers = Collections.unmodifiableList(list);
    }


    @SuppressWarnings("deprecation")
    protected SRURecordData parse(XMLStreamReader reader, String ns,
            boolean fullLegacyCompatMode)
            throws XMLStreamException, SRUClientException {
        // Resource
        XmlStreamReaderUtils.readStart(reader, ns, "Resource", true, true);
        final String pid =
                XmlStreamReaderUtils.readAttributeValue(reader, null, "pid");
        final String ref =
                XmlStreamReaderUtils.readAttributeValue(reader, null, "ref");
        XmlStreamReaderUtils.consumeStart(reader);

        // Resource/Resource (optional)
        if (XmlStreamReaderUtils.readStart(reader, ns, "Resource", false)) {
            logger.info("skipping nested <Resource> element");
            XmlStreamReaderUtils.readEnd(reader, ns, "Resource", true);
        }

        // Resource/DataView
        final List<DataView> dataviews = parseDataViews(reader, ns);

        // Resource/ResourceFragment
        final List<Resource.ResourceFragment> resourceFragments =
                parseResourceFragments(reader, ns);

        XmlStreamReaderUtils.readEnd(reader, ns, "Resource", true);
        if (fullLegacyCompatMode) {
            return new LegacyClarinFCSRecordData(pid, ref, dataviews,
                    resourceFragments);
        } else {
            return new ClarinFCSRecordData(pid, ref, dataviews,
                    resourceFragments);
        }
    }


    private List<DataView> parseDataViews(XMLStreamReader reader, String ns)
            throws XMLStreamException, SRUClientException {
        List<DataView> dataviews = null;

        while (XmlStreamReaderUtils.readStart(reader, ns, "DataView", false, true)) {
            final String pid =
                    XmlStreamReaderUtils.readAttributeValue(reader, null, "pid");
            final String ref =
                    XmlStreamReaderUtils.readAttributeValue(reader, null, "ref");
            final String type =
                    XmlStreamReaderUtils.readAttributeValue(reader, null, "type");
            if ((type == null) || type.isEmpty()) {
                throw new SRUClientException("element <DataView> needs a "
                        + "non-empty 'type' attribute");
            }

            // consume start element and get rid of any whitespace
            XmlStreamReaderUtils.consumeStart(reader);
            XmlStreamReaderUtils.consumeWhitespace(reader);

            logger.debug("processing <DataView> of type = {}", type);

            DataViewParser selectedParser = null;
            for (DataViewParser parser : parsers) {
                if (parser.acceptType(type) &&
                        ((selectedParser == null) ||
                         (selectedParser.getPriority() < parser.getPriority()))) {
                    selectedParser = parser;
                }
            }

            DataView dataview = null;
            if (selectedParser != null) {
                dataview = selectedParser.parse(reader, type, pid, ref);
            } else {
                logger.warn("no parser found for <DataView> of type = {}", type);
            }

            XmlStreamReaderUtils.readEnd(reader, ns, "DataView", true);

            if (dataview != null) {
                if (dataviews == null) {
                    dataviews = new LinkedList<>();
                }
                dataviews.add(dataview);
            } else {
                logger.warn("skipped <DataView> of type = {}", type);
            }
        } // while
        return dataviews;
    }


    private List<Resource.ResourceFragment> parseResourceFragments(
            XMLStreamReader reader, String ns) throws XMLStreamException,
            SRUClientException {
        List<Resource.ResourceFragment> resourceFragments = null;
        while (XmlStreamReaderUtils.readStart(reader, ns, "ResourceFragment", false, true)) {
            logger.debug("found ResourceFragment");
            String pid = XmlStreamReaderUtils.readAttributeValue(reader, null, "pid");
            String ref = XmlStreamReaderUtils.readAttributeValue(reader, null, "ref");
            XmlStreamReaderUtils.consumeStart(reader);
            final List<DataView> dataviews = parseDataViews(reader, ns);
            XmlStreamReaderUtils.readEnd(reader, ns, "ResourceFragment", true);

            if (resourceFragments == null) {
                resourceFragments = new LinkedList<>();
            }
            resourceFragments.add(new Resource.ResourceFragment(pid, ref, dataviews));
        } // while
        return resourceFragments;
    }

} // class AbstractClarinFCSRecordDataParser
