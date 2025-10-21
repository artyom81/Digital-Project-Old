/* This software is copyright (c) 2012-2021 by
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
package eu.clarin.sru.client.fcs.utils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRUExtraResponseData;
import eu.clarin.sru.client.SRUExtraResponseDataParser;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.XmlStreamReaderUtils;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import eu.clarin.sru.client.fcs.ClarinFCSConstants;


/**
 * As stripped down client for auto-detecting the FCS version supported by an
 * endpoint.
 */
public class ClarinFCSEndpointVersionAutodetector {
    private static final Logger logger = LoggerFactory
            .getLogger(ClarinFCSEndpointVersionAutodetector.class);
    private final SRUClient client;


    /**
     * The auto-detected FCS version
     */
    public static enum AutodetectedFCSVersion {
        /**
         * Unknown FCS version auto-detected
         */
        UNKNOWN {
            @Override
            public int getVersion() {
                return -1;
            }
        },

        /**
         * Legacy FCS
         */
        FCS_LEGACY {
            @Override
            public int getVersion() {
                return 0;
            }
        },

        /**
         * FCS version 1.0 auto-detected
         */
        FCS_1_0 {
            @Override
            public int getVersion() {
                return ((1 << 16) | 0);
            }
        },

        /**
         * FCS version 2.0 auto-detected
         */
        FCS_2_0 {
            @Override
            public int getVersion() {
                return ((2 << 16) | 0);
            }
        };

        /**
         * Get a numerical representation of the auto-detected version.
         *
         * @return numerical representation of the auto-detected version
         */
        public abstract int getVersion();
    }


    /**
     * Constructor.
     */
    public ClarinFCSEndpointVersionAutodetector() {
        client = new ClarinFCSClientBuilder()
                .setDefaultSRUVersion(SRUVersion.VERSION_2_0)
                .unknownDataViewAsString()
                .enableLegacySupport()
                .registerExtraResponseDataParser(
                        new AutodetectClarinFCSEndpointDescriptionParser())
                .buildClient();
    }


    /**
     * Try to auto-detected the FCS version of and endpoint.
     *
     * @param endpointURI
     *            the URI of the endpoint
     * @return the detected version
     * @throws SRUClientException
     *             if an error occurred
     */
    public AutodetectedFCSVersion autodetectVersion(String endpointURI)
            throws SRUClientException {
        // assume unknown
        AutodetectedFCSVersion version = AutodetectedFCSVersion.UNKNOWN;

        try {
            logger.debug("performing SRU 1.2 explain request to endpoint \"{}\"",
                        endpointURI);
            SRUExplainRequest request = new SRUExplainRequest(endpointURI);
            request.setStrictMode(false);
            request.setVersion(SRUVersion.VERSION_1_2);
            request.setExtraRequestData(
                    ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION,
                    ClarinFCSConstants.TRUE);
            request.setParseRecordDataEnabled(true);
            SRUExplainResponse response = client.explain(request);
            AutodetectClarinFCSEndpointDescription ed =
                    response.getFirstExtraResponseData(
                            AutodetectClarinFCSEndpointDescription.class);
            if (ed != null) {
                /*
                 * FCS 1.0 returns an endpoint description on explain. If
                 * version is 1, endpoint is FCS 1.0.
                 * Note: FCS 2.0 is using SRU 2.0, so we cannot check for
                 * FCS 2.0 just yet. First we need to perform a 2.0 request.
                 */
                if (ed.getVersion() == 1) {
                    version = AutodetectedFCSVersion.FCS_1_0;
                }
            } else {
                /*
                 * FCS legacy has no endpojnt description, so if none was
                 * found by the parser, we assume Legacy FCS
                 */
                version = AutodetectedFCSVersion.FCS_LEGACY;
            }

            // if still unknown, try FCS 2.0
            if (version == AutodetectedFCSVersion.UNKNOWN) {
                logger.debug("performing SRU 2.0 explain request to endpoint \"{}\"",
                        endpointURI);
                request.setVersion(SRUVersion.VERSION_2_0);
                response = client.explain(request);
                ed = response.getFirstExtraResponseData(
                        AutodetectClarinFCSEndpointDescription.class);
                if (ed != null) {
                    if (ed.getVersion() == 2) {
                        version = AutodetectedFCSVersion.FCS_2_0;
                    }
                    // NOTE: SRU 2.0 should really not respond with version=1 ?
                }
            }
            return version;
        } catch (SRUClientException e) {
            logger.warn("First version detection failed, probably due to invalid SRU 1.2"
                    + " explain response", e);
            /*
             * Test for lastest FCS version (FCS 2.0) since newer endpoints might not
             * correctly implement SRU 1.2 handling and only target SRU 2.0.
             * So, if SRU 1.2 fails due to unexpected namespaces, let's try to check with
             * FCS 2.0 with SRU 2.0 first.
             */
            try {
                logger.debug("performing SRU 2.0 explain request to endpoint \"{}\""
                        + " (after SRU 1.2 (?) explain request failed)",
                        endpointURI);
                SRUExplainRequest request = new SRUExplainRequest(endpointURI);
                request.setStrictMode(false);
                request.setVersion(SRUVersion.VERSION_2_0);
                request.setExtraRequestData(
                        ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION,
                        ClarinFCSConstants.TRUE);
                request.setParseRecordDataEnabled(true);
                SRUExplainResponse response = client.explain(request);
                AutodetectClarinFCSEndpointDescription ed =
                        response.getFirstExtraResponseData(
                                AutodetectClarinFCSEndpointDescription.class);
                if (ed != null) {
                    if (ed.getVersion() == 2) {
                        version = AutodetectedFCSVersion.FCS_2_0;
                    }
                }
                return version;
            } catch (SRUClientException e2) {
                throw e2;
            }
        }
    }


    private static final QName ED_ROOT_ELEMENT = new QName(
            "http://clarin.eu/fcs/endpoint-description", "EndpointDescription");


    private static final class AutodetectClarinFCSEndpointDescription
            implements SRUExtraResponseData {
        private final int version;


        private AutodetectClarinFCSEndpointDescription(int version) {
            this.version = version;
        }


        @Override
        public QName getRootElement() {
            return ED_ROOT_ELEMENT;
        }


        public int getVersion() {
            return version;
        }
    }


    private static final class AutodetectClarinFCSEndpointDescriptionParser
            implements SRUExtraResponseDataParser {

        @Override
        public boolean supports(QName name) {
            return ED_ROOT_ELEMENT.equals(name);
        }

        @Override
        public SRUExtraResponseData parse(XMLStreamReader reader)
                throws XMLStreamException, SRUClientException {
            int version = parseVersion(reader);
            reader.next(); // skip start tag

            /*
             * Skip all remaining elements. The XMLStreamReaderProxy prevents
             * us from reading beyond the extraResponeData contents.
             *
             */
            while (reader.getEventType() != XMLStreamConstants.END_DOCUMENT) {
                reader.next();
            }
            return new AutodetectClarinFCSEndpointDescription(version);
        }


        private static int parseVersion(XMLStreamReader reader)
                throws XMLStreamException {
            try {
                final String s = XmlStreamReaderUtils.readAttributeValue(
                        reader, null, "version", true);
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new XMLStreamException("Attribute 'version' is not a number",
                        reader.getLocation(), e);
            }
        }

    }

} // class ClarinFCSEndpointVersionAutodetector
