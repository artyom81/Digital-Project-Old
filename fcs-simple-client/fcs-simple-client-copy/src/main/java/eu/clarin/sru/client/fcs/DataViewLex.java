package eu.clarin.sru.client.fcs;

import java.util.Collections;
import java.util.List;

/**
 * A Data View implementation that stores the content of a Lex Data View.
 */
public class DataViewLex extends DataView {
    /**
     * The MIME type for CLARIN-FCS Lexical data views.
     */
    public static final String TYPE = "application/x-clarin-fcs-lex+xml";

    private final List<Field> fields;
    private final String xmlLang;
    private final String langUri;

    protected DataViewLex(String pid, String ref, List<Field> fields, String xmlLang, String langUri) {
        super(TYPE, pid, ref);

        if (fields == null) {
            throw new NullPointerException("fields == null");
        }
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("fields is empty");
        }
        this.fields = Collections.unmodifiableList(fields);

        // all other optional attributes
        this.xmlLang = xmlLang;
        this.langUri = langUri;
    }

    protected DataViewLex(String pid, String ref, List<Field> fields) {
        this(pid, ref, fields, null, null);
    }

    public List<Field> getFields() {
        return fields;
    }

    public String getXmlLang() {
        return xmlLang;
    }

    public String getLangUri() {
        return langUri;
    }

    /**
     * All valid <code>type</code> values for the <code>&lt;Field/&gt;</code>
     * element of an Lex Data View <code>&lt;Entry/&gt;</code>.
     */
    public enum FieldType {
        ENTRY_ID("entryId"),
        // lemma
        LEMMA("lemma"),
        TRANSLATION("translation"),
        TRANSCRIPTION("transcription"),
        PHONETIC("phonetic"),
        // prosaic descriptions
        DEFINITION("definition"),
        ETYMOLOGY("etymology"),
        // grammar/morphology
        CASE("case"),
        NUMBER("number"),
        GENDER("gender"),
        POS("pos"),
        BASEFORM("baseform"),
        SEGMENTATION("segmentation"),
        // numeric stuff
        SENTIMENT("sentiment"),
        FREQUENCY("frequency"),
        // relations
        ANTONYM("antonym"),
        HYPONYM("hyponym"),
        HYPERNYM("hypernym"),
        MERONYM("meronym"),
        HOLONYM("holonym"),
        SYNONYM("synonym"),
        // SUBORDINATE("subordinate"),
        // SUPERORDINATE("superordinate"),
        RELATED("related"),
        // references
        REF("ref"),
        SENSEREF("senseRef"),
        // citations/quotations
        CITATION("citation");

        private String type;

        FieldType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        /**
         * Retrive the enum value for a given type string. Is case-sensitive.
         * 
         * @param type string type value of an enum value
         * @return corresponding enum value
         * @throws IllegalArgumentException if <code>type</code> parameter does not
         *                                  specify a known enum value
         */
        public static FieldType fromString(String type) {
            if (type == null) {
                throw new NullPointerException("type == null");
            }
            for (FieldType ft : FieldType.values()) {
                if (ft.type.equals(type)) {
                    return ft;
                }
            }
            throw new IllegalArgumentException("No enum constant with type '" + type + "'!");
        }
    }

    /**
     * All valid virtual <code>type</code> values for the
     * <code>&lt;Entry/&gt;</code> element of an Lex Data View.
     */
    public enum VirtualFieldType {
        LANG("lang");

        private String type;

        VirtualFieldType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        /**
         * Retrive the enum value for a given type string. Is case-sensitive.
         * 
         * @param type string type value of an enum value
         * @return corresponding enum value
         * @throws IllegalArgumentException if <code>type</code> parameter does not
         *                                  specify a known enum value
         */
        public static VirtualFieldType fromString(String type) {
            if (type == null) {
                throw new NullPointerException("type == null");
            }
            for (VirtualFieldType ft : VirtualFieldType.values()) {
                if (ft.type.equals(type)) {
                    return ft;
                }
            }
            throw new IllegalArgumentException("No enum constant with type '" + type + "'!");
        }
    }

    /**
     * Container class for <code>&lt;Field/&gt;</code> data.
     */
    public static final class Field {
        private final FieldType type;
        private final List<Value> values;

        protected Field(FieldType type, List<Value> values) {
            if (type == null) {
                throw new NullPointerException("type == null");
            }
            this.type = type;
            if (values == null) {
                throw new NullPointerException("values == null");
            }
            if (values.isEmpty()) {
                throw new IllegalArgumentException("values is empty");
            }
            this.values = Collections.unmodifiableList(values);
        }

        public FieldType getType() {
            return type;
        }

        public List<Value> getValues() {
            return values;
        }
    }

    /**
     * Container class for <code>&lt;Value/&gt;</code> data.
     */
    public static final class Value {
        private final String value;

        private final String xmlId;
        private final String xmlLang;
        private final String langUri;
        private final boolean preferred;
        private final String ref;
        private final List<String> idRefs;
        private final String vocabRef;
        private final String vocabValueRef;
        private final String type;
        // citation
        private final String source;
        private final String sourceRef;
        private final String date;

        protected Value(String value, String xmlId, String xmlLang, String langUri, boolean preferred, String ref,
                List<String> idRefs, String vocabRef, String vocabValueRef, String type, String source,
                String sourceRef, String date) {
            // NOTE: this might be intentionally empty
            // if (value == null) {
            //     throw new NullPointerException("value == null");
            // }

            this.value = value;
            // all other optional attributes
            this.xmlId = xmlId;
            this.xmlLang = xmlLang;
            this.langUri = langUri;
            this.preferred = preferred;
            this.ref = ref;
            if (idRefs == null) {
                this.idRefs = Collections.emptyList();
            } else {
                this.idRefs = Collections.unmodifiableList(idRefs);
            }
            this.vocabRef = vocabRef;
            this.vocabValueRef = vocabValueRef;
            this.type = type;
            this.source = source;
            this.sourceRef = sourceRef;
            this.date = date;
        }

        protected Value(String value) {
            this(value, null, null, null, false, null, null, null, null, null, null, null, null);
        }

        protected Value(String value, String xmlLang) {
            this(value, null, xmlLang, null, false, null, null, null, null, null, null, null, null);
        }

        protected Value(String value, String xmlLang, boolean preferred) {
            this(value, null, xmlLang, null, preferred, null, null, null, null, null, null, null, null);
        }

        public String getValue() {
            return value;
        }

        public String getXmlId() {
            return xmlId;
        }

        public String getXmlLang() {
            return xmlLang;
        }

        public String getLangUri() {
            return langUri;
        }

        public boolean isPreferred() {
            return preferred;
        }

        public String getRef() {
            return ref;
        }

        public List<String> getIdRefs() {
            return idRefs;
        }

        public String getVocabRef() {
            return vocabRef;
        }

        public String getVocabValueRef() {
            return vocabValueRef;
        }

        public String getType() {
            return type;
        }

        public String getSource() {
            return source;
        }

        public String getSourceRef() {
            return sourceRef;
        }

        public String getDate() {
            return date;
        }
    }
}
