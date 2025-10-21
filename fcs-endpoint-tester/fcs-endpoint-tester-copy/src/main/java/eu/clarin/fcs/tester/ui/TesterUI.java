/**
 * This software is copyright (c) 2013-2016 by
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.hene.popupbutton.PopupButton;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Validator;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import eu.clarin.fcs.tester.FCSEndpointTester;
import eu.clarin.fcs.tester.FCSTestContext;
import eu.clarin.fcs.tester.FCSTestProfile;
import eu.clarin.fcs.tester.FCSTestResult;


@SuppressWarnings("serial")
@PreserveOnRefresh
@Theme("clarin")
@Title("CLARIN-FCS endpoint conformance tester")
@Widgetset("eu.clarin.fcs.tester.ui.Widgetset")
public class TesterUI extends UI {
    private static final String PARAM_DEFAULT_ENDPOINT_URI =
            "eu.clarin.fcs.tester.defaultEndpointURI";
    private static final String PARAM_DEFAULT_SEARCH_TERM =
            "eu.clarin.fcs.tester.defaultSearchTerm";
    private static final String COOKIE_NAME_ENDPOINT_URI =
            "lastEndpointURI";
    private static final String COOKIE_NAME_SEARCH_TERM =
            "lastSearchTerm";
    private static final Logger logger =
            LoggerFactory.getLogger(TesterUI.class);
    private static final ErrorMessage ERROR_INVALID_INPUT =
            new UserError("Invalid input:");
    private TextField uriField;
    private TextField queryField;
    private ComboBox profileCombo;
    private CheckBox strictModeCheckbox;
    private CheckBox performProbeCheckbox;
    private ComboBox connectTimeoutCombo;
    private ComboBox socketTimeoutCombo;
    private Panel content;
    private int pollingRequests = 0;


    @Override
    protected void init(VaadinRequest request) {
        logger.debug("init(): remoteAddr={}, session={}",
                request.getRemoteAddr(), request.getWrappedSession().getId());

        final ServletContext ctx =
                VaadinServlet.getCurrent().getServletContext();

        final VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(true);
        layout.setSpacing(false);
        layout.setStyleName("small-margin");
        setContent(layout);

        final Label title = new Label(
                "<h1>CLARIN-FCS endpoint conformance tester</h1>",
                ContentMode.HTML);
        title.setStyleName("header");
        layout.addComponent(title);

        /*
         * input panel
         */
        final HorizontalLayout top = new HorizontalLayout();
        top.setSpacing(true);
        top.setMargin(new MarginInfo(false, true, false, true));
        top.setWidth("100%");
        top.setStyleName("small-margin");

        final FormLayout inputForm = new FormLayout();
        top.addComponent(inputForm);
        top.setComponentAlignment(inputForm, Alignment.MIDDLE_LEFT);
        top.setExpandRatio(inputForm, 1.0f);

        uriField = new TextField("Endpoint BaseURI:");
        uriField.setWidth("100%");
        uriField.setInputPrompt("Please enter BaseURI of endpoint.");
        uriField.setRequiredError("An endpoint BaseURI is required!");
        uriField.setMaxLength(255);
        uriField.setNullRepresentation("");
        uriField.setRequired(true);
        uriField.addValidator(new Validator() {
            @Override
            public void validate(Object value) throws InvalidValueException {
                if (value != null) {
                    String s = (String) value;
                    s = s.trim();
                    if (!s.isEmpty()) {
                        try {
                            new URL(s);
                        } catch (MalformedURLException e) {
                            throw new InvalidValueException("Invalid URI syntax!");
                        }
                    }
                }
            }
        });
        String defaultEndpointURI = getCookie(COOKIE_NAME_ENDPOINT_URI);
        if (defaultEndpointURI == null) {
            defaultEndpointURI =
                    ctx.getInitParameter(PARAM_DEFAULT_ENDPOINT_URI);
            if (defaultEndpointURI != null) {
                defaultEndpointURI = defaultEndpointURI.trim();
                if (defaultEndpointURI.isEmpty()) {
                    defaultEndpointURI = null;
                }
            }
        }

        uriField.setValue(defaultEndpointURI);
        inputForm.addComponent(uriField);

        queryField = new TextField("Search Term:");
        queryField.setWidth("100%");
        queryField.setInputPrompt("Please enter a word that occurs at least once in you data.");
        queryField.setMaxLength(64);
        queryField.setNullRepresentation("");
        queryField.setRequired(true);
        queryField.setRequiredError("A search term is required!");

        String defaultSearchTerm = getCookie(COOKIE_NAME_SEARCH_TERM);
        if (defaultSearchTerm == null) {
            defaultSearchTerm = ctx.getInitParameter(PARAM_DEFAULT_SEARCH_TERM);
            if (defaultSearchTerm != null) {
                defaultSearchTerm = defaultSearchTerm.trim();
            }
        }
        queryField.setValue(defaultSearchTerm);
        inputForm.addComponent(queryField);

        final Button goButton = new Button("Go");
        top.addComponent(goButton);
        top.setComponentAlignment(goButton, Alignment.MIDDLE_LEFT);

        /* options panel */
        final VerticalLayout optionsContent = new VerticalLayout();
        optionsContent.setMargin(true);
        optionsContent.setSpacing(true);
        optionsContent.setStyleName("small-margin");

        profileCombo = new ComboBox("CLARIN-FCS test profile:");
        profileCombo.addItem(PROFILE_AUTODETECT);
        profileCombo.setItemCaption(PROFILE_AUTODETECT, "Auto detect");
        profileCombo.addItem(PROFILE_FCS_LEGACY);
        profileCombo.setItemCaption(PROFILE_FCS_LEGACY, "Legacy FCS");
        profileCombo.addItem(PROFILE_FCS_1_0);
        profileCombo.setItemCaption(PROFILE_FCS_1_0, "CLARIN-FCS 1.0");
        profileCombo.addItem(PROFILE_FCS_2_0);
        profileCombo.setItemCaption(PROFILE_FCS_2_0, "CLARIN-FCS 2.0");
        profileCombo.select(PROFILE_AUTODETECT);
        profileCombo.setNullSelectionAllowed(false);
        profileCombo.setTextInputAllowed(false);
        profileCombo.setWidth("100%");
        optionsContent.addComponent(profileCombo);

        strictModeCheckbox = new CheckBox("Perform checks in strict mode");
        strictModeCheckbox.setWidth("100%");
        strictModeCheckbox.setValue(Boolean.TRUE);
        optionsContent.addComponent(strictModeCheckbox);

        performProbeCheckbox = new CheckBox("Perform HTTP HEAD probe request");
        performProbeCheckbox.setWidth("100%");
        performProbeCheckbox.setValue(Boolean.TRUE);
        optionsContent.addComponent(performProbeCheckbox);

        connectTimeoutCombo = new ComboBox("Connect timeout:");
        fillTimeoutCombo(connectTimeoutCombo);
        connectTimeoutCombo.select(TIMEOUT_15_SECONDS);
        connectTimeoutCombo.setNullSelectionAllowed(false);
        connectTimeoutCombo.setTextInputAllowed(false);
        connectTimeoutCombo.setWidth("100%");
        optionsContent.addComponent(connectTimeoutCombo);

        socketTimeoutCombo = new ComboBox("Socket timeout:");
        fillTimeoutCombo(socketTimeoutCombo);
        socketTimeoutCombo.select(TIMEOUT_30_SECONDS);
        socketTimeoutCombo.setNullSelectionAllowed(false);
        socketTimeoutCombo.setTextInputAllowed(false);
        socketTimeoutCombo.setWidth("100%");
        optionsContent.addComponent(socketTimeoutCombo);

