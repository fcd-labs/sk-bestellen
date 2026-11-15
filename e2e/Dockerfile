# End-to-end tests. Not deployed; runs in the Docker network of the environment.
FROM mcr.microsoft.com/playwright:v1.65.1-noble@sha256:1507cbfc550a63a6b7396791ca4f00df17beb56342d2447b3f106c306f525579
WORKDIR /e2e
COPY package.json package-lock.json ./
# --ignore-scripts: browsers are already in the image; no install scripts run.
RUN npm ci --no-audit --no-fund --ignore-scripts
COPY playwright.config.ts ./
COPY tests ./tests
# Run as the unprivileged user of the Playwright image.
USER pwuser
ENTRYPOINT ["./node_modules/.bin/playwright", "test"]
