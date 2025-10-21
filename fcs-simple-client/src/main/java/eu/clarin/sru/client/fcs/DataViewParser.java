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

import eu.clarin.sru.client.SRUClientException;


/**
 * Base class for implementing parsers for parsing a specific Data View embedded
 * in a CLARIN-FCS record.
 * <p>
 * If multiple record parsers support a certain type, the one with the highest
 * priority is selected.
 * </p>
 */
public interface DataViewParser {

    /**
     * Check, if parser accepts a certain DataView type
     *
     * @param type
     *            the type to be checked
     * @return <code>true</code> if the parser supports this type,
     *         <code>false</code> otherwise
     */
    public boolean acceptType(String type);


    /**
     * Get the priority for this parser
     *
     * @return the priority for this parser
     */
    public int getPriority();


    /**
     * Parse a DataView. Implementations of this methods are required to be
     * thread-safe!
     *
     * @param reader
     *            the {@link XMLStreamReader} to read from
     * @param type
     *            the type of the DataView
     * @param pid
     *            the pid of this DataView
     * @param ref
     *            the reference of this DataView
     * @return the parsed {@link DataView} object
     * @throws XMLStreamException
     *             an error occurred while parsing the DataView
     * @throws SRUClientException
     *             any SRU exception, possibly wrapping another exception
     * @see DataView
     */
    public DataView parse(XMLStreamReader reader, String type, String pid,
            String ref) throws XMLStreamException, SRUClientException;

} // interface DataViewParser
