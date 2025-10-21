# FCS Endpoint Tester

The **SRU**/**CQL**/**FCS** Conformance Tester.

This webapp allows to test compliance of an endpoint with the CLARIN FCS specification. See details at [CLARIN FCS: Technical Details](https://www.clarin.eu/content/federated-content-search-clarin-fcs-technical-details).

## Requirements

- Java 8 (required to build Vaadin webapp)

## Building

### Build and install SRU/FCS dependencies

```bash
git clone git@github.com:clarin-eric/fcs-sru-client.git
cd fcs-sru-client
mvn install
```
```bash
git clone git@github.com:clarin-eric/fcs-simple-client.git
cd fcs-simple-client
mvn install
```

### Build endpoint tester

* Requires Java 8

```bash
mvn clean package
```

## Running

### Jetty

```bash
JETTY_VERSION="9.4.51.v20230217"

# download latest jetty release
wget https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/${JETTY_VERSION}/jetty-distribution-${JETTY_VERSION}.zip
unzip jetty-distribution-${JETTY_VERSION}.zip
rm jetty-distribution-${JETTY_VERSION}.zip

cd jetty-distribution-${JETTY_VERSION}/

# (optional)
java -jar start.jar --add-to-start=http,deploy

# link / add webapp war file
cd webapps/
cp ../../target/FCSEndpointTester-X.Y.Z-SNAPSHOT.war ROOT.war
cd ..

# run
java -jar start.jar
```

Visit http://localhost:8080/.

### Docker

Build the docker image:

```bash
# add --progress=plain to see build output
docker build -t fcs-endpoint-tester .
```

Run container and map local port 8080 to container port 8080. The application will be at http://localhost:8080/.

```bash
docker run --rm -it -p 8080:8080 fcs-endpoint-tester
```
