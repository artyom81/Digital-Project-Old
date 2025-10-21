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
import java.util.Collections;
import java.util.List;

/**
 * A Data View implementation that stores the content of a Advanced Data View.
 */
public class DataViewAdvanced extends DataView {
    /**
     * The MIME type for CLARIN-FCS Advanced data views.
     */
    public static final String TYPE = "application/x-clarin-fcs-adv+xml";
    private final List<Layer> layers;
    private final Unit unit;


    /**
     * Constructor.
     *
     * @param pid
     *            a persistent identifier or <code>null</code>
     * @param ref
     *            a reference URI or <code>null</code>
     * @param unit
     *            the unit for offsets in this Data View
     * @param layers
     *            the list of layers in this Data View
     * @throws NullPointerException
     *             if any mandatory argument is <code>null</code>
     * @throws IllegalArgumentException
     *             if any argument is illegal
     */
    protected DataViewAdvanced(String pid, String ref, Unit unit, List<Layer> layers) {
        super(TYPE, pid, ref);
        if (unit == null) {
            throw new NullPointerException("unit == null");
        }
        this.unit = unit;
        if (layers == null) {
            throw new NullPointerException("layers == null");
        }
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("layers is empty");
        }
        this.layers = Collections.unmodifiableList(layers);
    }


    public Unit getUnit() {
        return unit;
    }


    public List<Layer> getLayers() {
        return layers;
    }

    public enum Unit {
        ITEM, TIMESTAMP
    }


    public static final class Segment {
        private final String id;
        private final long start;
        private final long end;
        private URI reference;

        protected Segment(String id, long start, long end, URI reference) {
            if (id == null) {
                throw new NullPointerException("id == null");
            }
            this.id = id;
            this.start = start;
            this.end = end;
            this.reference = reference;
        }


        public String getId() {
            return id;
        }


        public long getStartOffset() {
            return start;
        }


        public long getEndOffset() {
            return end;
        }


        public URI getReference() {
            return reference;
        }
    }

    public static final class Layer {
        private final String id;
        private final List<Span> spans;

        protected Layer(String id, List<Span> spans) {
            if (id == null) {
                throw new NullPointerException("id == null");
            }
            this.id = id;
            if (spans == null) {
                throw new NullPointerException("spans == null");
            }
            if (spans.isEmpty()) {
                throw new IllegalArgumentException("spans is empty");
            }
            this.spans = Collections.unmodifiableList(spans);
        }


        public String getId() {
            return id;
        }


        public List<Span> getSpans() {
            return this.spans;
        }
    }

    public static final class Span {
        private final Segment segment;
        private final String highlight;
        private final String altValue;
        private final String content;


        protected Span(Segment segment, String highlight, String altValue,
                String content) {
            if (segment == null) {
                throw new NullPointerException("segment == null");
            }
            this.segment = segment;
            this.highlight = highlight;
            this.altValue = altValue;
            this.content = content;
        }


        public Segment getSegment() {
            return segment;
        }


        public String getHighlight() {
            return highlight;
        }


        public String getAltValue() {
            return altValue;
        }


        public String getContent() {
            return content;
        }


        public String getSegmentId() {
            return segment.getId();
        }


        public long getStartOffset() {
            return segment.getStartOffset();
        }


        public long getEndOffset() {
            return segment.getEndOffset();
        }


        public URI getReference() {
            return segment.getReference();
        }
    }

} // class DataViewAdvanced
