# ---- Stage 1: build the jar (tests already ran in the CI pipeline) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests -Djacoco.skip=true package

# ---- Stage 2: small runtime image ----
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home spring
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER spring
EXPOSE 8080
# All configuration comes from environment variables (DB_URL, DB_USERNAME, DB_PASSWORD, KEYCLOAK_*, ...)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]