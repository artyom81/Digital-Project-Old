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
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;

@FCSTestCase(priority=2000, profiles = {
        FCSTestProfile.CLARIN_FCS_LEGACY
})
public class TestScan1 extends FCSTest {

    @Override
    public String getName() {
        return "Scan";
    }


    @Override
    public String getDescription() {
        return "Scan with missing 'scanClause' argument";
    }


    @Override
    public String getExpected() {
        return "Expecting diagnostic \"info:srw/diagnostic/1/7\"";
    }


    @Override
    public FCSTestResult perform(FCSTestContext context, SRUClient client)
            throws SRUClientException {
        SRUScanRequest req = context.createScanRequest();
        req.setExtraRequestData(SRUScanRequest.X_MALFORMED_SCAN_CLAUSE,
                SRUScanRequest.MALFORMED_OMIT);
        SRUScanResponse res = client.scan(req);
        return findDiagnostic(res, "info:srw/diagnostic/1/7")
                ? makeSuccess()
                : makeErrorNoDiagnostic("info:srw/diagnostic/1/7");
    }

}