//        final Table x = new Table("Custom Request Parameters");
//        x.setWidth("100%");
//        x.setHeight("200px");
//        // x.setIt
//        IndexedContainer data = new IndexedContainer();
//        data.addContainerProperty("KEY", String.class, "");
//        data.addContainerProperty("VALUE", String.class, "");
//
//        Object id1 = data.addItem();
//        data.getContainerProperty(id1, "KEY").setValue("key1");
//        data.getContainerProperty(id1, "VALUE").setValue("value1");
//
//        x.setContainerDataSource(data);
//        optionsContent.addComponent(x);
//
//        HorizontalLayout addLayout = new HorizontalLayout();
//        addLayout.setSpacing(true);
//        final TextField keyField = new TextField();
//        addLayout.addComponent(keyField);
//        final TextField valueField = new TextField();
//        addLayout.addComponent(valueField);
//        final Button addButton = new Button("Add");
//        addLayout.addComponent(addButton);
//        optionsContent.addComponent(addLayout);

        final PopupButton optionsButton = new PopupButton("Options");
        optionsButton.setContent(optionsContent);
        top.addComponent(optionsButton);
        top.setComponentAlignment(optionsButton, Alignment.MIDDLE_LEFT);
        layout.addComponent(top);

        /*
         * main content
         */
        content = new Panel();
        content.setSizeFull();
        content.setStyleName("main-content");
        layout.addComponent(content);
        layout.setExpandRatio(content, 1.0f);

        final Label footer = new Label(
                "<div>For questions of bug reports please contact <a href=\"mailto:fcs@clarin.eu\">fcs@clarin.eu</a>.</div>",
                ContentMode.HTML);
        footer.setStyleName("footer");
        layout.addComponent(footer);

        setNoResultsView();

        /* setup up some behaviors */
        goButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                boolean valid = true;
                try {
                    uriField.validate();
                    uriField.setComponentError(null);
                } catch (InvalidValueException e) {
                    valid = false;
                    uriField.setComponentError(ERROR_INVALID_INPUT);
                }
                try {
                    queryField.validate();
                    queryField.setComponentError(null);
                } catch (InvalidValueException e) {
                    valid = false;
                    queryField.setComponentError(ERROR_INVALID_INPUT);
                }

                if (valid) {
                    performAction();
                }

            }
        });

        uriField.focus();

