# Changelog

Wijzigingen van het deelproduct Bestellen, per versie. Formaat: [Keep a Changelog](https://keepachangelog.com/nl/1.1.0/), versies: [semantic versioning](https://semver.org/lang/nl/).

## [Unreleased]

### Toegevoegd
- Bestellen: de gast kiest uit het menu, geeft het tafelnummer op en de bestelling gaat naar keuken, bar en kassa (intent sk-1dp-mvp0).
- Stubs voor keuken, bar en kassa, volgens het contract van de bestelling.
- Unittests, end-to-end tests (Playwright) en een security-scan (Trivy).

### Opgelost
- Frontend: nginx 1.30.7 (kwetsbaarheden in nginx 1.30.6).
