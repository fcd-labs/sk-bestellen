# Build context: bestellen/ (so the menu file is part of the image).
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
ARG APP_VERSION=0.0.0-dev
# Same layout as the repository: the tests read ../menu/menu.yaml.
WORKDIR /work/backend
COPY backend/pom.xml .
RUN mvn -B -q -Daether.connector.http.retryHandler.count=5 dependency:go-offline
COPY menu /work/menu
COPY backend/src ./src
# Unit tests (CI step 2) run in 'package'. Reports are kept; the result is checked in the
# 'checked' stage, so the image is only made when all tests pass.
RUN mvn -B -Daether.connector.http.retryHandler.count=5 package -Drevision=${APP_VERSION} -Dmaven.test.failure.ignore=true

# Test reports only: exported by the pipeline (docker build --target test-reports --output ...).
FROM scratch AS test-reports
COPY --from=build /work/backend/target/surefire-reports/ /

FROM build AS checked
RUN ls target/surefire-reports/TEST-*.xml >/dev/null && ! grep -qE '<(failure|error)[ >]' target/surefire-reports/TEST-*.xml

FROM eclipse-temurin:25-jre-alpine@sha256:e1898fd6a11fc965a994d8ace186fd3585aeb636cf8f05f47a723fcfee0b0918
# Apply Alpine security fixes released after the base image was built.
RUN apk upgrade --no-cache
WORKDIR /app
COPY --from=checked /work/backend/target/app.jar /app/app.jar
# The menu is a fixed, declarative file; changing it goes through the pipeline.
COPY menu/menu.yaml /app/menu/menu.yaml
# Run as an unprivileged user, not root.
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