//        final Notification info =
//                new Notification("The CLARIN-FCS endpoint conformance tester does" +
//                        " not yet support the new FCS specification!",
//                        Notification.Type.ERROR_MESSAGE);
//        info.setDelayMsec(5000);
//        info.setDescription("(Dismiss this message by clicking it)");
//        info.show(Page.getCurrent());
    }


    @Override
    public void detach() {
        setPollInterval(-1);
        super.detach();
    }


    void enablePolling() {
        if (++pollingRequests > 0) {
            setPollInterval(250);
        }
    }


    void disablePolling() {
        if (--pollingRequests <= 0) {
            setPollInterval(-1);
            pollingRequests = 0;
        }
    }


    private void setNoResultsView() {
        Component component = content.getContent();
        if ((component == null) ||
                !component.getClass().equals(NoResultsView.class)) {
            content.setContent(new NoResultsView());
        }
    }


    private void performAction() {
        FCSTestProfile profile = null;
        switch (((Integer) profileCombo.getValue()).intValue()) {
        case PROFILE_FCS_1_0:
            profile = FCSTestProfile.CLARIN_FCS_1_0;
            break;
        case PROFILE_FCS_2_0:
            profile = FCSTestProfile.CLARIN_FCS_2_0;
            break;
        case PROFILE_FCS_LEGACY:
            profile = FCSTestProfile.CLARIN_FCS_LEGACY;
            break;
        default:
            /* DO NOTHING */
        }

        final String endpointURI = uriField.getValue();
        final String searchTerm = queryField.getValue();
        final boolean strictMode =
                strictModeCheckbox.getValue().booleanValue();
        final boolean performProbeRequest =
                performProbeCheckbox.getValue().booleanValue();
        final int connectTimeout =
                ((Integer) connectTimeoutCombo.getValue()).intValue();
        final int socketTimeout =
                ((Integer) socketTimeoutCombo.getValue()).intValue();

        setCookie(COOKIE_NAME_ENDPOINT_URI, endpointURI);
        setCookie(COOKIE_NAME_SEARCH_TERM, searchTerm);

        logger.info("perform check for: profile={}, endpointURI={} query={}, strict={}",
                profile, endpointURI, searchTerm, (strictMode ? "yes" : "no"));

        final FCSEndpointTester tester = FCSEndpointTester.getInstance();

        final ProgressWindow progressWindow = new ProgressWindow(
                "Checking SRU/CQL endpoint ....", "Initializing ...");
        setNoResultsView();
        addWindow(progressWindow);

        final FCSEndpointTester.ProgressListener listener =
                new FCSEndpointTester.ProgressListener() {
            private int test = -1;
            @Override
            public void updateProgress(final String message) {
                accessSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        progressWindow.updateStatus(message, test);
                        if (test != -1) {
                            test++;
                        }
                    }
                });
            }

            @Override
            public void updateMaximum(final int maximum) {
                accessSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        if (maximum > 0) {
                            progressWindow.updateMaximum(maximum);
                            test = 0;
                        }
                    }
                });
            }

            @Override
            public void onDone(final FCSTestContext context,
                    final List<FCSTestResult> results) {
                // force update of status bar
                accessSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        progressWindow.updateStatus("Rendering results ...",
                                Integer.MAX_VALUE);
                    }
                });

                // render results
                accessSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        content.setContent(
                                new ResultsView(context, results));
                        progressWindow.close();
                    }
                });
            }


            @Override
            public void onError(final String message, Throwable t) {
                final String desc = (t != null) ? t.getMessage() : null;
                accessSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        setNoResultsView();
                        progressWindow.close();
                        Notification.show(message, desc,
                                Notification.Type.ERROR_MESSAGE);
                    }
                });
            }
        };

        /*
         * now finally submit to tester for running ...
         */
        tester.performTests(listener,
                    profile,
                    endpointURI,
                    searchTerm,
                    strictMode,
                    performProbeRequest,
                    connectTimeout,
                    socketTimeout);
    }


    private void readObject(java.io.ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        setNoResultsView();
    }


    private String getCookie(String name) {
        Cookie[] cookies = VaadinService.getCurrentRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    String s = cookie.getValue();
                    if (s != null) {
                        s = s.trim();
                        if (s.isEmpty()) {
                            s = null;
                        }
                        return s;
                    }
                }
            }
        }
        return null;
    }


    private void setCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(Integer.MAX_VALUE);
        cookie.setPath(VaadinService.getCurrentRequest().getContextPath());
        VaadinService.getCurrentResponse().addCookie(cookie);
    }

    private static final int PROFILE_AUTODETECT  = 1;
    private static final int PROFILE_FCS_1_0     = 2;
    private static final int PROFILE_FCS_2_0     = 3;
    private static final int PROFILE_FCS_LEGACY = 4;

    private static final int TIMEOUT_5_SECONDS   =   5 * 1000;
    private static final int TIMEOUT_10_SECONDS  =  10 * 1000;
    private static final int TIMEOUT_15_SECONDS  =  15 * 1000;
    private static final int TIMEOUT_30_SECONDS  =  30 * 1000;
    private static final int TIMEOUT_60_SECONDS  =  60 * 1000;
    private static final int TIMEOUT_120_SECONDS = 120 * 1000;
    private static final int TIMEOUT_180_SECONDS = 180 * 1000;
    private static final int TIMEOUT_300_SECONDS = 300 * 1000;

    private void fillTimeoutCombo(ComboBox combo) {
        combo.addItem(TIMEOUT_5_SECONDS);
        combo.setItemCaption(TIMEOUT_5_SECONDS, "5 seconds");
        combo.addItem(TIMEOUT_10_SECONDS);
        combo.setItemCaption(TIMEOUT_10_SECONDS, "10 seconds");
        combo.addItem(TIMEOUT_15_SECONDS);
        combo.setItemCaption(TIMEOUT_15_SECONDS, "15 seconds");
        combo.addItem(TIMEOUT_30_SECONDS);
        combo.setItemCaption(TIMEOUT_30_SECONDS, "30 seconds");
        combo.addItem(TIMEOUT_60_SECONDS);
        combo.setItemCaption(TIMEOUT_60_SECONDS, "1 minute");
        combo.addItem(TIMEOUT_120_SECONDS);
        combo.setItemCaption(TIMEOUT_120_SECONDS, "2 minutes");
        combo.addItem(TIMEOUT_180_SECONDS);
        combo.setItemCaption(TIMEOUT_180_SECONDS, "3 minutes");
        combo.addItem(TIMEOUT_300_SECONDS);
        combo.setItemCaption(TIMEOUT_300_SECONDS, "5 minutes");
    }

} // class TesterUI
