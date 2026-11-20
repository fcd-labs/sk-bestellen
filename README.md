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

```bash
./build.sh all
```

Daarna: http://127.0.0.1:8080. Dit wordt het startcommando van fcd-demo (volgt).

Wijzigingen: [CHANGELOG.md](CHANGELOG.md). Security: [SECURITY.md](SECURITY.md). Licentie: [MIT](LICENSE).
