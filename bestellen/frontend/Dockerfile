FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /src
COPY package.json package-lock.json ./
# --ignore-scripts: no install scripts from dependencies run during the build.
RUN npm ci --no-audit --no-fund --ignore-scripts
COPY . .
# Unit tests first; the image is only made when they pass.
RUN npx ng test --watch=false
RUN npx ng build

# nginx from the Alpine package (1.30.7 or newer: fixes CVEs in nginx 1.30.6).
# Small base image with a fixed digest; runs as the non-root nginx user on 8080.
FROM alpine:3.24@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf
# Apply Alpine security fixes released after the base image was built.
RUN apk upgrade --no-cache \
 && apk add --no-cache 'nginx>=1.30.7' \
 && rm -rf /etc/nginx/http.d /var/lib/nginx/html
COPY nginx-main.conf /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
# Explicit non-root user (the nginx user of the Alpine package).
USER nginx
EXPOSE 8080
# -e: log to stderr from the very start (the default log path is read-only).
CMD ["nginx", "-e", "/dev/stderr", "-g", "daemon off;"]
