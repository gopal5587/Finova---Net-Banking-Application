# syntax=docker/dockerfile:1

# ---- Build stage ----
# Uses a Maven + Temurin 21 image so the host needs no local Maven/JDK.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first: copy only the POM, resolve, then copy sources.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user to reduce container attack surface.
RUN groupadd --system finova && useradd --system --gid finova finova
USER finova

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
# Container-aware JVM flags; heap sizing left to the container memory limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
