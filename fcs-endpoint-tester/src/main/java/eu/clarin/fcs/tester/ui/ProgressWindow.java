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

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;


@SuppressWarnings("serial")
public class ProgressWindow extends Window {
    private final Label messageLabel;
    private final ProgressBar progress;
    private float maximum = -1;


    public ProgressWindow(String caption, String message) {
        super(caption);
        setModal(true);
        setReadOnly(true);
        setResizable(false);
        setStyleName(Reindeer.WINDOW_LIGHT);

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setSizeUndefined();

        progress = new ProgressBar();
        progress.setIndeterminate(false);
        progress.setWidth("350px");
        layout.addComponent(progress);
        layout.setComponentAlignment(progress, Alignment.MIDDLE_CENTER);

        messageLabel = new Label(message);
        messageLabel.setSizeUndefined();
        layout.addComponent(messageLabel);
        layout.setComponentAlignment(messageLabel, Alignment.MIDDLE_CENTER);
        setContent(layout);
    }


    @Override
    public void attach() {
        super.attach();
        ((TesterUI) UI.getCurrent()).enablePolling();
    }


    @Override
    public void detach() {
        ((TesterUI) UI.getCurrent()).disablePolling();
        super.detach();
    }


    @Override
    public void close() {
        UI.getCurrent().removeWindow(ProgressWindow.this);
    }


//    public void updateStatus(String message) {
//      if (message != null) {
//          messageLabel.setValue(message);
//      }
//      forceRepaint();
//    }


    public void updateMaximum(int maximum) {
        if ((maximum > 0) && (this.maximum < 0)) {
            this.maximum = maximum;
        }
        forceRepaint();
    }


    public void updateStatus(String message, float done) {
        if ((maximum > 0) && (done >= 0)) {
            progress.setValue(Math.min(done / maximum, 1.0f));
        }
        if (message != null) {
            messageLabel.setValue(message);
        }
        forceRepaint();
    }


    private void forceRepaint() {
        markAsDirtyRecursive();
        try {
            Thread.sleep(125);
        } catch (InterruptedException e) {
            /* IGNORE */
        }
    }

} // class ProgressWindow
