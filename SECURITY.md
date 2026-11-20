# Security

Meld een kwetsbaarheid via **Security → Report a vulnerability** in deze repository (privé). Maak er geen openbaar issue van. Je krijgt binnen een week antwoord.

## Log

| Datum | Actie |
|---|---|
| 2026-11-20 | Versheidscontrole voor Maven hersteld (zag niets door -q). Goedgekeurde afwijking EXP-002 op de securitygate tot 2026-12-31: json-schema-validator een major-versie achter (vereist Jackson 3). EXP-001 geldt alleen nog voor de frontend. Bestellen (volgende versie). |
| 2026-11-20 | Goedgekeurde afwijking EXP-001 op de securitygate tot 2026-12-31: frontend-libraries een major-versie achter (Angular 21, TypeScript 5.9, vitest 4, jsdom 28). Bestellen 0.1.0. |
| 2026-11-20 | nginx bijgewerkt van 1.30.6 naar 1.30.7 (4 kritieke en 2 hoge kwetsbaarheden). Scan van repository en images: geen bekende kwetsbaarheden. Bestellen 0.1.0 (nog niet gereleased). |
