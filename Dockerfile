# Stage 1: Build
FROM gradle:8.11-jdk17 AS builder

WORKDIR /app

# Copy Gradle files first for caching
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle/wrapper ./gradle/wrapper
COPY gradle/libs.versions.toml ./gradle/libs.versions.toml

# Copy source code
COPY src ./src

# Build the application
RUN gradle buildFatJar --no-daemon -x test

# Stage 2: Run
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*-all.jar app.jar
COPY --from=builder /app/build/resources/main/application.yaml application.yaml
COPY --from=builder /app/build/resources/main/openapi openapi

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "-config", "application.yaml"]
