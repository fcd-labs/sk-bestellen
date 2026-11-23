# sk-bestellen

Het deelproduct **Bestellen** van het product **ShowKitchen**, voor alle showcases van [Full Circle Delivery](https://fcd-labs.github.io/fcd/): de gast kiest uit een menu, en de bestelling gaat naar keuken, bar en kassa.

Elke showcase legt in zijn eigen configuratie vast welke versie hij gebruikt, en werkt op zijn eigen lokale git-server. Zo delen Demo, Classic en Ideal één deelproduct en verschillen ze alleen in hun manier van werken. Eerste showcase: [Demo](https://fcd-labs.github.io/fcd/showcases/demo/).

| | Wat |
|---|---|
| Deelproduct | **Bestellen**: frontend (Angular) en backend (Kotlin, Ktor) |
| Stubs | keuken, bar en kassa: losse containers, geen onderdeel van Bestellen. Visie: elk deelproduct verzorgt een stub van zichzelf; tot keuken, bar en kassa eigen deelproducten zijn, staan hun stubs hier |
| Intent | [sk-1dp-mvp0.md](sk-1dp-mvp0.md) — key result: elke bestelling van de gast komt aan bij keuken, bar en kassa |
| Product | [showkitchen.md](showkitchen.md) |
| Contracten | [order.openapi.yaml](contracts/order.openapi.yaml), [bestellen-api.openapi.yaml](bestellen/contracts/bestellen-api.openapi.yaml) |
| Menu | [menu.yaml](bestellen/menu/menu.yaml), datamodel [menu.schema.json](bestellen/menu/menu.schema.json) |
| Way of working | per showcase, zoals [fcd-demo](https://github.com/fcd-labs/fcd-demo) |

## Zelf draaien

**Randvoorwaarde: Docker** (een container-runtime met `docker` en `docker compose`, zoals Docker Desktop), plus git en bash. Alles draait lokaal, in containers. De eerste keer bouwt de Pipeline alle images; dat kost tijd (enkele minuten), daarna gaat het uit de cache.
Clone deze repository naast [fcd-pipeline](https://github.com/fcd-labs/fcd-pipeline) en de repository van een showcase, zoals [fcd-demo](https://github.com/fcd-labs/fcd-demo).

```bash
../fcd-pipeline/fcd check
../fcd-pipeline/fcd ci .
```

`fcd ci` doorloopt de CI-stappen en quality gates uit de configuratie van de showcase ([fcd-demo/fcd](https://github.com/fcd-labs/fcd-demo/tree/main/fcd)). Met `../fcd-pipeline/fcd hooks .` draait hij vóór elke push.
Uitrollen doet alleen de Pipeline, na een merge op `main`.

## ShowKitchen starten

ShowKitchen start altijd via een showcase: de Pipeline rolt Bestellen en de stubs uit, en `fcd up` start ze weer. Vanuit deze map, met de showcase Demo:

```bash
../fcd-pipeline/fcd up --showcase ../fcd-demo
```

`fcd up` start het Dashboard, het API-register, de Pipeline-agent en de laatst uitgerolde versie van ShowKitchen. Ook na een herstart van de Mac of Docker, want de containers starten niet vanzelf.

| | |
|---|---|
| ShowKitchen | http://127.0.0.1:8080 |
| Stubs keuken, bar, kassa (alleen API) | http://127.0.0.1:8091, 8092, 8093 |
| Dashboard | http://127.0.0.1:8070 |

- **De eerste keer** is er nog niets uitgerold. `../fcd-pipeline/fcd demo reset --showcase ../fcd-demo` rolt de start van de demo uit via de Pipeline (Bestellen 1.0.0); dat duurt een paar minuten.
- **Draait het?** `../fcd-pipeline/fcd status --showcase ../fcd-demo`.
- **Stoppen:** `../fcd-pipeline/fcd down --showcase ../fcd-demo`.

De poorten staan in de configuratie van de showcase (`Environment`).

Versies: [versions.env](versions.env). Afwijkingen op een gate staan in de configuratie van de showcase.

Wijzigingen: [CHANGELOG.md](CHANGELOG.md). Security: [SECURITY.md](SECURITY.md). Licentie: [MIT](LICENSE).
