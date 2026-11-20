# Changelog

Wijzigingen van het deelproduct Bestellen, per versie. Formaat: [Keep a Changelog](https://keepachangelog.com/nl/1.1.0/), versies: [semantic versioning](https://semver.org/lang/nl/).

## [Unreleased]

## [0.3.0] - 2026-11-20

### Gewijzigd
- Bestellingen van end-to-end tests (header `X-FCD-Test: true`) worden gemeten als test en tellen niet mee voor het key result (contract van de logregel 0.2.0).
- Versheidscontrole: een reden per onderdeel (backend/maven, frontend/npm), zodat een afwijking alleen voor haar eigen onderdeel geldt.

### Opgelost
- De versheidscontrole voor Maven zag niets (door `-q`). Nu gevonden: json-schema-validator een major-versie achter; afwijking EXP-002 tot 2026-12-31 (goedgekeurd door Johannes).

## [0.2.0] - 2026-11-20

### Toegevoegd
- Key result van de intent Bestellen gemeten: per bestelling of hij is aangekomen bij keuken, bar en kassa. De meting gaat als logregel naar het Dashboard (fcd-demo), asynchroon: de bestelling wacht er niet op en mislukt er niet door.

## [0.1.0] - 2026-11-20

### Toegevoegd
- Bestellen: de gast kiest uit het menu, geeft het tafelnummer op en de bestelling gaat naar keuken, bar en kassa (intent sk-1dp-mvp0).
- Stubs voor keuken, bar en kassa, volgens het contract van de bestelling.
- Unittests, end-to-end tests (Playwright) en een security-scan (Trivy).

### Gewijzigd
- Versies per container in `versions.env` (één bron); de image-tags volgen ze.
- De CI-stappen en quality gates lopen via `fcd` (fcd-demo-pipeline); `build.sh` en `VERSION` zijn weg.
- Contracten: `servers`, licentie en beschrijvingen toegevoegd (lint met Redocly).

### Opgelost
- Frontend: nginx 1.30.7 (kwetsbaarheden in nginx 1.30.6).
- Backend en stubs: een lege bestelling (`null`) of een lege regel gaf een serverfout (500), nu 400 (gevonden door de contracttest).
- Stubs: `placedAt` als getal werd geaccepteerd; volgens het contract is het een datum-tijd als tekst.
