package eu.clarin.sru.client.fcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.XmlStreamReaderUtils;

/**
 * An implementation of a Data View parser that parses Lex Data Views. This
 * parser expects input that conforms to the CLARIN-FCS specification for the
 * Lex Data View.
 *
 * @see DataViewLex
 */
public final class DataViewParserLex implements DataViewParser {
    private static final String FCS_LEX_NS = "http://clarin.eu/fcs/dataview/lex";

    private static final Logger logger = LoggerFactory.getLogger(DataViewParserLex.class);

    @Override
    public boolean acceptType(String type) {
        return DataViewLex.TYPE.equals(type);
    }

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public DataView parse(XMLStreamReader reader, String type, String pid, String ref)
            throws XMLStreamException, SRUClientException {
        XmlStreamReaderUtils.readStart(reader, FCS_LEX_NS, "Entry", true, true);
        final String entryXmlLang = XmlStreamReaderUtils.readAttributeValue(reader, XMLConstants.XML_NS_URI, "lang");
        final String entryLangUri = XmlStreamReaderUtils.readAttributeValue(reader, null, "langUri");
        reader.next(); // skip start element

        logger.debug("entry: xml:lang={}, langUri={}", entryXmlLang, entryLangUri);

        final Set<String> valueIds = new HashSet<>();
        final Map<String, Location> idRefsLocations = new HashMap<>();

        final List<DataViewLex.Field> fields = new ArrayList<>();
        while (XmlStreamReaderUtils.readStart(reader, FCS_LEX_NS, "Field", fields.isEmpty(), true)) {
            final String fieldTypeRaw = XmlStreamReaderUtils.readAttributeValue(reader, null, "type", true);
            final DataViewLex.FieldType fieldType;
            try {
                fieldType = DataViewLex.FieldType.fromString(fieldTypeRaw);
            } catch (IllegalArgumentException iae) {
                throw new XMLStreamException("Field specified unknown type: '" + fieldTypeRaw + "'",
                        reader.getLocation(), iae);
            }
            reader.next(); // skip start element

            logger.debug("field: type={}", fieldType);

            final List<DataViewLex.Value> values = new ArrayList<>();
            while (XmlStreamReaderUtils.readStart(reader, FCS_LEX_NS, "Value", values.isEmpty(), true)) {
                final String xmlLang = XmlStreamReaderUtils.readAttributeValue(reader, XMLConstants.XML_NS_URI, "lang");
                final String xmlId = XmlStreamReaderUtils.readAttributeValue(reader, XMLConstants.XML_NS_URI, "id");
                if (xmlId != null) {
                    valueIds.add(xmlId);
                }

                final String langUri = XmlStreamReaderUtils.readAttributeValue(reader, null, "langUri");
                final boolean preferred = Boolean
                        .parseBoolean(XmlStreamReaderUtils.readAttributeValue(reader, null, "preferred"));
                final String refRaw = XmlStreamReaderUtils.readAttributeValue(reader, null, "ref");
                final String idRefsRaw = XmlStreamReaderUtils.readAttributeValue(reader, null, "idRefs");
                List<String> idRefs = null;
                if (idRefsRaw != null) {
                    idRefs = Arrays.asList(idRefsRaw.trim().replaceAll("\\s+", " ").split(" "));
                    Location idRefsLocation = reader.getLocation();
                    for (String idRef : idRefs) {
                        idRefsLocations.put(idRef, idRefsLocation);
                    }
                }
                final String vocabRef = XmlStreamReaderUtils.readAttributeValue(reader, null, "vocabRef");
                final String vocabValueRef = XmlStreamReaderUtils.readAttributeValue(reader, null, "vocabValueRef");
                final String typeRaw = XmlStreamReaderUtils.readAttributeValue(reader, null, "type");
                final String source = XmlStreamReaderUtils.readAttributeValue(reader, null, "source");
                final String sourceRef = XmlStreamReaderUtils.readAttributeValue(reader, null, "sourceRef");
                final String date = XmlStreamReaderUtils.readAttributeValue(reader, null, "date");

                if (langUri != null && xmlLang == null) {
                    throw new XMLStreamException("Value with langUri attribute requires a xml:lang attribute",
                            reader.getLocation());
                }

                // check if any attributes are set
                boolean hasAttributes = false;
                hasAttributes |= xmlLang != null;
                hasAttributes |= xmlId != null;
                hasAttributes |= langUri != null;
                hasAttributes |= preferred;
                hasAttributes |= refRaw != null;
                hasAttributes |= idRefs != null && !idRefs.isEmpty();
                hasAttributes |= vocabRef != null;
                hasAttributes |= vocabValueRef != null;
                hasAttributes |= typeRaw != null;
                hasAttributes |= source != null;
                hasAttributes |= sourceRef != null;
                hasAttributes |= date != null;

                reader.next(); // skip start element

                String content = XmlStreamReaderUtils.readString(reader, false);
                XmlStreamReaderUtils.readEnd(reader, FCS_LEX_NS, "Value");

                logger.debug("value: content='{}', xml:id={}, xml:lang={}, langUri={}, "
                        + "preferred={}, ref={}, idRefs={}, vocabRef={}, vocabValueRef={}, type={}, "
                        + "source={}, sourceRef={}, date={}",
                        content, xmlId, xmlLang, langUri, preferred, refRaw, idRefs, vocabRef, vocabValueRef,
                        typeRaw, source, sourceRef, date);
                if (content == null) {
                    if (hasAttributes) {
                        logger.warn("value has no content but specifies attributes!");
                    } else {
                        logger.error("value has no content and no attributes set! Skip.");
                        continue;
                    }
                }

                DataViewLex.Value value = new DataViewLex.Value(content, xmlId, xmlLang, langUri, preferred,
                        refRaw, idRefs, vocabRef, vocabValueRef, typeRaw, source, sourceRef, date);
                values.add(value);
            }

            XmlStreamReaderUtils.readEnd(reader, FCS_LEX_NS, "Field");

            DataViewLex.Field field = new DataViewLex.Field(fieldType, values);
            fields.add(field);
        } // while

        XmlStreamReaderUtils.readEnd(reader, FCS_LEX_NS, "Entry");

        // check if id/idRefs valid
        for (Map.Entry<String, Location> entry : idRefsLocations.entrySet()) {
            if (!valueIds.contains(entry.getKey())) {
                throw new XMLStreamException("No value with id '" + entry.getKey() + "' found", entry.getValue());
            }
        }

        return new DataViewLex(pid, ref, fields, entryXmlLang, entryLangUri);
    }
}
