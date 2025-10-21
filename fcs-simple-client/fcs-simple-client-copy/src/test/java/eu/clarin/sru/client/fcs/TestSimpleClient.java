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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUDefaultHandlerAdapter;
import eu.clarin.sru.client.SRUDiagnostic;
import eu.clarin.sru.client.SRUExplainRecordData;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRURecordData;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSimpleClient;
import eu.clarin.sru.client.SRUWhereInList;


public class TestSimpleClient {
    private static final Logger logger =
            LoggerFactory.getLogger(TestSimpleClient.class);

    public static void main(String[] args) {
        if (args.length > 0) {
            logger.info("initializing client ...");

            SRUSimpleClient client = new ClarinFCSClientBuilder()
                        .addDefaultDataViewParsers()
                        .unknownDataViewAsString()
                        .buildSimpleClient();

            /*
             * just use one dump handler for each request.
             * A real application should be smarter here and use the
             * appropriate handler
             */
            SRUDefaultHandlerAdapter handler = new SRUDefaultHandlerAdapter() {
                @Override
                public void onDiagnostics(List<SRUDiagnostic> diagnostics)
                        throws SRUClientException {
                    for (SRUDiagnostic diagnostic : diagnostics) {
                        logger.info("onDiagnostics: uri={}, detail={}, message={}",
                                diagnostic.getURI(), diagnostic.getDetails(),
                                diagnostic.getMessage());
                    }
                }

                @Override
                public void onRequestStatistics(int bytes, long millisTotal,
                        long millisNetwork, long millisParsing) {
                    logger.info("onRequestStatistics(): {} bytes in {} millis",
                            bytes, millisTotal);
                }

                @Override
                public void onStartTerms() throws SRUClientException {
                    logger.info("onStartTerms()");
                }

                @Override
                public void onFinishTerms() throws SRUClientException {
                    logger.info("onFinishTerms()");
                }

                @Override
                public void onTerm(String value, int numberOfRecords,
                        String displayTerm, SRUWhereInList whereInList)
                        throws SRUClientException {
                    logger.info("onTerm(): value = {}, numberOfRecords = {}, " +
                            "displayTerm = {}, whereInList = {}",
                            value, numberOfRecords, displayTerm, whereInList);
                }

                @Override
                public void onStartRecords(int numberOfRecords,
                        String resultSetId, int resultSetIdleTime)
                        throws SRUClientException {
                    logger.info("onStartRecords(): numberOfRecords = {}",
                            numberOfRecords);
                }

                @Override
                public void onFinishRecords(int nextRecordPosition)
                        throws SRUClientException {
                    logger.info("onFinishRecords(): nextRecordPosition = {}",
                            nextRecordPosition);
                }

                @Override
                public void onRecord(String identifier, int position,
                        SRURecordData data) throws SRUClientException {
                    logger.info("onRecord(): identifier = {}, position = {}, schema = {}",
                                identifier, position, data.getRecordSchema());
                    if (ClarinFCSRecordData.RECORD_SCHEMA.equals(data.getRecordSchema())) {
                        ClarinFCSRecordData record =
                                (ClarinFCSRecordData) data;
                        TestUtils.dumpResource(record.getResource());
                    } else if (SRUExplainRecordData.RECORD_SCHEMA.equals(data.getRecordSchema())) {
                        TestUtils.dumpExplainRecordData(data);
                    }
                }

                @Override
                public void onSurrogateRecord(String identifier, int position,
                        SRUDiagnostic data) throws SRUClientException {
                    logger.info("onSurrogateRecord: identifier = {}, position = {}, uri={}, detail={}, message={}",
                            identifier, position, data.getURI(), data.getDetails(), data.getMessage());
                }
            };

            try {
                logger.info("performing 'explain' request ...");
                SRUExplainRequest request = TestUtils.makeExplainRequest(args[0]);
                client.explain(request, handler);
            } catch (SRUClientException e) {
                logger.error("a fatal error occured while performing 'explain' request", e);
            }

            try {
                logger.info("performing 'scan' request ...");
                SRUScanRequest request = TestUtils.makeScanRequest(args[0]);
                client.scan(request, handler);
            } catch (SRUClientException e) {
                logger.error("a fatal error occured while performing 'scan' request", e);
            }

            try {
                logger.info("performing 'searchRetrieve' request ...");
                SRUSearchRetrieveRequest request = TestUtils.makeSearchRequest(args[0], null);
                client.searchRetrieve(request, handler);
            } catch (SRUClientException e) {
                logger.error("a fatal error occured while performing 'searchRetrieve' request", e);
            }

            logger.info("done");
        } else {
            System.err.println("missing args");
            System.exit(64);
        }
    }


    static {
        org.apache.log4j.BasicConfigurator.configure(
                new org.apache.log4j.ConsoleAppender(
                        new org.apache.log4j.PatternLayout("%-5p [%t] %m%n"),
                        org.apache.log4j.ConsoleAppender.SYSTEM_ERR));
        org.apache.log4j.Logger logger =
                org.apache.log4j.Logger.getRootLogger();
        logger.setLevel(org.apache.log4j.Level.INFO);
        logger.getLoggerRepository().getLogger("eu.clarin").setLevel(
                org.apache.log4j.Level.DEBUG);
    }

} // class TestSimpleClient
