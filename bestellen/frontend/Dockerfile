FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /src
COPY package.json package-lock.json ./
# --ignore-scripts: no install scripts from dependencies run during the build.
RUN npm ci --no-audit --no-fund --ignore-scripts
COPY . .
# Unit tests (CI step 2). The report is kept; the result is checked in the 'checked' stage,
# so the image is only made when all tests pass.
RUN mkdir -p /reports && (npx ng test --watch=false --reporters=default --reporters=junit --output-file=/reports/TEST-frontend.xml || true)
RUN npx ng build

# Test reports only: exported by the pipeline (docker build --target test-reports --output ...).
FROM scratch AS test-reports
COPY --from=build /reports/ /

FROM build AS checked
RUN test -s /reports/TEST-frontend.xml && ! grep -qE '<(failure|error)[ >]' /reports/TEST-frontend.xml

# nginx from the Alpine package (1.30.7 or newer: fixes CVEs in nginx 1.30.6).
# Small base image with a fixed digest; runs as the non-root nginx user on 8080.
FROM alpine:3.24@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf
# Apply Alpine security fixes released after the base image was built.
RUN apk upgrade --no-cache \
 && apk add --no-cache 'nginx>=1.30.7' \
 && rm -rf /etc/nginx/http.d /var/lib/nginx/html
COPY nginx-main.conf /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=checked /src/dist/frontend/browser /usr/share/nginx/html
# Explicit non-root user (the nginx user of the Alpine package).
USER nginx
EXPOSE 8080
# -e: log to stderr from the very start (the default log path is read-only).
CMD ["nginx", "-e", "/dev/stderr", "-g", "daemon off;"]
