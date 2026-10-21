# syntax=docker/dockerfile:1

# ---- Build: compile and package the Spring Boot jar ---------------------------
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

# Wrapper and build scripts first: dependencies are downloaded once and cached between builds.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon --quiet dependencies > /dev/null

COPY src src
# Tests run in CI (they need Docker for Testcontainers), not in the image build.
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon --quiet bootJar -x test \
    && java -Djarmode=tools -jar build/libs/app.jar extract --layers --destination build/extracted

# ---- Run: JRE only, one layer per kind of change ------------------------------
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

# Least to most frequently changed, so an app-only change rebuilds a small layer.
COPY --from=build /workspace/build/extracted/dependencies/ ./
COPY --from=build /workspace/build/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/build/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/build/extracted/application/ ./

USER app
EXPOSE 8080
# Size the heap from the container's memory limit; restart (via Docker) rather than limp on after an OOM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
