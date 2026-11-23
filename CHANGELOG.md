# Changelog

Wijzigingen van het deelproduct Bestellen, per versie. Formaat: [Keep a Changelog](https://keepachangelog.com/nl/1.1.0/), versies: [semantic versioning](https://semver.org/lang/nl/).

## [Unreleased]

## [1.0.1] - 2026-11-21

### Opgelost
- Een bestelling kwam niet aan bij de bar: typfout in het standaardadres van de bar in de backend (`stub-baar` in plaats van `stub-bar`). Gevonden met het key result op het Dashboard (bar: rood). Een nieuwe unittest controleert de standaardadressen van keuken, bar en kassa. Backend 1.0.1.

## [1.0.0] - 2026-11-21

### Gewijzigd
- Bestellen 1.0.0: de versie waarmee de demo van de showcase Demo begint. Alle containers en stubs op 1.0.0.
- De repository heet `sk-bestellen` (was `sk-demo`): één deelproduct voor alle showcases; elke showcase legt de versie vast die hij gebruikt.
- De adressen van keuken, bar en kassa staan als standaard in de backend; `docker-compose.yml` hoeft ze niet meer mee te geven (met `KITCHEN_URL`, `BAR_URL` en `CASH_REGISTER_URL` kun je ze nog overschrijven).
- End-to-end: een test voor het kernpad (een bestelling plaatsen; de bevestiging, en doorgegeven aan keuken, bar en kassa), te kiezen met `@kernpad`.

### Documentatie
- README: hoe je ShowKitchen start (`fcd up` van een showcase, ook na een herstart), de eerste keer (`fcd demo reset`), status en stoppen.

## [0.4.0] - 2026-11-20

### Gewijzigd
- Gebouwd met de Pipeline van FCD (`fcd-pipeline`); stappen, gates, drempels en afwijkingen staan in de configuratie van de showcase (`fcd-demo/fcd`). `explains.yaml` is weg.
- Poorten en het adres van het Dashboard komen uit de omgeving van de showcase.

### Opgelost
- De meting van het key result zette de showcase vast op `demo`; nu uit de omgeving (`FCD_SHOWCASE`). Gevonden doordat het Dashboard van een andere showcase de logregel weigerde.

## [0.3.0] - 2026-11-20

### Gewijzigd
- Bestellingen van end-to-end tests (header `X-FCD-Test: true`) worden gemeten als test en tellen niet mee voor het key result (contract van de logregel 0.2.0).
- Versheidscontrole: een reden per onderdeel (backend/maven, frontend/npm), zodat een afwijking alleen voor haar eigen onderdeel geldt.

### Opgelost
- De versheidscontrole voor Maven zag niets (door `-q`). Nu gevonden: json-schema-validator een major-versie achter; afwijking EXP-002 tot 2026-12-31 (goedgekeurd door sim007).

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
