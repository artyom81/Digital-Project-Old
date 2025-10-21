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

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class NoResultsView extends VerticalLayout {

    public NoResultsView() {
        setMargin(true);
        setSizeFull();

        final Label message =
                new Label("<h2>No results available.</h2>", ContentMode.HTML);
        message.setSizeUndefined();
        message.setStyleName("no-results-label");
        addComponent(message);
        setComponentAlignment(message, Alignment.MIDDLE_CENTER);
    }

} // class NoResultsView
