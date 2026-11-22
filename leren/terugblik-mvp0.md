# Terugblik sk-1dp-mvp0

Leren: de laatste stap van de cirkel (intentie, delivery, product, leren).
Datum: 15 november 2026. Way of working: CLAUDE.md. Intent: sk-1dp-mvp0.md.

## Wat is bereikt

**Key result gehaald.** Elke bestelling van de gast komt aan bij keuken, bar en kassa, en de gast ziet dat.
Bewijs: de drie scenario's uit de intent slagen als end-to-end tests (Playwright), en zijn ook handmatig in de browser gecontroleerd.

| Fase | Resultaat | Commit |
|---|---|---|
| 1. Contract en datamodel | Ordercontract, contract frontend–backend, datamodel menu met voorbeeldmenu. Goedgekeurd door de product owner. | a1a553a |
| 2. Geraamte en pipeline | build.sh als enige weg, docker-compose, GitHub Actions-workflow, security-kaders en Trivy-scan. | a0b0f43, 37d27fe |
| 3. Backend | Kotlin, Ktor, Maven. 18 tests. | f5296b2 |
| 4. Frontend | Angular. 9 tests. Bestelscherm met resultaat per ontvanger. | ad63316 |
| 5. Stubs | Keuken, bar en kassa als losse containers. 3 × 6 tests. | ee12648 |
| 6. End-to-end | 3 scenario's in Playwright, onderdeel van build.sh. | f8440a4 |

## Wat ging goed

- **Contract first werkte.** Na de goedkeuring van fase 1 is het contract niet meer veranderd. Frontend, backend en stubs pasten in één keer op elkaar.
- **TDD.** Elke fase begon met rode tests. Fouten kwamen in de tests boven, niet pas in de browser.
- **Eén script.** `./build.sh all` bouwt, test, scant, rolt uit en draait de end-to-end tests. Er is geen andere weg.
- **Security als kader.** De scan vond echte problemen: een verouderd nginx-image en Alpine-pakketten met bekende kwetsbaarheden. De build stopte, en daarna zijn ze opgelost.
- **Kleine commits per fase**, met wat er geverifieerd is.

## Wat ging minder, en wat we leren

1. **Kaders kwamen laat.** Het security-kader en de keuze voor Maven kwamen tijdens fase 2. Daardoor is werk opnieuw gedaan: van Gradle naar Maven, en nieuwe Dockerfiles.
   *Les:* maak de kaders compleet voordat het bouwen begint, ook buildtool en framework.
2. **Claude Code koos zelf.** Gradle is gekozen zonder het voor te leggen. Dat botst met "wat niet beschreven is, is buiten scope".
   *Les:* elke technische keuze die niet in de kaders staat, eerst voorleggen.
3. **"Explain toegestaan" staat bij veel kaders op "?"**. Claude Code behandelde dat als N. Dat werkte, maar het is niet vastgelegd.
4. **Twee explains zijn nog niet goedgekeurd:** geen HTTPS en geen inloggen in mvp0.
5. **De pipeline heeft nooit in GitHub Actions gedraaid.** Er is nog geen repository. Uitrollen gebeurt lokaal met hetzelfde script. Het kader "GitOps: de pipeline rolt uit" is dus nog niet gehaald.
6. **Wachttijd door downloads.** De eerste builds duurden lang: basis-images, Maven-afhankelijkheden en Playwright (2,8 GB). Maven Central blokkeerde ons ook een keer (te veel verzoeken), en een download brak af. De voortgang was niet altijd zichtbaar.
   *Les:* vooraf melden wat groot is en hoe lang het duurt, en de voortgang laten zien.
7. **De contractcontrole is met de hand geprogrammeerd.** Backend en stubs controleren de regels van het contract in Kotlin, niet tegen het OpenAPI-bestand zelf. Als het contract verandert, kan de code achterlopen.
8. **De contracten zijn niet automatisch gecontroleerd.** De controle met Redocly staat nog op "later".
9. **`apk upgrade` in de images.** Dat lost bekende kwetsbaarheden op, maar twee builds kunnen andere pakketversies krijgen. Het is een bewuste afweging: veilig gaat voor reproduceerbaar.
10. **De drie stubs bevatten dezelfde code**, op verzoek van de product owner. Een wijziging moet dus drie keer.

## Open punten

- Een GitHub-repository aanmaken en de pipeline laten draaien (fase 7).
- De explains voor HTTPS en inloggen goedkeuren of afwijzen.
- "Explain toegestaan" invullen waar nu "?" staat.
- De contracten automatisch controleren (Redocly) in build.sh.
- De controle van backend en stubs koppelen aan het OpenAPI-bestand.

## Voorstellen voor CLAUDE.md en de intent

Deze voorstellen moet de product owner goedkeuren. Pas daarna worden ze verwerkt.

1. Nieuw kader: *Technische keuzes die niet in de kaders staan, legt Claude Code eerst voor aan de product owner.*
2. Nieuw kader: *build.sh controleert de contracten (lint van de OpenAPI-bestanden).*
3. Nieuw kader: *Backend en stubs controleren aanvragen tegen het OpenAPI-contract zelf (contracttests).*
4. Nieuw kader voor de werkwijze: *Bij een lange stap meldt Claude Code vooraf wat er gebeurt en hoe lang het duurt, en laat het de voortgang zien.*
5. "Explain toegestaan" invullen voor alle kaders met "?".
6. De explains voor HTTPS en inloggen goedkeuren of afwijzen.

## Besluiten van de product owner

- Voorstel 1 is goedgekeurd en staat in CLAUDE.md.
- Voorstellen 2, 3 en 4 zijn niet overgenomen.
- Over voorstellen 5 en 6 is nog geen besluit genomen.
- De GitHub-repository volgt later. Tot die tijd rolt build.sh lokaal uit, en blijft het kader "GitOps: de pipeline rolt uit" een open punt.
