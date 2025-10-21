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

/**
 * Base class for Data View implementations according to the CLARIN-FCS record
 * schema.
 */
public abstract class DataView {
    private final String type;
    private final String pid;
    private final String ref;


    /**
     * Constructor.
     *
     * @param type
     *            the MIME type of this DataView
     * @param pid
     *            a persistent identifier or <code>null</code>
     * @param ref
     *            a reference URI or <code>null</code>
     * @throws NullPointerException
     *             if a mandatory argument was not supplied
     *
     */
    protected DataView(String type, String pid, String ref) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        this.type = type;
        this.pid  = ((pid != null) && !pid.isEmpty()) ? pid : null;
        this.ref  = ((ref != null) && !ref.isEmpty()) ? ref : null;
    }


    /**
     * Get the MIME type of this DataView.
     *
     * @return the MIME type of this DataView
     */
    public String getMimeType() {
        return type;
    }


    /**
     * Convenience method to check if this DataView is of a certain MIME type.
     *
     * @param type
     *            the MIME type to test against
     * @return <code>true</code> if the DataView is in the supplied MIME type,
     *         <code>false</code> otherwise
     * @throws NullPointerException
     *             if any required arguments are not supplied
     */
    public boolean isMimeType(String type) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        return (this.type.equals(type));
    }


    /**
     * Get the persistent identifier for this DataView.
     *
     * @return a persistent identifier or <code>null</code> of this DataView has
     *         none
     */
    public String getPid() {
        return pid;
    }


    /**
     * Get the reference URI for this DataView.
     *
     * @return a reference URI or <code>null</code> of this DataView has
     *         none
     */
    public String getRef() {
        return ref;
    }

} // abstract class DataView
