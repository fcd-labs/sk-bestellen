# fcd-mvp0

Way of working voor Claude Code. De eerste, kleinste way of working van Full Circle Delivery (FCD) die de hele cirkel rond gaat: intentie, proces, product, leren. AI als versneller.
Hoort bij een intent, zoals sk-1dp-mvp0.md.

## Organisatie
- Squad: Claude Code met agents. De squad bestaat alleen uit AI-agents.
- Daarom is de product owner eigenaar van het deelproduct en keurt hij afwijkingen goed.

## Kaders
Bij elk kader geldt comply or explain: wijk je af, leg dan uit waarom. De product owner keurt de afwijking goed.

| Kader | Label | Explain toegestaan | Explain |
|---|---|---|---|
| Een mens keurt afwijkingen goed. | ? | N | |
| Wat niet beschreven is, is buiten scope. | | ? | |
| Technische keuzes die niet in de kaders staan, legt Claude Code eerst voor aan de product owner. | | ? | |
| Secure: alles is veilig, van contract en code tot container, pipeline en omgeving. | Security | ? | |
| Containers draaien als gewone gebruiker, niet als root, met zo min mogelijk rechten. | Security | ? | |
| Kleine basis-images met vaste versies. | Security | ? | |
| Poorten alleen open waar nodig. Lokaal alleen bereikbaar via 127.0.0.1. | Security | ? | |
| Elke aanvraag wordt gecontroleerd tegen het contract. Prijzen komen uit het menu, nooit van de gast. | Security | ? | |
| Foutmeldingen geven geen interne details prijs. | Security | ? | |
| De frontend stuurt beveiligingsheaders mee. | Security | ? | |
| build.sh scant afhankelijkheden, images en configuratie op kwetsbaarheden en geheimen. Bij een ernstige bevinding stopt de build. | Security | ? | |
| GitHub Actions: acties op een vaste versie, minimale rechten. | Security | ? | |
| Versleuteld verkeer (HTTPS). | Security | ? | mvp0: lokale omgeving, geen HTTPS (voorstel, nog goed te keuren) |
| Inloggen en autorisatie waar gebruikers zijn. | Security | ? | mvp0: de intent kent geen gebruikersaccounts (voorstel, nog goed te keuren) |
| Contract first: tussen deelproducten eerst het contract, dan de code. Het schema is onderdeel van het contract. Contracten in OpenAPI. | | ? | |
| Datamodel first: binnen een deelproduct eerst het datamodel, dan de code. | | ? | |
| Opbouw: micro-frontend (Angular), UX-API (Kotlin), backend (Kotlin), datastore | Architectuur | ? | mvp0: frontend en backend |
| Database: PostgreSQL | Architectuur | ? | mvp0: geen opslag, het menu is een bestand |
| Containerization | | J | |
| Docker-compose | | ? | |
| Kubernetes | | J | mvp0: docker-compose |
| Omgevingen: OTAP | | ? | mvp0: één omgeving |
| Automate everything | | J | |
| Single source of truth | | J | |
| Everything as code | | J | |
| Declarative | | J | |
| GitOps: alles staat in git, en de pipeline rolt uit | | J | |
| CI/CD – the only way to production, ook bij spoed. | | N | |
| Een bash-script is de enige weg om te bouwen en uit te rollen. | Continuous Delivery | ? | |
| GitHub Actions roept hetzelfde bash-script aan. | Continuous Delivery | ? | |
| Test-driven development (TDD). | Testen | ? | |
| Unittests. | Testen | ? | |
| Integratietests binnen het deelproduct. | Testen | ? | |
| End-to-end tests met Playwright. | Testen | ? | |
| Deze bestanden zijn in het Nederlands. Code is Engels: termen en variabelen in het Engels. | | ? | |
| Het deelproduct is de eenheid van deployment. Het bestaat uit één of meer containers. De pipeline deployt altijd het hele deelproduct. | | ? | |
| Het deelproduct heeft een eigen versie. | | ? | |
| Stubs (zoals voor andere deelproducten) zijn losse containers, niet onderdeel van het deelproduct. | | ? | |
