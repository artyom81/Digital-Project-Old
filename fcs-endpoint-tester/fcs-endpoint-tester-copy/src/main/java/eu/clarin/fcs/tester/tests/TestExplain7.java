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

import java.util.List;

import eu.clarin.fcs.tester.FCSTest;
import eu.clarin.fcs.tester.FCSTestCase;
import eu.clarin.fcs.tester.FCSTestContext;
import eu.clarin.fcs.tester.FCSTestProfile;
import eu.clarin.fcs.tester.FCSTestResult;
import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.fcs.ClarinFCSConstants;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;
import eu.clarin.sru.client.fcs.DataViewAdvanced;
import eu.clarin.sru.client.fcs.DataViewHits;

@FCSTestCase(priority=1100, profiles = {
        FCSTestProfile.CLARIN_FCS_2_0,
})
public class TestExplain7 extends FCSTest {

    @Override
    public String getName() {
        return "Explain";
    }


    @Override
    public String getDescription() {
        return "Check for a valid FCS endpoint description";
    }


    @Override
    public String getExpected() {
        return "Expecting exactly one valid FCS endpoint decription conforming to FCS 2.0 spec";
    }


    @Override
    public FCSTestResult perform(FCSTestContext context, SRUClient client)
            throws SRUClientException {
        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(
                ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION,
                ClarinFCSConstants.TRUE);
        SRUExplainResponse res = client.explain(req);
        
        List<ClarinFCSEndpointDescription> descs =
                res.getExtraResponseData(ClarinFCSEndpointDescription.class);
        if (descs != null) {
            if (descs.size() == 1) {
                return validate(context, descs.get(0));
            } else {
                return makeError("Endpoint must only return one instance of a CLARIN FCS endpoint description");
            }
        } else {
            return makeError("Endpoint did not return a CLARIN FCS endpoint description");
        }
    }


    private FCSTestResult validate(FCSTestContext context, ClarinFCSEndpointDescription desc) {
        if (desc.getVersion() == 2) {
            boolean supportsADV = desc.getCapabilities()
                    .contains(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH);
            if (supportsADV) {
                context.setProperty(FCSTestContext.PROP_SUPPORTS_ADV, Boolean.TRUE);
            }
            if (desc.getSupportedDataViews().size() == 0) {
                return makeError("No dataview declared. Endpoint must declare support for at least one data view");
            } else {
                boolean foundHits = false;
                boolean foundAdv = false;
                for (ClarinFCSEndpointDescription.DataView dataview : desc
                        .getSupportedDataViews()) {
                    if (dataview.isMimeType(DataViewHits.TYPE)) {
                        foundHits = true;
                    }
                    if (dataview.isMimeType(DataViewAdvanced.TYPE)) {
                        if (supportsADV) {
                            foundAdv = true;
                        }
                    }
                }
                if (!foundHits) {
                    return makeError("Endpoint must declare support for Generic Hits dataview (mimeType = " + DataViewHits.TYPE + ")");
                }
                if (supportsADV && !foundAdv) {
                    return makeError("Capabilites indicate support for Advanced Search, so Endpoint must declare support for Advanced dataview (mimeType = " + DataViewHits.TYPE + ")");
                    
                }
            }
            if (desc.getResources().size() == 0) {
                return makeError("No resources declared. Endpoint must declare at least one Resource");
            }
            if (supportsADV) {
                if (desc.getSupportedLayers().size() == 0) {
                    return makeError("Capabilites indicate support for Advanced Search, so Endpoint must supported layers");                    
                }
            }
            return makeSuccess();
        } else {
            return makeError("FCS 2.0 endpoint must provide an endpoint description with version set to \"2\"");
        }
    }

}
