
# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw

# Cache dependencies
RUN ./mvnw -B dependency:go-offline

COPY src src

RUN ./mvnw -B -DskipTests package

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 10001 --no-create-home appuser

COPY --from=build --chown=10001:10001 \
    /workspace/target/ledger-0.0.1-SNAPSHOT.jar \
    /app/ledger.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/ledger.jar"]
