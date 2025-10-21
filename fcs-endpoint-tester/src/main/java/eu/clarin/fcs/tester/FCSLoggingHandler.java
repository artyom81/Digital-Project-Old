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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


class FCSLoggingHandler extends Handler {
    public final Map<Long, List<LogRecord>> messages =
            new HashMap<Long, List<LogRecord>>();


    @Override
    public void publish(LogRecord record) {
        final Long id = Thread.currentThread().getId();

        List<LogRecord> records = null;
        synchronized (messages) {
            records = messages.get(id);
            if (records == null) {
                records = new LinkedList<LogRecord>();
                messages.put(id, records);
            }
        } // synchronized (messages)
        records.add(record);
    }


    @Override
    public void flush() {
    }


    @Override
    public void close() throws SecurityException {
    }


    List<LogRecord> getLogRecords() {
        synchronized (messages) {
            return messages.remove(Thread.currentThread().getId());
        } // synchronized (messages)
    }

} // class SRULoggingHandler
