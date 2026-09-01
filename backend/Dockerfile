# ---- build stage -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /project

# Dependencies first, so edits to src/ do not re-download the world
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package

# The JDBC driver is a runtime artifact of the server, not of the war
RUN mvn -B -ntp dependency:copy \
      -Dartifact=org.postgresql:postgresql:42.7.4 \
      -DoutputDirectory=/jdbc

# ---- runtime stage -----------------------------------------------------
FROM icr.io/appcafe/open-liberty:kernel-slim-java21-openj9-ubi-minimal

COPY --chown=1001:0 src/main/liberty/config/server.xml /config/server.xml

# Installs exactly the features listed in server.xml
RUN features.sh

COPY --from=build --chown=1001:0 /jdbc/postgresql-*.jar /config/lib/postgresql/
COPY --from=build --chown=1001:0 /project/target/kanban-backend.war /config/apps/

# Pre-warms the server (SCC, app metadata) for a faster start
RUN configure.sh

EXPOSE 9080 9443
