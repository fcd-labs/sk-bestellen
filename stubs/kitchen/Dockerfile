FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
ARG APP_VERSION=0.0.0-dev
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q -Daether.connector.http.retryHandler.count=5 dependency:go-offline
COPY src ./src
# 'package' runs the unit tests; the image is only made when they pass.
RUN mvn -B -Daether.connector.http.retryHandler.count=5 package -Drevision=${APP_VERSION}

FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
# Apply Alpine security fixes released after the base image was built.
RUN apk upgrade --no-cache
WORKDIR /app
COPY --from=build /src/target/app.jar /app/app.jar
# Run as an unprivileged user, not root.
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
