# syntax=docker/dockerfile:1
# Spring Boot extracted layered jar: dependency layers stay cached across builds; only the
# application layer changes on a code-only change.

# --- Build the single deployable from the reactor ---
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace
COPY . .
RUN ./mvnw -q -pl app/shopify-application -am -DskipTests package

# --- Extract layers ---
FROM eclipse-temurin:25-jdk AS extractor
WORKDIR /app
COPY --from=builder /workspace/app/shopify-application/target/shopify-application-*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# --- Runtime image, layers copied most-stable first ---
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=extractor /app/extracted/dependencies/ ./
COPY --from=extractor /app/extracted/spring-boot-loader/ ./
COPY --from=extractor /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor /app/extracted/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
