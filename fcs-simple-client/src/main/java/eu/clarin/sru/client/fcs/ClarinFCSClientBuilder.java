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

import java.util.ArrayList;
import java.util.List;

import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUClientConfig;
import eu.clarin.sru.client.SRUExtraResponseDataParser;
import eu.clarin.sru.client.SRURequestAuthenticator;
import eu.clarin.sru.client.SRUSimpleClient;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.SRUVersion;


/**
 * A class that implements the builder pattern for creating SRU client instances
 * that are configured to be used for CLARIN-FCS.
 *
 */
public class ClarinFCSClientBuilder {
    private static final boolean DEFAULT_UNKNOWN_AS_DOM =
            false;
    private static final SRUVersion DEFAULT_SRU_VERSION =
            SRUVersion.VERSION_1_2;
    private List<DataViewParser> parsers = new ArrayList<>();
    private List<SRUExtraResponseDataParser> extraDataParsers =
            new ArrayList<>();
    private SRUVersion defaultVersion = DEFAULT_SRU_VERSION;
    private boolean unknownAsDom = DEFAULT_UNKNOWN_AS_DOM;
    private boolean legacySupport = false;
    private boolean fullLegacyCompatMode = false;
    private int connectTimeout = SRUClientConfig.DEFAULT_CONNECT_TIMEOUT;
    private int socketTimeout = SRUClientConfig.DEFAULT_SOCKET_TIMEOUT;
    private SRURequestAuthenticator requestAuthStrategy;


    /**
     * Constructor.
     *
     * @param unknownAsDom
     *            if <code>true</code> unknown data views are parsed into a DOM
     *
     */
    public ClarinFCSClientBuilder(boolean unknownAsDom) {
        this.unknownAsDom = unknownAsDom;
    }


    /**
     * Constructor.
     */
    public ClarinFCSClientBuilder() {
        this(DEFAULT_UNKNOWN_AS_DOM);
    }


    /**
     * Add the recommended default set of data record view parsers.
     *
     * @return this {@link ClarinFCSClientBuilder} instance
     */
    public ClarinFCSClientBuilder addDefaultDataViewParsers() {
        doRegisterDataViewParser(parsers, new DataViewParserHits());
        doRegisterDataViewParser(parsers, new DataViewParserAdvanced());
        doRegisterDataViewParser(parsers, new DataViewParserHitsWithLexAnnotations());
        doRegisterDataViewParser(parsers, new DataViewParserLex());
        return this;
    }


    /**
     * Configure client to parse unknown Data Views into a DOM representation.
     *
     * @return this {@link ClarinFCSClientBuilder} instance
     * @see DataViewParserGenericDOM
     * @see DataViewGenericDOM
     */
    public ClarinFCSClientBuilder unknownDataViewAsDOM() {
        unknownAsDom = true;
        return this;
    }


    /**
     * Configure client to parse unknown Data Views into a String
     * representation.
     *
     * @return this {@link ClarinFCSClientBuilder} instance
     * @see DataViewParserGenericString
     * @see DataViewGenericString
     */
    public ClarinFCSClientBuilder unknownDataViewAsString() {
        unknownAsDom = false;
        return this;
    }


    /**
     * Set default SRU version to be used.
     *
     * @param defaultVersion
     *            the default SRU version to be used
     *
     * @return this {@link ClarinFCSClientBuilder} instance
     */
    public ClarinFCSClientBuilder setDefaultSRUVersion(
            final SRUVersion defaultVersion) {
        if (defaultVersion == null) {
            throw new NullPointerException("defaultVersion == null");
        }
        this.defaultVersion = defaultVersion;
        return this;
    }


    /**
     * Configure client to enable support for legacy CLARIN-FCS endpoints.
     *
     * @return this {@link ClarinFCSClientBuilder} instance
     */
    public ClarinFCSClientBuilder enableLegacySupport() {
        legacySupport = true;
        return this;
    }


    /**
     * Configure client to disable support for legacy CLARIN-FCS endpoints.
     *
     * @return this {@link ClarinFCSClientBuilder} instance
     */
    public ClarinFCSClientBuilder disableLegacySupport() {
        legacySupport = false;
        return this;
    }


    /**
     * Configure client to support legacy CLARIN-FCS endpoints in full
     * compatibility mode. Automatically enables support for legacy CLARIN-FCS.
     * <br>
     * NB: This feature should not be used in production!
     *
     * @return this {@link ClarinFCSClientBuilder} instance
     */
    public ClarinFCSClientBuilder enableFullLegacyCompatMode() {
        enableLegacySupport();
        fullLegacyCompatMode = true;
        return this;
    }


    /**
     * Configure client to disable support for legacy CLARIN_FCS in full
     * compatibility mode. <br>
     * NB: Support for legacy CLARIN-FCS will <em>not</em> be disabled
     * automatically!
     *
     * @return this {@link ClarinFCSClientBuilder} instance
     */
    public ClarinFCSClientBuilder disableFullLegacyCompatMode() {
        fullLegacyCompatMode = false;
        return this;
    }


