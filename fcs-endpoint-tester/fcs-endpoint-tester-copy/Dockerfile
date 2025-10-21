# ---------------------------------------------------------------------------
FROM maven:3.9.5-eclipse-temurin-8-focal AS builder

WORKDIR /work

# --------------------------------------------------------
# install SNAPSHOT dependencies

RUN git clone --depth 1 https://github.com/clarin-eric/fcs-sru-client.git && \
    cd fcs-sru-client && \
    mvn -q install && \
    cd .. && \
    rm -rf fcs-sru-client

RUN git clone --depth 1 https://github.com/clarin-eric/fcs-simple-client.git && \
    cd fcs-simple-client && \
    mvn -q install && \
    cd .. && \
    rm -rf fcs-simple-client

# --------------------------------------------------------
# build fcs endpoint tester

COPY pom.xml /work/
RUN mvn -B dependency:resolve-plugins
RUN mvn -B dependency:resolve

COPY src /work/src
RUN mvn -B clean package

# ---------------------------------------------------------------------------
FROM jetty:10-jdk17-eclipse-temurin AS jetty

COPY --from=builder /work/target/FCSEndpointTester-*.war /var/lib/jetty/webapps/ROOT.war
