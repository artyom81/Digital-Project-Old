# Changelog

# [2.6.0](https://github.com/clarin-eric/fcs-simple-client/releases/tag/FCSSimpleClient-2.6.0) - 2025-06-17

- Dependencies:
  - Bump [`sru-client`](https://github.com/clarin-eric/fcs-sru-client) to `2.5.0`

# [2.5.2](https://github.com/clarin-eric/fcs-simple-client/releases/tag/FCSSimpleClient-2.5.2) - 2025-05-26

- Fixed:
  - Allow empty Value elements when parsing the Lex Data View. It is not recommended but might have a use case.
  - Typo in log message.
  - Strict Java 8 compile check in `pom.xml`.
  - Rename Lex Field Type enum constant `CIT` to `CITATION` for the value `"citation"`.

# [2.5.1](https://github.com/clarin-eric/fcs-simple-client/releases/tag/FCSSimpleClient-2.5.1) - 2025-04-08

- Fixed:
  - Change to case-correct `idRefs` attribute name in Lex field value.

# [2.5.0](https://github.com/clarin-eric/fcs-simple-client/releases/tag/FCSSimpleClient-2.5.0) - 2025-04-04

- Fixes:
  - Add `lex` query type constant value

- Dependencies:
  - Bump [`sru-client`](https://github.com/clarin-eric/fcs-sru-client) to `2.4.0`

# [2.4.0](https://github.com/clarin-eric/fcs-simple-client/releases/tag/FCSSimpleClient-2.4.0) - 2025-04-02

- Additions:
  - Add support for the Lexical FCS ("LexFCS") extension:
    - Default Data View parsers for `DataViewParserHitsWithLexAnnotations` (extended `DataViewParserHits` with higher priority) and `DataViewParserLex` (official LexFCS Data View, `application/x-clarin-fcs-lex+xml`)
    - New capability URL: `CAPABILITY_LEX_SEARCH` (`http://clarin.eu/fcs/capability/lex-search`)
    - (optional) parsing support in endpoint description for `<SupportedLexFields>` in endpoints and `<AvailableLexFields>` in resources
    - POJOs for Data Views and `LexField`
    - **NOTE** _This is preliminary support for the new LexFCS specification extension, minor details might be changed in the future._

- Fixes:
  - Small fixes for older copy-paste errors in log messages, comments
  - Adding missing logging statements

# [2.3.0](https://github.com/clarin-eric/fcs-simple-client/releases/tag/FCSSimpleClient-2.3.0) - 2025-04-02

- Additions:
  - Add a more "secure" `withKeyPair(RSAPublicKey, RSAPrivateKey)` setter to `ClarinFCSRequestAuthenticator.Builder`. Users should use the `KeyReaderUtils.read*Key()` methods themselves that the other `withKeyPair*()` convenience methods use internally.

- Changes:
  - **BREAKING** Rename `withKeyPairStrings` to `withKeyPairContents` of `ClarinFCSRequestAuthenticator.Builder`

- Dependencies:
  - Bump `org.bouncycastle` to `1.80`
  - Bump `com.auth0:java-jwt` to `4.5.0`
  - Bump `org.slf4j` to `2.0.17`
  - Bump Maven build plugin versions
  - Bump Github Actions CI script actions versions

# [2.2.0](https://github.com/clarin-eric/fcs-simple-client/releases/tag/FCSSimpleClient-2.2.0) - 2024-12-06

- Additions:
  - **BREAKING** Add `<AvailabilityRestriction>` element to `<Resource>`s in `<EndpointDescription>`. (after `<Languages>` element)
  - Add `AvailabilityRestriction` enum to `ResourceInfo` class
  - Add `CAPABILITY_AUTHENTICATED_SEARCH` capability URL constant
  - Add function to add public/private RSA keys to `ClarinFCSRequestAuthenticator.Builder` using content strings (besides files)

- Changes:
  - Move public/private RSA PEM key readers to `KeyReaderUtils` class
  - Add `context` map to all `ClarinFCSRequestAuthenticator` functions
  - Add @Deprecated decorators to streaming `ClarinFCSEndpointDescriptionParser` functions  
    (reason: less robust against future changes and unexpected elements)

- Dependencies:
  - Bump `sru-client` to `2.3.0`
  - Add `maven-release-plugin`
  - Bump Maven build plugin versions
  - Bump `org.slf4j` to `2.0.16`
  - Bump `org.bouncycastle:bcprov-ext-jdk15on` to `org.bouncycastle:bcprov-jdk18on:1.79`
  - Bump `com.auth0:java-jwt` to `4.4.0`

# [2.1.1](https://github.com/clarin-eric/fcs-simple-client/releases/tag/FCSSimpleClient-2.1.1) - 2024-02-02

- Changes:
  - `<EndpointDescription>`: pass `<Resource>`s that have missing required `xml:lang="en"` elements
  - Change log levels to `warn` for specification violations
  - Add more debug logs for `<EndpointDescription>` parsing
  - Refactor code

- General:
  - Update `pom.xml` description

- Dependencies:
  - Bump `sru-client` to `2.2.1`
  - Add `maven-release-plugin`
  - Bump Maven build plugin versions
  - Bump `org.slf4j` to `1.7.36`
  - Bump `com.auth0:java-jwt` to `3.19.4`

# [2.1.0](https://github.com/clarin-eric/fcs-simple-client/releases/tag/FCSSimpleClient-2.1.0) - 2024-02-01

- Additions:
  - Add `<Institution>` element to `<Resource>`s in `<EndpointDescription>`.
  - Add [Github Pages](https://clarin-eric.github.io/fcs-simple-client/) with [JavaDoc](https://clarin-eric.github.io/fcs-simple-client/project-reports.html)
  - Add Changelog document
  
  * Add convenience for checking for checking type
  * Add utility class to auto-detect the FCS versionn supported by an endpoint
  * Add _experimental_ basic support for FCS authentication

- Bug Fixes
  - fix NPE when parsing malformed FCS 2.0 endpoint descriptions

- Changes:
  * Change to XPath based `<EndpointDescription>` parsing in `ClarinFCSEndpointDescriptionParser`.
    This means that the whole XML subtree will be loaded in memory compared to the prior stream based parsing and it requires more time and memory.
    It however also allows to better handle _broken_ Endpoint Descriptions, e.g., due to spec updates and newer/older library versions.
    Switching to the older stream based parsing is still possible and might be necessary for some use-cases (too many resources on endpoints)

  - Emit debugging output of parsed capabilities
  - Be strict when parsing FCS 2.0 endpoint descriptions
  - Add some useful constants
  - Move some constants to ClarinFCSConstants to make them public
  - More complete legacy compat mode (to be used in FCS conformance tester)

- Dependencies:
  - Update to latest (`2.2.0`) [`fcs-sru-client`](https://github.com/clarin-eric/fcs-sru-client) library
  - SLF4j to `1.7.32`
  - Bump Maven build plugin versions

- General:
  - Cleanup, whitespaces, copyright

For older changes, see commit history at [https://github.com/clarin-eric/fcs-simple-client/commits/main/](https://github.com/clarin-eric/fcs-simple-client/commits/main/?after=6055b0d7327ceed0c9a56dd51dcd1d077088ff8c+0&branch=main&qualified_name=refs%2Fheads%2Fmain)