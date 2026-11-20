# sk-demo

Het product ShowKitchen in de showcase Demo: deelproduct Bestellen, met stubs voor keuken, bar en kassa.
Wat voor alle repositories geldt staat in `../CLAUDE.md`. De way of working staat in `../fcd-demo/fcd-demo.md`; stappen, gates, drempels en afwijkingen in de configuratie `../fcd-demo/fcd/`. De intent staat in `sk-1dp-mvp0.md`.

- Deelproduct Bestellen: containers `bestellen-frontend` (Angular) en `bestellen-backend` (Kotlin, Ktor, Maven). Releasen doen we op het deelproduct.
- Stubs `stub-kitchen`, `stub-bar`, `stub-cash-register`: losse containers, niet onderdeel van Bestellen. Ze rollen mee uit, maar worden niet gereleased. De drie stubs hebben dezelfde code: een wijziging gaat drie keer.
- Contract first: `contracts/order.openapi.yaml` (de bestelling naar de stubs) en `bestellen/contracts/` (frontend–backend). Datamodel first: `bestellen/menu/menu.schema.json`.
- Een menu wijzigen gaat via de pipeline (`bestellen/menu/menu.yaml`).
- Elke wijziging: een regel in `CHANGELOG.md`. Een security-actie: ook een regel in `SECURITY.md`, in hetzelfde pull request.
- Bouwen, testen en controleren: `../fcd-pipeline/fcd ci .` (feiten over dit product in `ci/steps.sh`; de rest in de configuratie van de showcase). Versies in `versions.env`, met de hand of `fcd bump`.
