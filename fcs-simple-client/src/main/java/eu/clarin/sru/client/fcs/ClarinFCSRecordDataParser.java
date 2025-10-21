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
 * A record data parse to parse records conforming to CLARIN-FCS specification.
 */
public class ClarinFCSRecordDataParser extends
        AbstractClarinFCSRecordDataParser {


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
    public ClarinFCSRecordDataParser(List<DataViewParser> parsers) {
        super(parsers);
    }



    @Override
    public String getRecordSchema() {
        return ClarinFCSRecordData.RECORD_SCHEMA;
    }


    @Override
    public SRURecordData parse(XMLStreamReader reader)
            throws XMLStreamException, SRUClientException {
        logger.debug("parsing CLARIN-FCS record");

        return parse(reader, ClarinFCSRecordData.RECORD_SCHEMA, false);
    }

} // class ClarinFCSRecordParser
