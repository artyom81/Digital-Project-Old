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

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRURecordData;


/**
 * A record data parse to parse legacy records.
 *
 * @deprecated Use only to talk to legacy clients. Endpoints should upgrade to
 *             recent CLARIN-FCS specification.
 */
@Deprecated
public class LegacyClarinFCSRecordDataParser extends
        AbstractClarinFCSRecordDataParser {
    private final boolean fullLegacyCompatMode;


    /**
     * Constructor.
     *
     * @param parsers
     *            the list of data view parsers to be used by this record data
     *            parser. This list should contain one
     *            {@link DataViewParserGenericDOM} or
     *            {@link DataViewParserGenericString} instance.
     * @param fullLegacyCompatMode
     *            enable full legacy CLARIN-FCS compat mode
     * @throws NullPointerException
     *             if parsers is <code>null</code>
     * @throws IllegalArgumentException
     *             if parsers is empty or contains duplicate entries
     */
    public LegacyClarinFCSRecordDataParser(List<DataViewParser> parsers,
            boolean fullLegacyCompatMode) {
        super(parsers);
        this.fullLegacyCompatMode = fullLegacyCompatMode;
    }


    @Override
    public String getRecordSchema() {
        return LegacyClarinFCSRecordData.RECORD_SCHEMA;
    }


    @Override
    public SRURecordData parse(XMLStreamReader reader)
            throws XMLStreamException, SRUClientException {
        if (!fullLegacyCompatMode) {
            logger.warn("The endpoint supplied data in the deprecated " +
                    "CLARIN-FCS record data format. Please upgrade to the " +
                    "new CLARIN-FCS specification as soon as possible.");
        }
        return parse(reader, LegacyClarinFCSRecordData.RECORD_SCHEMA,
                fullLegacyCompatMode);
    }

} // class LegacyClarinFCSRecordDataParser
