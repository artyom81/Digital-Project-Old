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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExtraResponseData;
import eu.clarin.sru.client.SRUExtraResponseDataParser;
import eu.clarin.sru.client.XmlStreamReaderUtils;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.DataView;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.DataView.DeliveryPolicy;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.Layer;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.LexField;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.Layer.ContentEncoding;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.ResourceInfo.AvailabilityRestriction;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.ResourceInfo;


/**
 * An extra response data parser for parsing CLARIN-FCS endpoint descriptions.
 */
public class ClarinFCSEndpointDescriptionParser implements
        SRUExtraResponseDataParser {
    /**
     * constant for infinite resource enumeration parsing depth
     */
    public static final int INFINITE_MAX_DEPTH = -1;
    /**
     * constant for default parsing resource enumeration parsing depth
     */
    public static final int DEFAULT_MAX_DEPTH = INFINITE_MAX_DEPTH;
    /**
     * constant for default parsing method (event based streaming, or in-memory XML DOM)
     */
    public static final boolean DEFAULT_PARSING_STREAMING = false;

    private static final Logger logger = LoggerFactory.getLogger(ClarinFCSClientBuilder.class);
    private static final String ED_NS_URI = "http://clarin.eu/fcs/endpoint-description";
    private static final String ED_NS_LEGACY_URI = "http://clarin.eu/fcs/1.0/resource-info";
    private static final QName ED_ROOT_ELEMENT = new QName(ED_NS_URI, "EndpointDescription");
    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;
    private static final String CAPABILITY_PREFIX = "http://clarin.eu/fcs/capability/";
    private static final String MIMETYPE_HITS_DATAVIEW = "application/x-clarin-fcs-hits+xml";
    private static final String MIMETYPE_ADV_DATAVIEW = "application/x-clarin-fcs-adv+xml";
    private static final String MIMETYPE_LEX_DATAVIEW = "application/x-clarin-fcs-lex+xml";
    private static final String LANG_EN = "en";
    private static final String POLICY_SEND_DEFAULT = "send-by-default";
    private static final String POLICY_NEED_REQUEST = "need-to-request";
    private static final String LAYER_ENCODING_VALUE = "value";
    private static final String LAYER_ENCODING_EMPTY = "empty";
    private static final String AVAILABILITY_RESTRICTION_AUTHONLY = "authOnly";
    private static final String AVAILABILITY_RESTRICTION_PERSONALID = "personalIdentifier";

    private final int maxDepth;
    private final boolean streaming;

    /**
     * Constructor. By default, the parser will parse the endpoint resource
     * enumeration to an infinite depth. It will also parse the XML using an
     * in-memory DOM with XPaths.
     */
    public ClarinFCSEndpointDescriptionParser() {
        this(DEFAULT_MAX_DEPTH, DEFAULT_PARSING_STREAMING);
    }


    /**
     * Constructor. By default, the parser will use in-memory DOM with XPaths.
     *
     * @param maxDepth
     *            maximum depth for parsing the endpoint resource enumeration.
     * @throws IllegalArgumentException
     *             if an argument is illegal
     */
    public ClarinFCSEndpointDescriptionParser(int maxDepth) {
        this(maxDepth, DEFAULT_PARSING_STREAMING);
    }


    /**
     * Constructor. By default, the parser will parse the endpoint resource
     * enumeration to an infinite depth.
     *
     * @param streaming
     *            parse SRU extra response data in streaming manner instead of
     *            in-memory DOM using XPaths.
     * @throws IllegalArgumentException
     *             if an argument is illegal
     */
    public ClarinFCSEndpointDescriptionParser(boolean streaming) {
        this(DEFAULT_MAX_DEPTH, streaming);
    }


    /**
     * Constructor.
     *
     * @param maxDepth
     *            maximum depth for parsing the endpoint resource enumeration.
     * @param streaming
     *            parse SRU extra response data in streaming manner instead of
     *            in-memory DOM using XPaths.
     * @throws IllegalArgumentException
     *             if an argument is illegal
     */
    public ClarinFCSEndpointDescriptionParser(int maxDepth, boolean streaming) {
        if (maxDepth < -1) {
            throw new IllegalArgumentException("maxDepth < -1");
        }
        this.maxDepth = maxDepth;
        this.streaming = streaming;
    }

    @Override
    public boolean supports(QName name) {
        return ED_ROOT_ELEMENT.equals(name);
    }


    @Override
    public SRUExtraResponseData parse(XMLStreamReader reader)
            throws XMLStreamException, SRUClientException {
        if (streaming) {
            return parseEndpointDescription(reader, maxDepth);
        } else {
            logger.debug("parsing with xpath");
            final Document erdDoc = XmlStreamReaderUtils.parseToDocument(reader);
            try {
                checkLegacyMode(erdDoc);
                return parseEndpointDescription(erdDoc, maxDepth);
            } catch (XPathExpressionException e) {
                throw new SRUClientException("internal error", e);
            }
        }
    }


    /**
     * Get the maximum resource enumeration parsing depth. The first level is
     * indicate by the value <code>0</code>.
     *
     * @return the default resource parsing depth or <code>-1</code> for
     *         infinite.
     */
    public int getMaximumResourceParsingDepth() {
        return maxDepth;
    }


    // -----------------------------------------------------------------------

    private static void checkLegacyMode(Document doc)
            throws SRUClientException {
        Element root = doc.getDocumentElement();
        if (root != null) {
            String ns = root.getNamespaceURI();
            if (ns != null) {
                if (ns.equals(ED_NS_LEGACY_URI)) {
                    logger.error("Detected out-dated resource info catalog file." +
                            " Update to the current version is required");
                    throw new SRUClientException("unsupport file format: " + ns);
                } else if (!ns.equals(ED_NS_URI)) {
                    logger.error("Detected unsupported resource info catalog file " + 
                            " with namespace '" + ns + '"');
                    throw new SRUClientException("Unsupport file format: " + ns);
                }
            } else {
                throw new SRUClientException("No namespace URI was detected " +
                        "for resource info catalog file!");
            }
        } else {
            throw new SRUClientException("Error retrieving root element");
        }
    }


    private static ClarinFCSEndpointDescription parseEndpointDescription(Document doc, int maxDepth)
        throws SRUClientException, XPathExpressionException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getPrefix(String namespaceURI) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    throw new NullPointerException("prefix == null");
                }
                if (prefix.equals("ed")) {
                    return ED_NS_URI;
                } else if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
                    return XMLConstants.XML_NS_URI;
                } else {
                    return XMLConstants.NULL_NS_URI;
                }
            }
        });

        // version
        int version = -1;
        XPathExpression exp = xpath.compile("//ed:EndpointDescription/@version");
        String v = (String) exp.evaluate(doc, XPathConstants.STRING);
        if (v != null) {
            try {
                version = Integer.parseInt(v);
                if ((version != 1) && (version != 2)) {
                    throw new SRUClientException("Attribute @version element "
                            + "<EndpointDescription> must have a value of either '1' or '2' ");
                }
            } catch (NumberFormatException e) {
                throw new SRUClientException("Cannot parse version number", e);
            }
        }
        if (version == -1) {
            throw new SRUClientException("Attribute @version missing on element <EndpointDescription>");
        }
        logger.debug("Endpoint description version is {}", version);

        // Capabilities
        List<URI> capabilities = parseCapabilities(xpath, doc, version);
        logger.debug("CAP: {}", capabilities);

        // used to check for uniqueness of id attribute
        final Set<String> xml_ids = new HashSet<>();

        // SupportedDataViews
        List<DataView> supportedDataViews = parseSupportedDataViews(xpath, doc, capabilities, xml_ids);
        logger.debug("DV: {}", supportedDataViews);

        // SupportedLayers
        List<Layer> supportedLayers = parseSupportedLayers(xpath, doc, capabilities, xml_ids);
        logger.debug("L: {}", supportedLayers);

        // SupportedLexFields
        List<LexField> supportedLexFields = parseSupportedLexFields(xpath, doc, capabilities, xml_ids);
        logger.debug("F: {}", supportedLexFields);

        // Resources
        exp = xpath.compile("/ed:EndpointDescription/ed:Resources/ed:Resource");
        NodeList list = (NodeList) exp.evaluate(doc, XPathConstants.NODESET);
        final Set<String> pids = new HashSet<>();
        List<ResourceInfo> resources = parseResources(xpath, list, 0, maxDepth, pids,
                supportedDataViews, supportedLayers, supportedLexFields, version, capabilities);
        if ((resources == null) || resources.isEmpty()) {
            throw new SRUClientException("No resources where defined in endpoint description");
        }

        return new ClarinFCSEndpointDescription(version, capabilities,
                supportedDataViews, supportedLayers, supportedLexFields, resources);
    }


    private static List<URI> parseCapabilities(XPath xpath, Document doc, int version)
            throws SRUClientException, XPathExpressionException {
        List<URI> capabilities = new ArrayList<>();
        XPathExpression exp = xpath.compile("//ed:Capabilities/ed:Capability");
        NodeList list = (NodeList) exp.evaluate(doc, XPathConstants.NODESET);
        if ((list != null) && (list.getLength() > 0)) {
            for (int i = 0; i < list.getLength(); i++) {
                String s = list.item(i).getTextContent().trim();
                try {
                    URI uri = new URI(s);
                    if (!capabilities.contains(uri)) {
                        capabilities.add(uri);
                    } else {
                        logger.debug("ignoring duplicate capability entry for '{}'", uri);
                    }
                } catch (URISyntaxException e) {
                    throw new SRUClientException("capability is not encoded as a proper URI: " + s);
                }
            }
        }
        final boolean hasBasicSearch = (capabilities.indexOf(ClarinFCSConstants.CAPABILITY_BASIC_SEARCH) != -1);
        final boolean hasAdvancedSearch = (capabilities.indexOf(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH) != -1);
        final boolean hasAuthenticatedSearch = (capabilities.indexOf(ClarinFCSConstants.CAPABILITY_AUTHENTICATED_SEARCH) != -1);
        if (!hasBasicSearch) {
            throw new SRUClientException("Endpoint must support 'basic-search' (" +
                    ClarinFCSConstants.CAPABILITY_BASIC_SEARCH +
                    ") to conform to CLARIN-FCS specification");
        }
        if (hasAdvancedSearch && (version < 2)) {
            logger.warn("Endpoint description is declared as version " +
                    "FCS 1.0 (@version = 1), but contains support for " +
                    "Advanced Search in capabilities list! FCS 1.0 only " +
                    "supports Basic Search");
        }
        if (hasAuthenticatedSearch && (version < 2)) {
            logger.warn("Endpoint description is declared as version " +
                    "FCS 1.0 (@version = 1), but contains support for " +
                    "Authenticated Search in capabilities list! FCS 1.0 only " +
                    "supports Basic Search");
        }

        return capabilities;
    }


    private static List<DataView> parseSupportedDataViews(XPath xpath, Document doc,
            List<URI> capabilities, Set<String> xml_ids)
            throws SRUClientException, XPathExpressionException {
        List<DataView> supportedDataViews = new ArrayList<>();
        XPathExpression exp = xpath.compile("//ed:SupportedDataViews/ed:SupportedDataView");
        NodeList list = (NodeList) exp.evaluate(doc, XPathConstants.NODESET);
        if ((list != null) && (list.getLength() > 0)) {
            for (int i = 0; i < list.getLength(); i++) {
                Element item = (Element) list.item(i);
                String id = getAttribute(item, "id");
                if (id == null) {
                    throw new SRUClientException("Element <SupportedDataView> "
                            + "must have a proper 'id' attribute");
                }
                if ((id.indexOf(' ') != -1) || (id.indexOf(',') != -1) ||
                        (id.indexOf(';') != -1)) {
                    throw new SRUClientException("Value of attribute 'id' on " +
                            "element '<SupportedDataView>' may not contain the " +
                            "characters ',' (comma) or ';' (semicolon) " +
                            "or ' ' (space)");
                }

                if (xml_ids.contains(id)) {
                    throw new SRUClientException("The value of attribute " +
                            "'id' of element <SupportedDataView> must be " +
                            "unique: " + id);
                }
                xml_ids.add(id);

                String p = getAttribute(item, "delivery-policy");
                if (p == null) {
                    throw new SRUClientException("Element <SupportedDataView> "
                            + "must have a 'delivery-policy' attribute");
                }
                DeliveryPolicy policy = null;
                if (POLICY_SEND_DEFAULT.equals(p)) {
                    policy = DeliveryPolicy.SEND_BY_DEFAULT;
                } else if (POLICY_NEED_REQUEST.equals(p)) {
                    policy = DeliveryPolicy.NEED_TO_REQUEST;
                } else {
                    throw new SRUClientException("Invalid value '" + p +
                            "' for attribute 'delivery-policy' on element " +
                            "<SupportedDataView>");
                }
                String mimeType = item.getTextContent();
                if (mimeType != null) {
                    mimeType = mimeType.trim();
                    if (mimeType.isEmpty()) {
                        mimeType = null;
                    }
                }
                if (mimeType == null) {
                    throw new SRUClientException("Element <SupportedDataView> " +
                            "must contain a MIME-type as content");
                }
                // check for duplicate entries ...
                for (DataView dataView : supportedDataViews) {
                    if (id.equals(dataView.getIdentifier())) {
                        throw new SRUClientException("A <SupportedDataView> with " +
                                "the id '" + id + "' is already defined!");
                    }
                    if (mimeType.equals(dataView.getMimeType())) {
                        throw new SRUClientException("A <SupportedDataView> with " +
                                "the MIME-type '" + mimeType + "' is already defined!");
                    }
                }
                supportedDataViews.add(new DataView(id, mimeType, policy));
            }
        } else {
            throw new SRUClientException("Endpoint configuration contains " +
                    "no valid information about supported data views");
        }

        final boolean hasAdvancedSearch = capabilities.contains(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH);
        final boolean hasLexicalSearch = capabilities.contains(ClarinFCSConstants.CAPABILITY_LEX_SEARCH);
        boolean hasHitsView = false;
        boolean hasAdvView = false;
        boolean hasLexView = false;
        if (supportedDataViews != null) {
            for (DataView dataView : supportedDataViews) {
                if (MIMETYPE_HITS_DATAVIEW.equals(dataView.getMimeType())) {
                    hasHitsView = true;
                } else if (MIMETYPE_ADV_DATAVIEW.equals(dataView.getMimeType())) {
                    hasAdvView = true;
                } else if (MIMETYPE_LEX_DATAVIEW.equals(dataView.getMimeType())) {
                    hasLexView = true;
                }
            }
        }
        if (!hasHitsView) {
            throw new SRUClientException("Endpoint must support " +
                    "generic hits dataview (expected MIME type '" +
                    MIMETYPE_HITS_DATAVIEW +
                    "') to conform to CLARIN-FCS specification");
        }
        if (hasAdvancedSearch && !hasAdvView) {
            throw new SRUClientException("Endpoint claimes to support " +
                    "Advanced FCS but does not declare Advanced Data View (" +
                    MIMETYPE_ADV_DATAVIEW + ") in <SupportedDataViews>");
        }
        if (hasLexicalSearch && !hasLexView) {
            throw new SRUClientException("Endpoint claimes to support " +
                    "Lexical FCS but does not declare Lex Data View (" +
                    MIMETYPE_LEX_DATAVIEW + ") in <SupportedDataViews>");
        }

        return supportedDataViews;
    }


    private static List<Layer> parseSupportedLayers(XPath xpath, Document doc,
            List<URI> capabilities, Set<String> xml_ids)
            throws SRUClientException, XPathExpressionException {
        List<Layer> supportedLayers = null;
        XPathExpression exp = xpath.compile("//ed:SupportedLayers/ed:SupportedLayer");
        NodeList list = (NodeList) exp.evaluate(doc, XPathConstants.NODESET);
        if ((list != null) && (list.getLength() > 0)) {
            logger.debug("parsing supported layers");
            for (int i = 0; i < list.getLength(); i++) {
                Element item = (Element) list.item(i);
                String id = getAttribute(item, "id");
                if (id == null) {
                    throw new SRUClientException("Element <SupportedLayer> "
                            + "must have a proper 'id' attribute");
                }

                if (xml_ids.contains(id)) {
                    throw new SRUClientException("The value of attribute " +
                            "'id' of element <SupportedLayer> must be " +
                            "unique: " + id);
                }
                xml_ids.add(id);

                String s = getAttribute(item, "result-id");
                if (s == null) {
                    throw new SRUClientException("Element <SupportedLayer> "
                            + "must have a proper 'result-id' attribute");
                }
                URI resultId = null;
                try {
                    resultId = new URI(s);
                } catch (URISyntaxException e) {
                    throw new SRUClientException("Attribute 'result-id' on " +
                            "Element <SupportedLayer> is not encoded " +
                            "as proper URI: " + s);
                }

                String type = cleanString(item.getTextContent());
                if ((type != null) && !type.isEmpty()) {
                    // sanity check on layer types
                    if (!(type.equals("text") ||
                            type.equals("lemma") ||
                            type.equals("pos") ||
                            type.equals("orth") ||
                            type.equals("norm") ||
                            type.equals("phonetic") ||
                            type.startsWith("x-"))) {
                        logger.debug("layer type '{}' is not defined by specification", type);
                    }
                } else {
                    throw new SRUClientException("Element <SupportedLayer> " +
                            "does not define a proper layer type");
                }

                String qualifier = getAttribute(item, "qualifier");

                Layer.ContentEncoding encoding =
                        Layer.ContentEncoding.VALUE;
                s = getAttribute(item, "type");
                if (s != null) {
                    if (LAYER_ENCODING_VALUE.equals(s)) {
                        encoding = Layer.ContentEncoding.VALUE;
                    } else if (LAYER_ENCODING_EMPTY.equals(s)) {
                        encoding = Layer.ContentEncoding.EMPTY;
                    } else {
                        throw new SRUClientException(
                                "invalid layer encoding: " + s);
                    }
                }

                String altValueInfo = getAttribute(item, "alt-value-info");
                URI altValueInfoURI = null;
                if (altValueInfo != null) {
                    s = getAttribute(item, "alt-value-info-uri");
                    if (s != null) {
                        try {
                          altValueInfoURI = new URI(s);
                        } catch (URISyntaxException e) {
                            throw new SRUClientException("Attribute " +
                                    "'alt-value-info-uri' on Element " +
                                    "<SupportedLayer> is not encoded " +
                                    "as proper URI: " + s);
                        }
                    }
                }

                if (supportedLayers == null) {
                    supportedLayers = new ArrayList<>(list.getLength());
                }
                supportedLayers.add(new Layer(id, resultId, type, encoding,
                        qualifier, altValueInfo, altValueInfoURI));
            }
        }

        final boolean hasAdvancedSearch = capabilities.contains(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH);
        if (hasAdvancedSearch && (supportedLayers == null)) {
            throw new SRUClientException("Endpoint must declare " +
                    "all supported layers (<SupportedLayers>) if they " +
                    "provide the 'advanced-search' (" +
                    ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH +
                    ") capability");
        }
        if ((supportedLayers != null) && !hasAdvancedSearch) {
                logger.warn("Endpoint description has <SupportedLayer> but " +
                        "does not indicate support for Advanced Search using " +
                        "the capability ({})!",
                        ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH);
        } // necessary

        return supportedLayers;
    }


    private static List<LexField> parseSupportedLexFields(XPath xpath, Document doc,
            List<URI> capabilities, Set<String> xml_ids)
            throws SRUClientException, XPathExpressionException {
        List<LexField> supportedLexFields = null;
        XPathExpression exp = xpath.compile("//ed:SupportedLexFields/ed:SupportedLexField");
        NodeList list = (NodeList) exp.evaluate(doc, XPathConstants.NODESET);
        if ((list != null) && (list.getLength() > 0)) {
            logger.debug("parsing supported layers");
            for (int i = 0; i < list.getLength(); i++) {
                Element item = (Element) list.item(i);
                String id = getAttribute(item, "id");
                if (id == null) {
                    throw new SRUClientException("Element <SupportedLexField> "
                            + "must have a proper 'id' attribute");
                }

                if (xml_ids.contains(id)) {
                    throw new SRUClientException("The value of attribute " +
                            "'id' of element <SupportedLexField> must be " +
                            "unique: " + id);
                }
                xml_ids.add(id);

                String type = cleanString(item.getTextContent());
                if ((type != null) && !type.isEmpty()) {
                    // sanity check on layer types
                    boolean valid = false;
                    try {
                        DataViewLex.FieldType.fromString(type);
                        valid = true;
                    } catch (IllegalArgumentException iae) {
                    }
                    if (!valid) {
                        try {
                            DataViewLex.VirtualFieldType.fromString(type);
                            valid = true;
                        } catch (IllegalArgumentException iae) {
                        }
                    }
                    if (!valid) {
                        // TODO: check for "x-" extensible type definition?
                        if (!type.startsWith("x-")) {
                            logger.debug("lex field type '{}' is not defined by specification", type);
                        }
                    }
                } else {
                    throw new SRUClientException("Element <SupportedLexField> " +
                            "does not define a proper field type");
                }

                if (supportedLexFields == null) {
                    supportedLexFields = new ArrayList<>(list.getLength());
                }
                supportedLexFields.add(new LexField(id, type));
            }
        }

        final boolean hasLexicalSearch = capabilities.contains(ClarinFCSConstants.CAPABILITY_LEX_SEARCH);
        if (hasLexicalSearch && (supportedLexFields == null)) {
            throw new SRUClientException("Endpoint must declare " +
                    "all supported lex fields (<SupportedLexFields>) if they " +
                    "provide the 'lex-search' (" +
                    ClarinFCSConstants.CAPABILITY_LEX_SEARCH +
                    ") capability");
        }
        if ((supportedLexFields != null) && !hasLexicalSearch) {
                logger.warn("Endpoint description has <SupportedLexFields> but " +
                        "does not indicate support for Lexical Search using " +
                        "the capability ({})!",
                        ClarinFCSConstants.CAPABILITY_LEX_SEARCH);
        } // necessary

        return supportedLexFields;
    }


    private static List<ResourceInfo> parseResources(XPath xpath, NodeList nodes,
            int depth, int maxDepth, Set<String> pids, List<DataView> supportedDataViews,
            List<Layer> supportedLayers, List<LexField> supportedLexFields, int version,
            List<URI> capabilities)
                    throws SRUClientException, XPathExpressionException {
        final boolean hasAdvancedSearch = capabilities.contains(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH);
        final boolean hasLexicalSearch = capabilities.contains(ClarinFCSConstants.CAPABILITY_LEX_SEARCH);
        final boolean hasAuthenticatedSearch = capabilities.contains(ClarinFCSConstants.CAPABILITY_AUTHENTICATED_SEARCH);
        List<ResourceInfo> ris = null;

        for (int k = 0; k < nodes.getLength(); k++) {
            final Element node = (Element) nodes.item(k);
            String pid = null;
            Map<String, String> titles = null;
            Map<String, String> descrs = null;
            Map<String, String> insts = null;
            String link = null;
            List<String> langs = null;
            AvailabilityRestriction availabilityRestriction = AvailabilityRestriction.NONE;
            List<DataView> availableDataViews = null;
            List<Layer> availableLayers = null;
            List<LexField> availableLexFields = null;
            List<ResourceInfo> sub = null;

            pid = getAttribute(node, "pid");
            if (pid == null) {
                throw new SRUClientException("Element <ResourceInfo> " +
                        "must have a proper 'pid' attribute");
            }
            if (pids.contains(pid)) {
                throw new SRUClientException("Another element <Resource> " +
                        "with pid '" + pid + "' already exists");
            }
            pids.add(pid);
            logger.debug("Processing resource with pid '{}' at level {}", pid, depth);

            XPathExpression exp = xpath.compile("ed:Title");
            NodeList list = (NodeList) exp.evaluate(node, XPathConstants.NODESET);
            if ((list != null) && (list.getLength() > 0)) {
                for (int i = 0; i < list.getLength(); i++) {
                    final Element n = (Element) list.item(i);

                    final String lang = getLangAttribute(n);
                    if (lang == null) {
                        throw new SRUClientException("Element <Title> " +
                                "must have a proper 'xml:lang' attribute");
                    }

                    final String title = cleanString(n.getTextContent());
                    if (title == null) {
                        throw new SRUClientException("Element <Title> " +
                                "must have a non-empty 'xml:lang' attribute");
                    }

                    if (titles == null) {
                        titles = new HashMap<>();
                    }
                    if (titles.containsKey(lang)) {
                        logger.warn("A <Title> with language '{}' already exists",
                                lang);
                    } else {
                        titles.put(lang, title);
                    }
                }
                if ((titles != null) && !titles.containsKey(LANG_EN)) {
                    logger.warn("A <Title> with language 'en' is mandatory");
                }
            }
            logger.debug("Title: {}", titles);

            exp = xpath.compile("ed:Description");
            list = (NodeList) exp.evaluate(node, XPathConstants.NODESET);
            if ((list != null) && (list.getLength() > 0)) {
                for (int i = 0; i < list.getLength(); i++) {
                    Element n = (Element) list.item(i);

                    String lang = getLangAttribute(n);
                    if (lang == null) {
                        throw new SRUClientException("Element <Description> " +
                                "must have a proper 'xml:lang' attribute");

                    }
                    String desc = cleanString(n.getTextContent());

                    if (descrs == null) {
                        descrs = new HashMap<>();
                    }

                    if (descrs.containsKey(lang)) {
                        logger.warn("A <Description>  with language '{}' " +
                                "already exists", lang);
                    } else {
                        descrs.put(lang, desc);
                    }
                }
                if ((descrs != null) && !descrs.containsKey(LANG_EN)) {
                    logger.warn("A <Description> with language 'en' is mandatory");
                }
            }
            logger.debug("Description: {}", descrs);

            exp = xpath.compile("ed:Institution");
            list = (NodeList) exp.evaluate(node, XPathConstants.NODESET);
            if ((list != null) && (list.getLength() > 0)) {
                for (int i = 0; i < list.getLength(); i++) {
                    Element n = (Element) list.item(i);

                    String lang = getLangAttribute(n);
                    if (lang == null) {
                        throw new SRUClientException("Element <Institution> " +
                                "must have a proper 'xml:lang' attribute");

                    }
                    String inst = cleanString(n.getTextContent());

                    if (insts == null) {
                        insts = new HashMap<>();
                    }

                    if (insts.containsKey(lang)) {
                        logger.warn("An <Institution> with language '{}' " +
                                "already exists", lang);
                    } else {
                        insts.put(lang, inst);
                    }
                }
                if ((insts != null) && !insts.containsKey(LANG_EN)) {
                    logger.warn("An <Institution> with language 'en' is mandatory");
                }
            }
            logger.debug("Institution: {}", insts);

            exp = xpath.compile("ed:LandingPageURI");
            list = (NodeList) exp.evaluate(node, XPathConstants.NODESET);
            if ((list != null) && (list.getLength() > 0)) {
                for (int i = 0; i < list.getLength(); i++) {
                    Element n = (Element) list.item(i);
                    link = cleanString(n.getTextContent());
                }
            }
            logger.debug("LandingPageURI: {}", link);

            exp = xpath.compile("ed:Languages/ed:Language");
            list = (NodeList) exp.evaluate(node, XPathConstants.NODESET);
            if ((list != null) && (list.getLength() > 0)) {
                for (int i = 0; i < list.getLength(); i++) {
                    Element n = (Element) list.item(i);

                    String s = n.getTextContent();
                    if (s != null) {
                        s = s.trim();
                        if (s.isEmpty()) {
                            s = null;
                        }
                    }

                    /*
                     * enforce three letter codes
                     */
                    if ((s == null) || (s.length() != 3)) {
                        throw new SRUClientException("Element <Language> " +
                                "must use ISO-639-3 three letter language codes");
                    }

                    if (langs == null) {
                        langs = new ArrayList<>();
                    }
                    langs.add(s);
                }
            }
            logger.debug("Languages: {}", langs);

            exp = xpath.compile("ed:AvailabilityRestriction");
            list = (NodeList) exp.evaluate(node, XPathConstants.NODESET);
            if ((list != null) && (list.getLength() > 0)) {
                for (int i = 0; i < list.getLength(); i++) {
                    Element n = (Element) list.item(i);
                    String avr = cleanString(n.getTextContent());
                    if (avr != null) {
                        if (AVAILABILITY_RESTRICTION_AUTHONLY.equals(avr)) {
                            availabilityRestriction = AvailabilityRestriction.AUTH_ONLY;
                        } else if (AVAILABILITY_RESTRICTION_PERSONALID.equals(avr)) {
                            availabilityRestriction = AvailabilityRestriction.PERSONAL_IDENTIFIER;
                        } else {
                            throw new SRUClientException(
                                    "invalid availability restriction: " + avr);
                        }
                    }
                    if (!AvailabilityRestriction.NONE.equals(availabilityRestriction) && !hasAuthenticatedSearch) {
                        throw new SRUClientException(
                                    "Resource declares <AvailabilityRestriction>" + 
                                    "but does support 'authenticated-search' (" +
                                    ClarinFCSConstants.CAPABILITY_AUTHENTICATED_SEARCH +
                                    ")!");
                    }
                    // TODO: check if parent also declared restriction and whether they differ -> warn
                }
            }
            logger.debug("AvailabilityRestriction: {}", availabilityRestriction);

            exp = xpath.compile("ed:AvailableDataViews");
            Node n = (Node) exp.evaluate(node, XPathConstants.NODE);
            if ((n != null) && (n instanceof Element)) {
                String ref = getAttribute((Element) n, "ref");
                if (ref == null) {
                    throw new SRUClientException("Element <AvailableDataViews> " +
                                    "must have a 'ref' attribute");
                }
                String[] refs = ref.split("\\s+");
                if ((refs == null) || (refs.length < 1)) {
                    throw new SRUClientException("Attribute 'ref' on element " +
                            "<AvailableDataViews> must contain a whitespace " +
                            "seperated list of data view references");
                }

                for (String ref2 : refs) {
                    DataView dataview = null;
                    for (DataView dv : supportedDataViews) {
                        if (ref2.equals(dv.getIdentifier())) {
                            dataview = dv;
                            break;
                        }
                    }
                    if (dataview != null) {
                        if (availableDataViews == null) {
                            availableDataViews = new ArrayList<>();
                        }
                        availableDataViews.add(dataview);
                    } else {
                        throw new SRUClientException("A data view with " + "identifier '" + ref2 +
                                        "' was not defined in <SupportedDataViews>");
                    }
                }
            } else {
                throw new SRUClientException("Missing element <AvailableDataViews>");
            }
            if (availableDataViews == null) {
                throw new SRUClientException("No available data views were " +
                        "defined for resource with PID '" + pid + "'");
            }
            logger.debug("DataViews: {}", availableDataViews);

            exp = xpath.compile("ed:AvailableLayers");
            n = (Node) exp.evaluate(node, XPathConstants.NODE);
            if ((n != null) && (n instanceof Element)) {
                String ref = getAttribute((Element) n, "ref");
                if (ref == null) {
                    throw new SRUClientException("Element <AvailableLayers> " +
                            "must have a 'ref' attribute");
                }
                String[] refs = ref.split("\\s+");
                if ((refs == null) || (refs.length < 1)) {
                    throw new SRUClientException("Attribute 'ref' on element " +
                            "<AvailableLayers> must contain a whitespace " +
                            "seperated list of layer references");
                }

                for (String ref2 : refs) {
                    Layer layer = null;
                    for (Layer l : supportedLayers) {
                        if (ref2.equals(l.getIdentifier())) {
                            layer = l;
                            break;
                        }
                    }
                    if (layer != null) {
                        if (availableLayers == null) {
                            availableLayers = new ArrayList<>();
                        }
                        availableLayers.add(layer);
                    } else {
                        throw new SRUClientException("A layer with identifier '" + ref2 +
                                "' was not defined " + "in <SupportedLayers>");
                    }
                }
            } else {
                if (hasAdvancedSearch) {
                    logger.debug("No <SupportedLayers> for resource '{}'", pid);
                }
            }
            logger.debug("Layers: {}", availableLayers);

            exp = xpath.compile("ed:AvailableLexFields");
            n = (Node) exp.evaluate(node, XPathConstants.NODE);
            if ((n != null) && (n instanceof Element)) {
                String ref = getAttribute((Element) n, "ref");
                if (ref == null) {
                    throw new SRUClientException("Element <AvailableLexFields> " +
                            "must have a 'ref' attribute");
                }
                String[] refs = ref.split("\\s+");
                if ((refs == null) || (refs.length < 1)) {
                    throw new SRUClientException("Attribute 'ref' on element " +
                            "<AvailableLexFields> must contain a whitespace " +
                            "seperated list of lex field references");
                }

                for (String ref2 : refs) {
                    LexField lexField = null;
                    for (LexField lf : supportedLexFields) {
                        if (ref2.equals(lf.getIdentifier())) {
                            lexField = lf;
                            break;
                        }
                    }
                    if (lexField != null) {
                        if (availableLexFields == null) {
                            availableLexFields = new ArrayList<>();
                        }
                        availableLexFields.add(lexField);
                    } else {
                        throw new SRUClientException("A lex field with identifier '" + 
                                ref2 + "' was not defined " + "in <SupportedLexFields>");
                    }
                }
            } else {
                if (hasLexicalSearch) {
                    logger.debug("No <SupportedLexFields> for resource '{}'", pid);
                }
            }
            logger.debug("LexFields: {}", availableLexFields);

            final int nextDepth = depth + 1;
            if ((maxDepth == INFINITE_MAX_DEPTH) || (nextDepth < maxDepth)) {
                exp = xpath.compile("ed:Resources/ed:Resource");
                list = (NodeList) exp.evaluate(node, XPathConstants.NODESET);
                if ((list != null) && (list.getLength() > 0)) {
                    sub = parseResources(xpath, list, depth + 1, maxDepth, pids, supportedDataViews,
                            supportedLayers, supportedLexFields, version, capabilities);
                }
            }

            // Extensions (skipped) ...

            if (ris == null) {
                ris = new ArrayList<>();
            }
            if ((availableLayers != null) && (version < 1)) {
                logger.warn("Endpoint claims to support FCS 1.0, but " +
                        "includes information about <AvailableLayers> for " +
                        "resource with pid '{}'", pid);
            }
            if ((availableLexFields != null) && (version < 1)) {
                logger.warn("Endpoint claims to support FCS 1.0, but " +
                        "includes information about <AvailableLexFields> for " +
                        "resource with pid '{}'", pid);
            }
            ris.add(new ResourceInfo(pid, titles, descrs, insts, link, langs,
                    availabilityRestriction, availableDataViews,
                    availableLayers, availableLexFields, sub));
        }

        return ris;
    }


    private static String getAttribute(Element el, String localName) {
        String value = el.getAttribute(localName);
        if (value != null) {
            value = value.trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }


    private static String getLangAttribute(Element el) {
        String lang = el.getAttributeNS(XMLConstants.XML_NS_URI, "lang");
        if (lang != null) {
            lang = lang.trim();
            if (!lang.isEmpty()) {
                return lang;
            }
        }
        return null;
    }


    private static String cleanString(String s) {
        if (s != null) {
            s = s.trim();
            if (!s.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String z : s.split("\\s*\\n+\\s*")) {
                    z = z.trim();
                    if (!z.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append(' ');
                        }
                        sb.append(z);
                    }
                }
                if (sb.length() > 0) {
                    return sb.toString();
                }
            }
        }
        return null;
    }


    // -----------------------------------------------------------------------

    @Deprecated
    private static ClarinFCSEndpointDescription parseEndpointDescription(XMLStreamReader reader, int maxDepth)
            throws XMLStreamException, SRUClientException {
        final int version = parseVersion(reader);
        if ((version != VERSION_1) && (version != VERSION_2)) {
            throw new SRUClientException("Attribute 'version' of " +
                    "element '<EndpointDescription>' must be of value '1' or '2'");
        }
        reader.next(); // consume start tag

        // Capabilities
        List<URI> capabilities = null;
        XmlStreamReaderUtils.readStart(reader, ED_NS_URI, "Capabilities", true);
        while (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                "Capability", (capabilities == null))) {
            final String s = XmlStreamReaderUtils.readString(reader, true);
            try {
                if (!s.startsWith(CAPABILITY_PREFIX)) {
                    throw new XMLStreamException("Capabilites must start " +
                            "with prefix '" + CAPABILITY_PREFIX +
                            "' (offending value = '" + s + "')",
                            reader.getLocation());
                }
                final URI uri = new URI(s);
                if (capabilities == null) {
                    capabilities = new ArrayList<>();
                }
                capabilities.add(uri);
                logger.debug("parsed capability:{}", uri);
            } catch (URISyntaxException e) {
                throw new XMLStreamException("Capabilities must be encoded " +
                        "as URIs (offending value = '" + s + "')",
                        reader.getLocation(), e);
            }
            XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "Capability");
        } // while
        XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "Capabilities");

        if (capabilities == null) {
            throw new SRUClientException("Endpoint must support at " +
                    "least one capability!");
        }
        final boolean hasBasicSearch = (capabilities
                .indexOf(ClarinFCSConstants.CAPABILITY_BASIC_SEARCH) != -1);
        final boolean hasAdvancedSearch = (capabilities
                .indexOf(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH) != -1);
        final boolean hasLexicalSearch = (capabilities
                .indexOf(ClarinFCSConstants.CAPABILITY_LEX_SEARCH) != -1);
        final boolean hasAuthenicatedSearch = (capabilities
                .indexOf(ClarinFCSConstants.CAPABILITY_AUTHENTICATED_SEARCH) != -1);
        if (!hasBasicSearch) {
            throw new SRUClientException(
                    "Endpoint must support " + "'basic-search' (" +
                            ClarinFCSConstants.CAPABILITY_BASIC_SEARCH +
                            ") to conform to CLARIN-FCS specification!");
        }

        // SupportedDataViews
        List<DataView> supportedDataViews = null;
        XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                "SupportedDataViews", true);
        while (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                "SupportedDataView", (supportedDataViews == null), true)) {
            final String id = XmlStreamReaderUtils.readAttributeValue(
                    reader, null, "id", true);
            if ((id.indexOf(' ') != -1) || (id.indexOf(',') != -1) ||
                    (id.indexOf(';') != -1)) {
                throw new XMLStreamException("Value of attribute 'id' on " +
                        "element '<SupportedDataView>' may not contain the " +
                        "characters ',' (comma) or ';' (semicolon) " +
                        "or ' ' (space)", reader.getLocation());
            }
            final DeliveryPolicy policy = parsePolicy(reader);
            reader.next(); // consume start tag

            final String type = XmlStreamReaderUtils.readString(reader, true);
            // do some sanity checks ...
            if (supportedDataViews != null) {
                for (DataView dataView : supportedDataViews) {
                    if (dataView.getIdentifier().equals(id)) {
                        throw new XMLStreamException("Supported data view " +
                                "with identifier '" + id +
                                "' was already declared", reader.getLocation());
                    }
                    if (dataView.getMimeType().equals(type)) {
                        throw new XMLStreamException("Supported data view " +
                                "with MIME type '" + type +
                                "' was already declared", reader.getLocation());
                    }
                }
            } else {
                supportedDataViews = new ArrayList<>();
            }
            supportedDataViews.add(new DataView(id, type, policy));
            XmlStreamReaderUtils.readEnd(reader,
                    ED_NS_URI, "SupportedDataView");
        } // while
        XmlStreamReaderUtils.readEnd(reader, ED_NS_URI,
                "SupportedDataViews", true);
        boolean found = false;
        if (supportedDataViews != null) {
            for (DataView dataView : supportedDataViews) {
                if (MIMETYPE_HITS_DATAVIEW.equals(dataView.getMimeType())) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            throw new SRUClientException("Endpoint must support " +
                    "generic hits dataview (expected MIME type '" +
                    MIMETYPE_HITS_DATAVIEW +
                    "') to conform to CLARIN-FCS specification");
        }

        // SupportedLayers
        List<Layer> supportedLayers = null;
        if (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                "SupportedLayers", false)) {
            while (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                    "SupportedLayer", (supportedLayers == null), true)) {
                final String id = XmlStreamReaderUtils.readAttributeValue(reader, null, "id");
                if ((id.indexOf(' ') != -1) || (id.indexOf(',') != -1) ||
                        (id.indexOf(';') != -1)) {
                    throw new XMLStreamException("Value of attribute 'id' on " +
                            "element '<SupportedLayer>' may not contain the " +
                            "characters ',' (comma) or ';' (semicolon) " +
                            "or ' ' (space)", reader.getLocation());
                }
                URI resultId = null;
                final String s1 = XmlStreamReaderUtils.readAttributeValue(reader,
                        null, "result-id");
                try {
                    resultId = new URI(s1);
                } catch (URISyntaxException e) {
                    throw new XMLStreamException("'result-id' must be encoded " +
                            "as URIs (offending value = '" + s1 + "')",
                            reader.getLocation(), e);
                }
                final ContentEncoding encoding = parseContentEncoding(reader);
                String qualifier = XmlStreamReaderUtils.readAttributeValue(reader, null, "qualifier", false);
                String altValueInfo = XmlStreamReaderUtils.readAttributeValue(reader, null, "alt-value-info", false);
                URI altValueInfoURI = null;
                final String s2 = XmlStreamReaderUtils.readAttributeValue(reader, null,
                        "alt-value-info-uri", false);
                if (s2 != null) {
                    try {
                        altValueInfoURI = new URI(s2);
                    } catch (URISyntaxException e) {
                        throw new XMLStreamException("'alt-value-info-uri' must be encoded " +
                                "as URIs (offending value = '" + s2 + "')",
                                reader.getLocation(), e);
                    }
                }
                reader.next(); // consume element
                final String layer = XmlStreamReaderUtils.readString(reader, true);
                logger.debug("layer: id={}, resultId={}, layer={}, encoding={}, qualifier={}, alt-value-info={}, alt-value-info-uri={}",
                        id, resultId, layer, encoding, qualifier, altValueInfo, altValueInfoURI);

                XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "SupportedLayer");
                if (supportedLayers == null) {
                    supportedLayers = new ArrayList<>();
                }
                supportedLayers.add(new Layer(id, resultId, layer,
                        encoding, qualifier, altValueInfo,
                        altValueInfoURI));
            } // while
            XmlStreamReaderUtils.readEnd(reader, ED_NS_URI,
                    "SupportedLayers", true);
        }
        if (hasAdvancedSearch && (supportedLayers == null)) {
            throw new SRUClientException("Endpoint must declare " +
                    "all supported layers (<SupportedLayers>) if they " +
                    "provide the 'advanced-search' (" +
                    ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH +
                    ") capability");
        }
        if (!hasAdvancedSearch && (supportedLayers != null)) {
            // XXX: hard error?!
            logger.warn("Endpoint superflously declared supported " +
                    "layers (<SupportedLayers> without providing the " +
                    "'advanced-search' (" +
                    ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH +
                    ") capability");
        }

        // SupportedLexFields
        List<LexField> supportedLexFields = null;
        if (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                "SupportedLexFields", false)) {
            while (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                    "SupportedLexField", (supportedLayers == null), true)) {
                final String id = XmlStreamReaderUtils.readAttributeValue(reader, null, "id");
                if ((id.indexOf(' ') != -1) || (id.indexOf(',') != -1) ||
                        (id.indexOf(';') != -1)) {
                    throw new XMLStreamException("Value of attribute 'id' on " +
                            "element '<SupportedLexField>' may not contain the " +
                            "characters ',' (comma) or ';' (semicolon) " +
                            "or ' ' (space)", reader.getLocation());
                }
                reader.next(); // consume element
                final String fieldType = XmlStreamReaderUtils.readString(reader, true);
                logger.debug("lex field: id={}, field={}", id, fieldType);

                XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "SupportedLexField");
                if (supportedLexFields == null) {
                    supportedLexFields = new ArrayList<>();
                }
                supportedLexFields.add(new LexField(id, fieldType));
            } // while
            XmlStreamReaderUtils.readEnd(reader, ED_NS_URI,
                    "SupportedLexFields", true);
        }
        if (hasLexicalSearch && (supportedLexFields == null)) {
            throw new SRUClientException("Endpoint must declare " +
                    "all supported lex fields (<SupportedLexFields>) " +
                    "if they provide the 'lex-search' (" +
                    ClarinFCSConstants.CAPABILITY_LEX_SEARCH +
                    ") capability");
        }
        if (!hasLexicalSearch && (supportedLexFields != null)) {
            // XXX: hard error?!
            logger.warn("Endpoint superflously declared supported " +
                    "lex fields (<SupportedLexFields> without providing " +
                    "the 'lex-search' (" +
                    ClarinFCSConstants.CAPABILITY_LEX_SEARCH +
                    ") capability");
        }

        // Resources
        final List<ResourceInfo> resources = parseResources(reader, 0, maxDepth,
                hasAdvancedSearch, hasLexicalSearch, hasAuthenicatedSearch,
                supportedDataViews, supportedLayers, supportedLexFields);

        // skip over extensions
        while (!XmlStreamReaderUtils.peekEnd(reader,
                ED_NS_URI, "EndpointDescription")) {
            if (reader.isStartElement()) {
                final String namespaceURI = reader.getNamespaceURI();
                final String localName    = reader.getLocalName();
                logger.debug("skipping over extension with element {{}}{}",
                        namespaceURI, localName);
                XmlStreamReaderUtils.skipTag(reader, namespaceURI, localName);
            }
        }

        XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "EndpointDescription");

        return new ClarinFCSEndpointDescription(version, capabilities,
                supportedDataViews, supportedLayers, supportedLexFields, resources);
    }


    @Deprecated
    private static List<ResourceInfo> parseResources(XMLStreamReader reader,
            int depth, int maxDepth, boolean hasAdvancedSearch,
            boolean hasLexicalSearch, boolean hasAuthenicatedSearch,
            List<DataView> supportedDataviews, List<Layer> supportedLayers,
            List<LexField> supportedLexFields)
            throws XMLStreamException {
        List<ResourceInfo> resources = null;

        XmlStreamReaderUtils.readStart(reader, ED_NS_URI, "Resources", true);
        while (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                "Resource", (resources == null), true)) {
            final String pid = XmlStreamReaderUtils.readAttributeValue(reader,
                    null, "pid", true);
            reader.next(); // consume start tag

            logger.debug("hasAdvSearch: {}", hasAdvancedSearch);
            logger.debug("hasLexSearch: {}", hasLexicalSearch);
            logger.debug("hasAuthSearch: {}", hasAuthenicatedSearch);

            logger.debug("parsing resource with pid = {}", pid);

            final Map<String, String> title = parseI18String(reader, "Title", true);
            logger.debug("title: {}", title);

            final Map<String, String> description = parseI18String(reader, "Description", false);
            logger.debug("description: {}", description);

            final Map<String, String> institution = parseI18String(reader, "Institution", false);
            logger.debug("institution: {}", institution);

            final String landingPageURI = XmlStreamReaderUtils.readContent(reader, ED_NS_URI,
                    "LandingPageURI", false);
            logger.debug("landingPageURI: {}", landingPageURI);

            List<String> languages = null;
            XmlStreamReaderUtils.readStart(reader,
                    ED_NS_URI, "Languages", true);
            while (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                    "Language", (languages == null))) {
                final String language = XmlStreamReaderUtils.readString(reader, true);
                XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "Language");
                if (languages == null) {
                    languages = new ArrayList<>();
                } else {
                    for (String l : languages) {
                        if (l.equals(language)) {
                            throw new XMLStreamException("language '" +
                                    language + "' was already defined " +
                                    "in '<Language>'", reader.getLocation());
                        }
                    } // for
                }
                languages.add(language);
            } // while
            XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "Languages", true);
            logger.debug("languages: {}", languages);

            AvailabilityRestriction availabilityRestriction = AvailabilityRestriction.NONE;
            if (XmlStreamReaderUtils.readStart(reader, ED_NS_URI, "AvailabilityRestriction", false)) {
                availabilityRestriction = parseAvailabilityRestriction(reader);
                XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "AvailabilityRestriction");
                logger.debug("availability restriction: {}", availabilityRestriction);
            }

            // AvailableDataViews
            XmlStreamReaderUtils.readStart(reader, ED_NS_URI, "AvailableDataViews", true, true);
            final String dvs = XmlStreamReaderUtils.readAttributeValue(reader, null, "ref", true);
            reader.next(); // consume start tag
            XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "AvailableDataViews");
            List<DataView> dataviews = null;
            for (String dv : dvs.split("\\s+")) {
                boolean found = false;
                if (supportedDataviews != null) {
                    for (DataView dataview : supportedDataviews) {
                        if (dataview.getIdentifier().equals(dv)) {
                            found = true;
                            if (dataviews == null) {
                                dataviews = new ArrayList<>();
                            }
                            dataviews.add(dataview);
                            break;
                        }
                    } // for
                }
                if (!found) {
                    throw new XMLStreamException("DataView with id '" + dv +
                            "' was not declared in <SupportedDataViews>",
                            reader.getLocation());
                }
            } // for
            logger.debug("DataViews: {}", dataviews);

            // AvailableLayers
            List<Layer> layers = null;
            if (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                    "AvailableLayers", false, true)) {
                final String ls = XmlStreamReaderUtils.readAttributeValue(reader,
                        null, "ref", true);
                reader.next(); // consume start tag
                XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "AvailableLayers");
                for (String l : ls.split("\\s+")) {
                    boolean found = false;
                    if (supportedLayers != null) {
                        for (Layer layer : supportedLayers) {
                            if (layer.getIdentifier().equals(l)) {
                                found = true;
                                if (layers == null) {
                                    layers = new ArrayList<>();
                                }
                                layers.add(layer);
                                break;
                            }
                        } // for
                    }
                    if (!found) {
                        throw new XMLStreamException("Layer with id '" + l +
                                "' was not declared in <SupportedLayers>",
                                reader.getLocation());
                    }
                } // for
                logger.debug("Layers: {}", layers);
            }
            if (hasAdvancedSearch && (layers == null)) {
                throw new XMLStreamException("Endpoint must declare " +
                        "all available layers (<AvailableLayers>) on a " +
                        "resource, if they provide the 'advanced-search' (" +
                        ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH +
                        ") capability. Offending resource id pid=" + pid +
                        ")", reader.getLocation());
            }

            // AvailableLexFields
            List<LexField> lexFields = null;
            if (XmlStreamReaderUtils.readStart(reader, ED_NS_URI,
                    "AvailableLexFields", false, true)) {
                final String ls = XmlStreamReaderUtils.readAttributeValue(reader,
                        null, "ref", true);
                reader.next(); // consume start tag
                XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "AvailableLexFields");
                for (String l : ls.split("\\s+")) {
                    boolean found = false;
                    if (supportedLexFields != null) {
                        for (LexField lexField : supportedLexFields) {
                            if (lexField.getIdentifier().equals(l)) {
                                found = true;
                                if (lexFields == null) {
                                    lexFields = new ArrayList<>();
                                }
                                lexFields.add(lexField);
                                break;
                            }
                        } // for
                    }
                    if (!found) {
                        throw new XMLStreamException("LexField with id '" + l +
                                "' was not declared in <SupportedLexFields>",
                                reader.getLocation());
                    }
                } // for
                logger.debug("LexFields: {}", layers);
            }
            if (hasLexicalSearch && (layers == null)) {
                throw new XMLStreamException("Endpoint must declare " +
                        "all available lex fields (<AvailableLexFields>) " +
                        "on a resource, if they provide the 'lex-search' (" +
                        ClarinFCSConstants.CAPABILITY_LEX_SEARCH +
                        ") capability. Offending resource id pid=" + pid +
                        ")", reader.getLocation());
            }

            List<ResourceInfo> subResources = null;
            if (XmlStreamReaderUtils.peekStart(reader,
                    ED_NS_URI, "Resources")) {
                final int nextDepth = depth + 1;
                if ((maxDepth == INFINITE_MAX_DEPTH) ||
                        (nextDepth < maxDepth)) {
                    subResources = parseResources(reader, nextDepth,
                            maxDepth, hasAdvancedSearch, hasLexicalSearch,
                            hasAuthenicatedSearch, supportedDataviews,
                            supportedLayers, supportedLexFields);
                } else {
                    XmlStreamReaderUtils.skipTag(reader, ED_NS_URI,
                            "Resources", true);
                }
            }

            while (!XmlStreamReaderUtils.peekEnd(reader,
                    ED_NS_URI, "Resource")) {
                if (reader.isStartElement()) {
                    final String namespaceURI = reader.getNamespaceURI();
                    final String localName    = reader.getLocalName();
                    logger.debug("skipping over extension with element " +
                            "{{}}{} (resource)", namespaceURI, localName);
                    XmlStreamReaderUtils.skipTag(reader,
                            namespaceURI, localName);
                }
            } // while

            XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "Resource");

            if (resources == null) {
                resources = new ArrayList<>();
            }
            resources.add(new ResourceInfo(pid, title, description,
                    institution, landingPageURI, languages,
                    availabilityRestriction, dataviews, layers, lexFields,
                    subResources));
        } // while
        XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, "Resources");

        return resources;
    }


    @Deprecated
    private static Map<String, String> parseI18String(XMLStreamReader reader,
            String localName, boolean required) throws XMLStreamException {
        Map<String, String> result = null;
        while (XmlStreamReaderUtils.readStart(reader, ED_NS_URI, localName,
                ((result == null) && required), true)) {
            final String lang = XmlStreamReaderUtils.readAttributeValue(reader,
                    XMLConstants.XML_NS_URI, "lang", true);
            reader.next(); // skip start tag
            final String content = XmlStreamReaderUtils.readString(reader, true);
            if (result == null) {
                result = new HashMap<>();
            }
            if (result.containsKey(lang)) {
                throw new XMLStreamException("language '" + lang +
                        "' already defined for element '<" + localName + ">'",
                        reader.getLocation());
            } else {
                result.put(lang, content);
            }
            XmlStreamReaderUtils.readEnd(reader, ED_NS_URI, localName);
        } // while
        return result;
    }


    @Deprecated
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


    @Deprecated
    private static DeliveryPolicy parsePolicy(XMLStreamReader reader)
            throws XMLStreamException {
        final String s = XmlStreamReaderUtils.readAttributeValue(reader,
                null, "delivery-policy", true);
        if (POLICY_SEND_DEFAULT.equals(s)) {
            return DeliveryPolicy.SEND_BY_DEFAULT;
        } else if (POLICY_NEED_REQUEST.equals(s)) {
            return DeliveryPolicy.NEED_TO_REQUEST;
        } else {
            throw new XMLStreamException("Unexpected value '" + s +
                    "' for attribute 'delivery-policy' on " +
                    "element '<SupportedDataView>'", reader.getLocation());
        }
    }


    @Deprecated
    private static ContentEncoding parseContentEncoding(XMLStreamReader reader)
            throws XMLStreamException {
        final String s = XmlStreamReaderUtils.readAttributeValue(reader,
                null, "encoding", false);
        if (s != null) {
            if (LAYER_ENCODING_VALUE.equals(s)) {
                return ContentEncoding.VALUE;
            } else if (POLICY_NEED_REQUEST.equals(s)) {
                return ContentEncoding.EMPTY;
            } else {
                throw new XMLStreamException("Unexpected value '" + s +
                                "' for attribute 'encoding' on " +
                                "element '<SupportedLayer>'",
                        reader.getLocation());
            }
        } else {
            return null;
        }
    }


    @Deprecated
    private static AvailabilityRestriction parseAvailabilityRestriction(XMLStreamReader reader)
            throws XMLStreamException {
        final String s = XmlStreamReaderUtils.readString(reader, true);
        if (AVAILABILITY_RESTRICTION_AUTHONLY.equals(s)) {
            return AvailabilityRestriction.AUTH_ONLY;
        } else if (AVAILABILITY_RESTRICTION_PERSONALID.equals(s)) {
            return AvailabilityRestriction.PERSONAL_IDENTIFIER;
        } else {
            throw new XMLStreamException("Unexpected value '" + s +
                    "' for content in element '<AvailabilityRestriction>'",
                    reader.getLocation());
        }
    }

} // class ClarinFCSEndpointDescriptionParser
