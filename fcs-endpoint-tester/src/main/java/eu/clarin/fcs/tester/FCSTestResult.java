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

import java.util.List;
import java.util.logging.LogRecord;


public class FCSTestResult {
    public static enum Code {
        SUCCESS, WARNING, ERROR, SKIPPED;
    }

    private final FCSTest testCase;
    private final Code code;
    private final String message;
    private List<LogRecord> records;


    public FCSTestResult(FCSTest testCase, Code code, String message,
            List<LogRecord> records) {
        this.testCase = testCase;
        this.code = code;
        this.message = message;
        this.records = records;
    }


    public FCSTestResult(FCSTest testCase, Code code, String message) {
        this(testCase, code, message, null);
    }


    public FCSTestResult(FCSTest testCase, Code code) {
        this(testCase, code, null, null);
    }


    public void setLogRecords(List<LogRecord> records) {
        this.records = records;
    }


    public Code getCode() {
        return code;
    }


    public List<LogRecord> getLogRecords() {
        return records;
    }


    public String getTestCaseName() {
        return testCase.getName();
    }


    public String getTestCaseDescription() {
        return testCase.getDescription();
    }


    public String getActualResultMessage() {
        return message;
    }


    public String getExpectedResultMessage() {
        return testCase.getExpected();
    }

} // class SRUTestResult
