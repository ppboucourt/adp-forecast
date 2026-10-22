# Build stage: also used by `make test` and `make lint`.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
# Resolve dependencies in their own layer so source edits don't re-download them.
RUN ./gradlew --no-daemon --quiet dependencies > /dev/null
COPY src src
COPY data data
RUN ./gradlew --no-daemon --quiet bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/adp-forecast.jar app.jar
COPY data data
VOLUME /data
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
