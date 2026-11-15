FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /src
COPY package.json package-lock.json ./
# --ignore-scripts: no install scripts from dependencies run during the build.
RUN npm ci --no-audit --no-fund --ignore-scripts
COPY . .
# Unit tests first; the image is only made when they pass.
RUN npx ng test --watch=false
RUN npx ng build

# Unprivileged nginx: runs as a non-root user and listens on 8080.
FROM nginxinc/nginx-unprivileged:1.30.6-alpine@sha256:f5116ed4606f3eb015dd4a8e28cb9b351476dc0ad4bc6e9f93b19cec130cdf03
# Apply Alpine security fixes released after the base image was built.
USER root
RUN apk upgrade --no-cache
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
# Explicit non-root user (the nginx user of this image).
USER 101
EXPOSE 8080
