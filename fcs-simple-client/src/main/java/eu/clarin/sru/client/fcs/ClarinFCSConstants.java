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

import eu.clarin.sru.client.SRUClientConstants;

public final class ClarinFCSConstants {

    /** constant for FCS-QL query type, advanced search */
    public static final String QUERY_TYPE_FCS = "fcs";


    /** constant for CQL query type, basic search */
    public static final String QUERY_TYPE_CQL =
            SRUClientConstants.QUERY_TYPE_CQL;


    /** constant for LexCQL query type, lexical search */
    public static final String QUERY_TYPE_LEX = "lex";


    /**
     * constant for extra request parameter "x-fcs-endpoint-description"
     */
    public static final String X_FCS_ENDPOINT_DESCRIPTION  =
            "x-fcs-endpoint-description";


    /**
     * constant for Basic Search capability
     */
    public static final URI CAPABILITY_BASIC_SEARCH =
            URI.create("http://clarin.eu/fcs/capability/basic-search");


    /**
     * constant for Advanced Search capability
     */
    public static final URI CAPABILITY_ADVANCED_SEARCH =
            URI.create("http://clarin.eu/fcs/capability/advanced-search");


    /**
     * constant for Lexical Search capability
     */
    public static final URI CAPABILITY_LEX_SEARCH =
            URI.create("http://clarin.eu/fcs/capability/lex-search");


    /**
     * constant for Authenticated Search capability
     */
    public static final URI CAPABILITY_AUTHENTICATED_SEARCH =
            URI.create("http://clarin.eu/fcs/capability/authenticated-search");


    /**
     * constant for value "true" for extra request parameter
     * "x-fcs-endpoint-description"
     */
    public static final String TRUE = "true";


    /**
     * constant for extra request parameter "x-clarin-resource-info"
     */
    public static final String LEGACY_X_CLARIN_RESOURCE_INFO =
            "x-clarin-resource-info";


    /**
     * constant for extra request parameter "x-unlimited-resultset" (NB: only
     * applicable for SRUServer implementation)
     */
    public static final String X_UNLIMITED_RESULTSET =
            SRUClientConstants.X_UNLIMITED_RESULTSET;

    /**
     * constant for extra request parameter "x-unlimited-termlist" (NB: only
     * applicable for SRUServer implementation)
     */
    public static final String X_UNLIMITED_TERMLIST =
            SRUClientConstants.X_UNLIMITED_TERMLIST;

    /**
     * constant for extra request parameter "x-indent-response" (NB: only
     * applicable for SRUServer implementation)
     */
    public static final String X_INDENT_RESPONSE =
            SRUClientConstants.X_INDENT_RESPONSE;

    private static final String FCS_DIAGNOSTIC_PREFIX =
            "http://clarin.eu/fcs/diagnostic/";

    /**
     * constant for FCS diagnostic
     * "Persistent identifier passed by the Client for restricting the search is invalid."
     */
    public static final String FCS_INVALID_PERSISTENT_IDENTIFIER =
            FCS_DIAGNOSTIC_PREFIX + 1;

    /**
     * constant for FCS diagnostic
     * "Resource set too large. Query context automatically adjusted."
     */
    public static final String FCS_RESOURCE_TOO_LARGE_QUERY_ADJUSTED =
            FCS_DIAGNOSTIC_PREFIX + 2;

    /**
     * constant for FCS diagnostic
     * "Resource set too large. Cannot perform query."
     */
    public static final String FCS_RESOURCE_TOO_LARGE_CANNOT_PERFORM_QUERY =
            FCS_DIAGNOSTIC_PREFIX + 3;

    /**
     * constant for FCS diagnostic
     * "Requested Data View not valid for this resource."
     */
    public static final String FCS_REQUESTED_DATAVIEW_NOT_VALID_FOR_RESOURCE =
            FCS_DIAGNOSTIC_PREFIX + 4;

    /**
     * constant for FCS diagnostic
     * "General query syntax error."
     */
    public static final String FCS_GENERAL_QUERY_SYNTAX_ERROR =
            FCS_DIAGNOSTIC_PREFIX + 10;

    /**
     * constant for FCS diagnostic
     * "Query too complex. Cannot perform Query."
     */
    public static final String FCS_QUERY_TOO_COMPLEX =
            FCS_DIAGNOSTIC_PREFIX + 11;

    /**
     * constant for FCS diagnostic "Query was rewritten."
     */
    public static final String FCS_QUERY_WAS_REWRITTEN =
            FCS_DIAGNOSTIC_PREFIX + 12;

    /**
     * constant for FCS diagnostic "General processing hint."
     */
    public static final String FCS_GENERAL_PROCESSING_HINT =
            FCS_DIAGNOSTIC_PREFIX + 14;


    /* hide constructor */
    private ClarinFCSConstants() {
    }

} // class ClarinFCSConstants
