# syntax=docker/dockerfile:1.7

FROM gradle:8.14-jdk21 AS build

WORKDIR /workspace

COPY --chmod=755 gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties VERSION ./
RUN sed -i 's/\r$//' gradlew
RUN ./gradlew --no-daemon dependencies

COPY src src
RUN ./gradlew --no-daemon clean build -x test

FROM eclipse-temurin:21-jre-noble AS runtime

ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0"

RUN apt-get update \
    && apt-get install --no-install-recommends -y \
        libleptonica-dev \
        libtesseract-dev \
        curl \
        tesseract-ocr \
        tesseract-ocr-por \
        tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 scriptum

WORKDIR /app

COPY --from=build --chown=scriptum:scriptum /workspace/build/quarkus-app/ ./

USER scriptum

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/q/health/ready || exit 1

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]