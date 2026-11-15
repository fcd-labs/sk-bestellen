# sk-1dp-mvp0

Intent voor Claude Code. Showcase van ShowKitchen met één deelproduct, versie nul. Bouw volgens CLAUDE.md (de way of working).

- sk = ShowKitchen: een software as a service (SaaS) product van een leverancier voor restaurants.
- 1dp = één deelproduct.
- mvp0 = versie nul. Simpel houden.

## Intent: Bestellen

1. Persona en userstory: als gast wil ik uit een menu kiezen, zodat ik mijn bestelling kan doorgeven aan de keuken, bar en kassa, zodat ik mijn bestelling krijg.
2. Waarom (objective): de gast kan bestellen.
3. Meten (key result): elke bestelling van de gast komt aan bij keuken, bar en kassa. Zichtbaar: de gast ziet het menu, kiest, en ziet dat de bestelling naar de stubs van keuken, bar en kassa is gestuurd.
4. Gedrag als scenario's:
   - De gast krijgt een menu om te kiezen. Hij kan kiezen, geeft zijn tafelnummer op en zijn bestelling gaat naar keuken, bar en kassa.
   - Gegeven een menu, wanneer de gast kiest en zijn tafelnummer opgeeft, dan gaat de bestelling met het tafelnummer naar keuken, bar en kassa.
   - Gegeven een geplaatste bestelling, dan ziet de gast dat de bestelling naar de stubs van keuken, bar en kassa is gestuurd.

## Deelproduct: Bestellen

1. Verantwoordelijkheid:
   - Wel: het menu tonen, de gast laten kiezen, en de bestelling versturen naar keuken, bar en kassa.
   - Niet: het menu beheren, en betalen.
   - Het menu is een vast, declaratief bestand in het deelproduct zelf, in de repository. Een menu wijzigen gaat via de pipeline.
2. Gebruikers: de gast.
3. Interfaces:
   - Bestellen verstuurt één bestelling, de hele bestelling, rechtstreeks naar keuken, bar en kassa, volgens een contract (contract first). Geen gebeurtenissen.
   - De bestelling bevat het tafelnummer, zodat keuken, bar en kassa weten voor wie de bestelling is.
   - Per gerecht staat in het menu of het in de keuken of aan de bar wordt gemaakt. Elke regel van de bestelling vermeldt dat, zodat keuken en bar zien welke regels voor hen zijn. Ze krijgen nog steeds de hele bestelling.
   - Binnen het deelproduct praten frontend en backend ook volgens een contract in OpenAPI.
4. Intents: Bestellen.
5. Techniek in mvp0 (besluiten van de product owner):
   - Backend in Kotlin met Ktor, gebouwd met Maven. Frontend in Angular.
   - Geen opslag: het menubestand is de enige data. Bestellingen worden niet bewaard; de gast ziet het resultaat in het antwoord.
   - Eén omgeving: lokaal met docker-compose. Een GitHub-repository volgt later.

Keuken, bar en kassa bestaan in deze versie als stubs: losse containers, niet onderdeel van het deelproduct Bestellen. Ze ontvangen de bestelling volgens hetzelfde contract. Elke stub is een eigen Kotlin-project met Ktor, gebouwd met Maven.

## Eerste stap: contract en datamodel

1. Claude Code stelt eerst het contract van de bestelling (OpenAPI), het contract tussen frontend en backend (OpenAPI) en het datamodel van het menu voor, met voorbeelddata voor het menu. Nog geen code.
2. De product owner keurt ze goed. Daarmee heeft de product owner een startsituatie om verder op te bouwen.
3. Pas daarna wordt er gebouwd.