    /**
     * Get the timeout in milliseconds until a connection is established.
     *
     * @return this connect timeout in milliseconds
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }


    /**
     * Set the timeout in milliseconds until a connection is established.
     * <p>
     * A timeout value of <code>0</code> is interpreted as an infinite timeout;
     * <code>-1</code> is interpreted as system default.
     * </p>
     *
     * @param connectTimeout
     *            the connect timeout in milliseconds
     * @return this {@link ClarinFCSClientBuilder} instance
     */
    public ClarinFCSClientBuilder setConnectTimeout(int connectTimeout) {
        if (connectTimeout < -1) {
            throw new IllegalArgumentException("connectTimeout < -1");
        }
        this.connectTimeout = connectTimeout;
        return this;
    }


    /**
     * Get the socket timeout (<code>SO_TIMEOUT</code>) in milliseconds,
     * which is the timeout for waiting for data.
     *
     * @return socketTimeout
     *            the socket timeout in milliseconds
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }


    /**
     * Set the socket timeout (<code>SO_TIMEOUT</code>) in milliseconds, which
     * is the timeout for waiting for data.
     * <p>
     * A timeout value of <code>0</code> is interpreted as an infinite timeout;
     * <code>-1</code> is interpreted as system default.
     * </p>
     *
     * @param socketTimeout
     *            the socket timeout in milliseconds
     * @return this {@link ClarinFCSClientBuilder} instance
     */
    public ClarinFCSClientBuilder setSocketTimeout(int socketTimeout) {
        if (socketTimeout < -1) {
            throw new IllegalArgumentException("socketTimeout < -1");
        }
        this.socketTimeout = socketTimeout;
        return this;
    }


    public ClarinFCSClientBuilder setRequestAuthenticator(
            SRURequestAuthenticator requestAuthStrategy) {
        this.requestAuthStrategy = requestAuthStrategy;
        return this;
    }


    /**
     * Register a Data View parser.
     *
     * @param parser
     *            the data view parser to be registered
     * @return this {@link ClarinFCSClientBuilder} instance
     * @throws IllegalArgumentException
     *             if an error occurred while registering the data view parser
     * @see DataViewParser
     */
    public ClarinFCSClientBuilder registerDataViewParser(DataViewParser parser) {
        if (parser == null) {
            throw new NullPointerException("parser == null");
        }
        if ((parser instanceof DataViewParserGenericDOM) ||
                (parser instanceof DataViewParserGenericString)) {
            throw new IllegalArgumentException("parsers of type '" +
                    parser.getClass().getName() +
                    "' cannot be added manually");
        }

        if (!doRegisterDataViewParser(parsers, parser)) {
            throw new IllegalArgumentException("parser of type '" +
                    parser.getClass().getName() + "' was already registered");
        }
        return this;
    }


    /**
     * Register an extra response data parser.
     *
     * @param parser
     *            the extra response data parser to be registered
     * @return this {@link ClarinFCSClientBuilder} instance
     * @throws IllegalArgumentException
     *             if an error occurred while registering the extra response
     *             data parser
     * @see SRUExtraResponseDataParser
     */
    public ClarinFCSClientBuilder registerExtraResponseDataParser(
            SRUExtraResponseDataParser parser) {
        if (parser == null) {
            throw new NullPointerException("parser == null");
        }
        extraDataParsers.add(parser);
        return this;
    }


    /**
     * Create a {@link SRUSimpleClient} instance.
     *
     * @return a configured {@link SRUSimpleClient} instance
     */
    public SRUSimpleClient buildSimpleClient() {
        return new SRUSimpleClient(makeClientConfig());
    }


    /**
     * Create a {@link SRUClient} instance.
     *
     * @return a configured {@link SRUClient} instance
     */
    public SRUClient buildClient() {
        return new SRUClient(makeClientConfig());
    }


    /**
     * Create a {@link SRUThreadedClient} instance.
     *
     * @return a configured {@link SRUThreadedClient} instance
     */
    public SRUThreadedClient buildThreadedClient() {
        return new SRUThreadedClient(makeClientConfig());
    }


    @SuppressWarnings("deprecation")
    private SRUClientConfig makeClientConfig() {
        final SRUClientConfig.Builder builder = new SRUClientConfig.Builder();
        builder
            .setDefaultVersion(defaultVersion)
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(socketTimeout)
            .setRequestAuthenticator(requestAuthStrategy);
        final List<DataViewParser> p = finalizeDataViewParsers();
        builder.addRecordDataParser(new ClarinFCSRecordDataParser(p));
        if (legacySupport) {
            builder.addRecordDataParser(new LegacyClarinFCSRecordDataParser(p, fullLegacyCompatMode));
        }
        if ((extraDataParsers != null) && !extraDataParsers.isEmpty()) {
            for (SRUExtraResponseDataParser parser : extraDataParsers) {
                builder.addExtraResponseDataParser(parser);
            }
        }
        return builder.build();
    }


    @SuppressWarnings("deprecation")
    private List<DataViewParser> finalizeDataViewParsers() {
        final List<DataViewParser> result =
                new ArrayList<>(parsers.size() +
                        (legacySupport ? 2 : 1));
        result.addAll(parsers);
        if (unknownAsDom) {
            result.add(new DataViewParserGenericDOM());
        } else {
            result.add(new DataViewParserGenericString());
        }
        if (legacySupport) {
            result.add(new DataViewParserKWIC(fullLegacyCompatMode));
        }
        return result;
    }


    private static boolean doRegisterDataViewParser(
            List<DataViewParser> parsers, DataViewParser parser) {
        if (parsers.contains(parser)) {
            return false;
        } else {
            parsers.add(parser);
            return true;
        }
    }

} // class ClarinFCSClientBuilder
