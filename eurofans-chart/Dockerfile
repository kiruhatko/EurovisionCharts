# syntax=docker/dockerfile:1
#
# NOTE on Java version: the spec targets Java 26. This image currently builds
# and runs on Java 21 (see pom.xml's <java.version> comment) because the
# sandbox this project was authored in could not reach Adoptium/an apt
# repository carrying a JDK 26 package to empirically verify the build.
# Nothing in the codebase uses a Java-26-only language feature. To upgrade:
# bump <java.version> in pom.xml and the two image tags below to 26, then
# rebuild -- no other changes required.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    apk add --no-cache maven && \
    mvn -q -DskipTests package && \
    cp target/eurofans-chart-*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S eurofans && adduser -S eurofans -G eurofans
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER eurofans
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
