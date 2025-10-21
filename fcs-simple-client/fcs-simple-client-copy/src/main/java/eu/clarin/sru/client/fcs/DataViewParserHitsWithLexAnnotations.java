package eu.clarin.sru.client.fcs;

import java.util.Arrays;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.XmlStreamReaderUtils;

/**
 * An implementation of a Data View parser that parses HITS Data Views with
 * optional <code>kind</code> attributes. This parser expects input that
 * conforms to the CLARIN-FCS specification for the HITS Data View except that
 * <code>&lt;Hit/&gt;</code> elements may have an optional <code>kind</code>
 * attribute.
 * 
 * If both the {@link DataViewParserHits} and
 * {@link DataViewParserHitsWithLexAnnotations} are used, the latter has
 * priority. The {@link DataViewHitsWithLexAnnotations} is backwards compatible
 * to {@link DataViewHits}.
 *
 * @see DataViewHits
 * @see DataViewParserHits
 * @see DataViewHitsWithLexAnnotations
 */
public final class DataViewParserHitsWithLexAnnotations implements DataViewParser {
    private static final int OFFSET_CHUNK_SIZE = 8;
    private static final String FCS_HITS_NS = "http://clarin.eu/fcs/dataview/hits";
    private static final Logger logger = LoggerFactory.getLogger(DataViewParserHitsWithLexAnnotations.class);

    @Override
    public boolean acceptType(String type) {
        return DataViewHitsWithLexAnnotations.TYPE.equals(type);
    }

    @Override
    public int getPriority() {
        return 1010;
    }

    @Override
    public DataView parse(XMLStreamReader reader, String type, String pid,
            String ref) throws XMLStreamException, SRUClientException {
        int offsets[] = new int[OFFSET_CHUNK_SIZE];
        String hitKinds[] = new String[OFFSET_CHUNK_SIZE];
        int offsets_idx = 0;
        int hitKinds_idx = 0;
        StringBuilder buffer = new StringBuilder();
        XmlStreamReaderUtils.readStart(reader, FCS_HITS_NS, "Result", true);

        int idx = 0;
        while (!XmlStreamReaderUtils.peekEnd(reader, FCS_HITS_NS, "Result")) {
            if (buffer.length() > 0) {
                if (!Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {
                    buffer.append(' ');
                }
                idx = buffer.length();
            }

            if (XmlStreamReaderUtils.readStart(reader, FCS_HITS_NS, "Hit", false, true)) {
                String hitKind = XmlStreamReaderUtils.readAttributeValue(reader, null, "kind");
                reader.next(); // skip start element

                String hit = XmlStreamReaderUtils.readString(reader, false);
                XmlStreamReaderUtils.readEnd(reader, FCS_HITS_NS, "Hit");
                if (hit.length() > 0) {
                    buffer.append(hit);
                    if (offsets_idx == offsets.length) {
                        offsets = Arrays.copyOf(offsets, offsets.length + 8);
                    }
                    if (hitKinds_idx == hitKinds.length) {
                        hitKinds = Arrays.copyOf(hitKinds, hitKinds.length + 8);
                    }
                    /*
                     * add pair of offsets and simultaneously increase index
                     */
                    offsets[offsets_idx++] = idx;
                    offsets[offsets_idx++] = idx + hit.length();
                    hitKinds[hitKinds_idx++] = hitKind;
                } else {
                    logger.warn("skipping empty <Hit> element within <Result> element");
                }
            } else {
                buffer.append(XmlStreamReaderUtils.readString(reader, false));
            }
        } // while
        XmlStreamReaderUtils.readEnd(reader, FCS_HITS_NS, "Result");

        final String text = buffer.toString();
        return new DataViewHitsWithLexAnnotations(pid, ref, text, offsets, offsets_idx, hitKinds);
    }
}
