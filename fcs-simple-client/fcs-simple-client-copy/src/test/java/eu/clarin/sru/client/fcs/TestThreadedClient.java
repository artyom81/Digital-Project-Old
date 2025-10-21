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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;

@Deprecated
public class TestThreadedClient {
    private static final Logger logger =
            LoggerFactory.getLogger(TestThreadedClient.class);

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
                /*
                 * the following requests will be run asynchronously and
                 * concurrently
                 */
                logger.info("submitting 'explain' request ...");
                SRUExplainRequest request1 = TestUtils.makeExplainRequest(args[0]);
                Future<SRUExplainResponse> result1 = client.explain(request1);

                logger.info("submitting 'scan' request ...");
                SRUScanRequest request2 = TestUtils.makeScanRequest(args[0]);
                Future<SRUScanResponse> result2 = client.scan(request2);

                logger.info("submitting 'searchRetrieve' request ...");
                SRUSearchRetrieveRequest request3 = TestUtils.makeSearchRequest(args[0], null);
                Future<SRUSearchRetrieveResponse> result3 =
                        client.searchRetrieve(request3);

                /*
                 * HACK: the following code is quick and dirty. Don't every busy
                 * wait on responses like this in a real-world application!
                 */
                while (!(result1.isDone() && result2.isDone() &&
                        result3.isDone())) {
                    logger.debug("waiting for results ...");
                    try {
                        Thread.sleep(125);
                    } catch (InterruptedException e) {
                        /* IGNORE */
                    }
                }

                try {
                    TestUtils.printExplainResponse(result1.get());
                } catch (ExecutionException e) {
                    logger.error("some error occured while performing 'explain' request", e);
                }
                try {
                    TestUtils.printScanResponse(result2.get());
                } catch (ExecutionException e) {
                    logger.error("some error occured while performing 'scan' request", e);
                }
                try {
                    TestUtils.printSearchResponse(result3.get());
                } catch (ExecutionException e) {
                    logger.error("some error occured while performing 'searchRetrieve' request", e);
                }
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
