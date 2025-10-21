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

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUCallback;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;


public class TestThreadedClientCallback {
    private static final Logger logger =
            LoggerFactory.getLogger(TestThreadedClientCallback.class);

    public static void main(String[] args) {
        if (args.length > 0) {
            logger.info("initializing client ...");

            SRUThreadedClient client = new ClarinFCSClientBuilder()
                    .addDefaultDataViewParsers()
                    .unknownDataViewAsString()
                    .enableLegacySupport()
                    .registerExtraResponseDataParser(
                            new ClarinFCSEndpointDescriptionParser())
                    .buildThreadedClient();

            try {
                final CountDownLatch latch = new CountDownLatch(3);

                /*
                 * the following requests will be run asynchronously and
                 * concurrently
                 * Invoke requests and supply a callback, that
                 *  a) prints the results
                 *  b) downs a latch (which is used to make the main-thread wait
                 *     on the requests to be completed)
                 */
                logger.info("submitting 'explain' request ...");
                SRUExplainRequest request1 =
                        TestUtils.makeExplainRequest(args[0]);
                client.explain(request1, new SRUCallback<SRUExplainRequest, SRUExplainResponse>() {
                    @Override
                    public void onSuccess(SRUExplainResponse response) {
                        TestUtils.printExplainResponse(response);
                        latch.countDown();
                    }

                    @Override
                    public void onError(SRUExplainRequest request,
                            SRUClientException error) {
                        logger.error("error while performing request", error);
                        latch.countDown();
                    }
                });

                logger.info("submitting 'scan' request ...");
                SRUScanRequest request2 =
                        TestUtils.makeScanRequest(args[0]);
                client.scan(request2, new SRUCallback<SRUScanRequest, SRUScanResponse>() {
                    @Override
                    public void onSuccess(SRUScanResponse response) {
                        TestUtils.printScanResponse(response);
                        latch.countDown();
                    }

                    @Override
                    public void onError(SRUScanRequest request,
                            SRUClientException error) {
                        logger.error("error while performing request", error);
                        latch.countDown();
                    }
                });

                logger.info("submitting 'searchRetrieve' request ...");
                SRUSearchRetrieveRequest request3 =
                        TestUtils.makeSearchRequest(args[0], null);
                client.searchRetrieve(request3, new SRUCallback<SRUSearchRetrieveRequest, SRUSearchRetrieveResponse>() {
                    @Override
                    public void onSuccess(SRUSearchRetrieveResponse response) {
                        TestUtils.printSearchResponse(response);
                        latch.countDown();
                    }

                    @Override
                    public void onError(SRUSearchRetrieveRequest request,
                            SRUClientException error) {
                        logger.error("error while performing request", error);
                        latch.countDown();
                    }
                });

                latch.await();
            } catch (Exception e) {
                logger.error("a fatal error occured while performing request", e);
            }

            client.shutdown();
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

}
