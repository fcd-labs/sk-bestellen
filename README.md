# sk-demo

Het product **ShowKitchen** in de showcase Demo van [Full Circle Delivery](https://fcd-labs.github.io/fcd/): de gast kiest uit een menu, en de bestelling gaat naar keuken, bar en kassa.

Meer over deze showcase: https://fcd-labs.github.io/fcd/showcases/demo/

| | Wat |
|---|---|
| Deelproduct | **Bestellen**: frontend (Angular) en backend (Kotlin, Ktor) |
| Stubs | keuken, bar en kassa: losse containers, geen onderdeel van Bestellen |
| Intent | [sk-1dp-mvp0.md](sk-1dp-mvp0.md) — key result: elke bestelling van de gast komt aan bij keuken, bar en kassa |
| Product | [showkitchen.md](showkitchen.md) |
| Contracten | [order.openapi.yaml](contracts/order.openapi.yaml), [bestellen-api.openapi.yaml](bestellen/contracts/bestellen-api.openapi.yaml) |
| Menu | [menu.yaml](bestellen/menu/menu.yaml), datamodel [menu.schema.json](bestellen/menu/menu.schema.json) |
| Way of working | [fcd-demo](https://github.com/fcd-labs/fcd-demo) |

## Zelf draaien

Nodig: git, bash en een container-runtime met `docker` en `docker compose`. Alles draait lokaal.
Clone deze repository naast [fcd-demo-pipeline](https://github.com/fcd-labs/fcd-demo-pipeline) en de andere repositories van [fcd-demo](https://github.com/fcd-labs/fcd-demo).

```bash
../fcd-demo-pipeline/fcd check
../fcd-demo-pipeline/fcd ci .
```

`fcd ci` doorloopt de CI-stappen en quality gates (zie [fcd-mvp0.md](https://github.com/fcd-labs/fcd-demo/blob/main/fcd-mvp0.md)). Met `../fcd-demo-pipeline/fcd hooks .` draait hij vóór elke push.
Uitrollen doet alleen de Pipeline, na een merge op `main`. Daarna: http://127.0.0.1:8080.

Versies: [versions.env](versions.env). Afwijkingen op een gate: [explains.yaml](explains.yaml).

Wijzigingen: [CHANGELOG.md](CHANGELOG.md). Security: [SECURITY.md](SECURITY.md). Licentie: [MIT](LICENSE).
