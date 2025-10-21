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
package eu.clarin.fcs.tester.ui;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.commons.lang.StringEscapeUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.jouni.animator.AnimatorProxy;
import org.vaadin.jouni.animator.shared.AnimType;

import com.vaadin.event.MouseEvents;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import eu.clarin.fcs.tester.FCSTestContext;
import eu.clarin.fcs.tester.FCSTestResult;
import eu.clarin.fcs.tester.FCSTestResult.Code;

@SuppressWarnings("serial")
public class ResultsView extends VerticalLayout {
    private static final Logger logger =
            LoggerFactory.getLogger(ResultsView.class);
    private static final Resource ICON_SUCCESS =
            new ThemeResource("icons/result_success.png");
    private static final Resource ICON_WARNING =
            new ThemeResource("icons/result_warning.png");
    private static final Resource ICON_ERROR   =
            new ThemeResource("icons/result_error.png");
    private static final Resource ICON_SKIPPED =
            new ThemeResource("icons/weather_clouds.png");
    private static final DateTimeFormatter dateFmt =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");


    public ResultsView(FCSTestContext context, List<FCSTestResult> results) {
        setMargin(true);

        logger.debug("render results");

        int count_success = 0;
        int count_warning = 0;
        int count_error   = 0;
        int count_skipped = 0;
        for (FCSTestResult result : results) {
            switch (result.getCode()) {
            case SUCCESS:
                count_success++;
                break;
            case WARNING:
                count_warning++;
                break;
            case ERROR:
                count_error++;
                break;
            case SKIPPED:
                count_skipped++;
                break;
            } // switch
        } // for

        final String profileDisplayString =
                context.getProfile().toDisplayString();

        final Label resultsHeadline = new Label(String.format(
                "<h2>Result for %s (using test profile %s):</h2>",
                StringEscapeUtils.escapeXml(context.getBaseURI()),
                profileDisplayString), ContentMode.HTML);
        resultsHeadline.setStyleName("result-overall-headline");
        addComponent(resultsHeadline);

        Label resultOverall;
        if (count_error > 0) {
            String msg;
            if (count_error == 1) {
                msg = "The endpoint fails to pass in one test.";
            } else {
                msg = String.format("The endpoint fails to pass in %d tests.", count_error);
            }
            resultOverall = new Label(msg);
            resultOverall.setIcon(ICON_ERROR);
        } else if (count_warning > 0) {
            String msg;
            if (count_warning == 1) {
                msg = "The endpoint has minor problems to pass one test.";
            } else {
                msg = String.format("The endpoint has minor problems to pass %d tests.", count_warning);
            }
            resultOverall = new Label(msg);
            resultOverall.setIcon(getIconForCode(Code.WARNING));
        } else {
            resultOverall = new Label("The endpoint passed all tests successfully.");
            resultOverall.setIcon(getIconForCode(Code.SUCCESS));
        }
        resultOverall.setStyleName("result-overall-label");
        addComponent(resultOverall);

        final Label resultsOverview = new Label(
                String.format("Success: %d,  Warnings: %d, Errors: %d, Skipped: %d",
                        count_success, count_warning, count_error, count_skipped));
        addComponent(resultsOverview);

        final Label resultListHeadline =
                new Label("<h2>Results for individual test cases:</h2>", ContentMode.HTML);
        resultListHeadline.setStyleName("result-individual-headline");
        addComponent(resultListHeadline);

        for (FCSTestResult result : results) {
            final CssLayout content = new CssLayout();
            content.addStyleName("result-panel-content");

            final Label expectedResultLabel = new Label(
                    String.format("<span class=\"result-label-caption\">Expected result:</span> %s",
                            StringEscapeUtils.escapeXml(result.getExpectedResultMessage())),
                    ContentMode.HTML);
            expectedResultLabel.setStyleName("result-label");
            content.addComponent(expectedResultLabel);

            final Label actualResultLabel = new Label(
                    String.format("<span class=\"result-label-caption\">Actual result:</span> <em>%s</em>",
                            StringEscapeUtils.escapeXml(result.getActualResultMessage())),
                    ContentMode.HTML);
            actualResultLabel.setStyleName("result-label");
            content.addComponent(actualResultLabel);

            if (result.getLogRecords() != null) {
                final StringBuilder sb = new StringBuilder();
                for (LogRecord record : result.getLogRecords()) {
                    String clazz = null;
                    if (record.getLevel() == Level.SEVERE) {
                        clazz = "error";
                    } else if (record.getLevel() == Level.WARNING) {
                        clazz = "warning";
                    }
                    if (clazz != null) {
                        sb.append("<span class=\"").append(clazz).append("\">");
                    }
                    sb.append("<span class= \"timestamp\">")
                        .append("[")
                        .append(StringEscapeUtils.escapeXml(dateFmt.print(record.getMillis())))
                        .append("] ")
                        .append("<span>")
                        .append(StringEscapeUtils.escapeXml(record.getMessage()));
                    if (clazz != null) {
                        sb.append("</span>");
                    }
                    sb.append("<br />");
                }

                final Label logMessages =
                        new Label(sb.toString(), ContentMode.HTML);
                logMessages.setCaption("Debug messages:");
                logMessages.setStyleName("result-panel-content-messages");
                content.addComponent(logMessages);
            }

            final String caption = String.format("[%s] %s: %s",
                    profileDisplayString,
                    result.getTestCaseName(),
                    result.getTestCaseDescription());

            final AnimatorProxy proxy = new AnimatorProxy();
            proxy.setStyleName("animator");
            final CssLayout c = new CssLayout();
            c.setWidth("100%");
            c.addComponent(proxy);

            final Panel panel =
                    new Panel(StringEscapeUtils.escapeXml(caption), c);
            panel.addStyleName("result-panel");
            panel.setIcon(getIconForCode(result.getCode()));
            panel.addClickListener(new MouseEvents.ClickListener() {
                boolean open = false;

                @Override
                public void click(MouseEvents.ClickEvent event) {
                    if (open) {
                        proxy.animate(content, AnimType.ROLL_UP_CLOSE_REMOVE);
                        panel.removeStyleName("result-panel-opened");
                        open = false;
                    } else {
                        if ((content.getParent() == null) || (content.getParent() != c)) {
                            c.addComponent(content);
                        }
                        proxy.animate(content, AnimType.ROLL_DOWN_OPEN);
                        panel.addStyleName("result-panel-opened");
                        open = true;
                    }
                }
            });
            addComponent(panel);
        } // for
    }


    private static Resource getIconForCode(FCSTestResult.Code code) {
        if (code != null) {
            switch (code) {
            case SUCCESS:
                return ICON_SUCCESS;
            case WARNING:
                return ICON_WARNING;
            case ERROR:
                return ICON_ERROR;
            case SKIPPED:
                return  ICON_SKIPPED;
            }
        }
        return null;
    }

} // class TestResultView
