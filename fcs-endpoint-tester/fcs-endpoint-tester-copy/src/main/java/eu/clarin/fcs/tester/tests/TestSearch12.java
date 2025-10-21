/**
 * This software is copyright (c) 2013 by
 *  - Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 * This is free software. You can redistribute it
 * and/or modify it under the terms described in
 * the GNU General Public License v3 of which you
 * should have received a copy. Otherwise you can download
 * it from
 *
 *   http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * @copyright Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package eu.clarin.fcs.tester.tests;

import eu.clarin.fcs.tester.FCSTest;
import eu.clarin.fcs.tester.FCSTestCase;
import eu.clarin.fcs.tester.FCSTestContext;
import eu.clarin.fcs.tester.FCSTestProfile;
import eu.clarin.fcs.tester.FCSTestResult;
import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUClientConstants;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUDiagnostic;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.fcs.DataView;
import eu.clarin.sru.client.fcs.LegacyClarinFCSRecordData;
import eu.clarin.sru.client.fcs.Resource;
import eu.clarin.sru.client.fcs.Resource.ResourceFragment;

@FCSTestCase(priority=3600, profiles = {
        FCSTestProfile.CLARIN_FCS_LEGACY
})
@SuppressWarnings("deprecation")
public class TestSearch12 extends FCSTest {
    private static final String FCS_RECORD_SCHEMA = "http://clarin.eu/fcs/1.0";
    private static final String MIME_TYPE_KWIC =
            "application/x-clarin-fcs-kwic+xml";


    @Override
    public String getName() {
        return "SearchRetrieve";
    }


    @Override
    public String getDescription() {
        return "Search for user specified search term and " +
                "requesting endpoint to return results in CLARIN-FCS " +
                "record schema '" + FCS_RECORD_SCHEMA + "'";
    }


    @Override
    public String getExpected() {
        return "Expecting at least one record in CLARIN-FCS legacy record " +
                "schema (without any surrogate diagnostics)";
    }


    @Override
    public FCSTestResult perform(FCSTestContext context, SRUClient client)
            throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL,
                escapeCQL(context.getUserSearchTerm()));
        req.setRecordSchema(FCS_RECORD_SCHEMA);
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = client.searchRetrieve(req);
        if (findDiagnostic(res, "info:srw/diagnostic/1/66")) {
            return makeError("Endpoint claims to not " +
                    "support FCS record schema (" + FCS_RECORD_SCHEMA + ")");
        } else if (res.getRecordsCount() == 0) {
            return makeWarning("Endpoint has no results " +"" +
                    "for search term \"" + context.getUserSearchTerm() +
                    "\". Please supply a different search term.");
        } else {
            for (SRURecord record : res.getRecords()) {
                if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
                    final SRUSurrogateRecordData data =
                            (SRUSurrogateRecordData) record.getRecordData();
                    final SRUDiagnostic d = data.getDiagnostic();

                    if (isDiagnostic(d, "info:srw/diagnostic/1/67")) {
                        return makeError("Endpoint cannot render record in " +
                                "CLARIN-FCS record format and returned " +
                                "surrogate diagnostic \"info:srw/diagnostic/1/67\" " +
                                "instead.");
                    } else if (isDiagnostic(d, "info:clarin/sru/diagnostic/2")) {
                        return makeError("Endpoint sent one or more records with record " +
                                        "schema of '" + d.getDetails() +
                                        "' instead of '" + FCS_RECORD_SCHEMA +
                                        "'.");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Endpoint returned unexpected surrgogate ")
                                .append("diagnostic \"")
                                .append(d.getURI()).append("\"");
                        if (d.getDetails() != null) {
                            sb.append("; details = \"")
                                .append(d.getDetails()).append("\"");
                        }
                        if (d.getMessage() != null) {
                            sb.append("; message = \"")
                                .append(d.getMessage()).append("\"");
                        }
                        return makeError(sb.toString());
                    }
                } else if (record.isRecordSchema(FCS_RECORD_SCHEMA)) {
                    final LegacyClarinFCSRecordData data =
                            (LegacyClarinFCSRecordData) record.getRecordData();
                    final Resource resource = data.getResource();
                    boolean foundKwic = false;
                    if (resource.hasDataViews()) {
                        for (DataView dataView : resource.getDataViews()) {
                            if (dataView.isMimeType(MIME_TYPE_KWIC)) {
                                foundKwic = true;
                            }
                        }
                    }

                    if (resource.hasResourceFragments()) {
                        for (ResourceFragment fragment : resource.getResourceFragments()) {
                            for (DataView dataView : fragment.getDataViews()) {
                                if (dataView.isMimeType(MIME_TYPE_KWIC)) {
                                    foundKwic = true;
                                }
                            }
                        }
                    }
                    if (!foundKwic) {
                        return makeError("Endpoint did not provide mandatory KWIC dataview in results");
                    }

                } else {
                    return makeError("Endpoint does not supply results in FCS record schema (" + FCS_RECORD_SCHEMA + "\"");
                }
            }
            if (res.getRecordsCount() > req.getMaximumRecords()) {
                return makeError("Endpoint did not honor upper requested limit for " +
                                "\"maximumRecords\" parameter (up to " +
                                req.getMaximumRecords() +
                                " records where requested and endpoint delivered " +
                                res.getRecordsCount() + " results)");
            }
            return makeSuccess();
        }
    }

}
