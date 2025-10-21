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
package eu.clarin.fcs.tester;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;

import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescriptionParser;


public class FCSTestContext {
    public static final String PROP_SUPPORTS_ADV = "supports_adv_search";
    private final FCSTestProfile profile;
    private final String baseURI;
    private final String userSearchTerm;
    private final boolean strictMode;
    private final int connectTimeout;
    private final int socketTimeout;
    private final String randomSearchTerm =
            RandomStringUtils.randomAlphanumeric(16);
    private final String unicodeSearchTerm = "öäüÖÄÜß€";
    private SRUClient client;
    private Map<String, Object> properies;


    public FCSTestContext(FCSTestProfile profile, String baseURI,
            String userSearchTerm, boolean strictMode, int connectTimeout,
            int socketTimeout) {
        if (profile == null) {
            throw new NullPointerException("profile == null");
        }
        this.profile = profile;
        if (baseURI == null) {
            throw new NullPointerException("baseURI == null");
        }
        this.baseURI = baseURI;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.strictMode = strictMode;
        if (userSearchTerm.indexOf(' ') != -1) {
            this.userSearchTerm = "\"" + userSearchTerm + "\"";
        } else {
            this.userSearchTerm = userSearchTerm;
        }
    }


    public void init() {
        if (client != null) {
            throw new IllegalStateException("already initialized!");
        }

        ClarinFCSClientBuilder builder = null;
        switch (profile) {
        case CLARIN_FCS_1_0:
            builder = new ClarinFCSClientBuilder()
                .addDefaultDataViewParsers()
                .setDefaultSRUVersion(SRUVersion.VERSION_1_2)
                .unknownDataViewAsString()
                .registerExtraResponseDataParser(
                        new ClarinFCSEndpointDescriptionParser());
            if (!strictMode) {
                builder.enableLegacySupport();
            }
            break;
        case CLARIN_FCS_2_0:
            builder = new ClarinFCSClientBuilder()
                .addDefaultDataViewParsers()
                .setDefaultSRUVersion(SRUVersion.VERSION_2_0)
                .unknownDataViewAsString()
                .registerExtraResponseDataParser(
                        new ClarinFCSEndpointDescriptionParser());
            break;
        case CLARIN_FCS_LEGACY:
            builder = new ClarinFCSClientBuilder()
                .addDefaultDataViewParsers()
                .setDefaultSRUVersion(SRUVersion.VERSION_1_2)
                .unknownDataViewAsString()
                .enableLegacySupport()
                .enableFullLegacyCompatMode();
            break;
        }
        client = builder.buildClient();
    }


    public FCSTestProfile getProfile() {
        return profile;
    }


    public String getBaseURI() {
        return baseURI;
    }


    public boolean isStrictMode() {
        return strictMode;
    }


    public int getConnectTimeout() {
        return connectTimeout;
    }


    public int getSocketTimeout() {
        return socketTimeout;
    }


    public String getUserSearchTerm() {
        return userSearchTerm;
    }


    public String getRandomSearchTerm() {
        return randomSearchTerm;
    }


    public String getUnicodeSearchTerm() {
        return unicodeSearchTerm;
    }


    public SRUClient getClient() {
        return client;
    }


    public SRUExplainRequest createExplainRequest() {
        SRUExplainRequest request =
                new SRUExplainRequest(baseURI.toString());
        request.setStrictMode(strictMode);
        return request;
    }


    public SRUScanRequest createScanRequest() {
        SRUScanRequest request =
                new SRUScanRequest(baseURI.toString());
        request.setStrictMode(strictMode);
        return request;
    }


    public SRUSearchRetrieveRequest createSearchRetrieveRequest() {
        SRUSearchRetrieveRequest request =
                new SRUSearchRetrieveRequest(baseURI.toString());
        request.setStrictMode(strictMode);
        return request;
    }

    public void setProperty(String key, Object value) {
        if (properies == null) {
            properies = new HashMap<String, Object>();
        }
        properies.put(key, value);
    }
    
    public Object getProperty(String key) {
        if (properies != null) {
            return properies.get(key);
        } else {
            return null;
        }
    }
    
    public boolean hasProperty(String key) {
        if (properies != null) {
            return properies.containsKey(key);
        } else {
            return false;
        }
    }

} // class FCSTestContext
