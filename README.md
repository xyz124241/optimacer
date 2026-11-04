# NeptunOptimize

Eigenständiges Performance-Plugin für Paper-Server.

## Features

- **Unendliches Dörfler-Handeln**: Dörfler/Wanderhändler werden nie "ausverkauft", Preise steigen
  nicht durch wiederholtes Handeln.
- **XP-Orb-Zusammenlegen**: nah beieinander liegende XP-Orbs werden zu einem Entity zusammengefasst.
- **Item-Stacking am Boden**: gleiche Items (auch nicht stapelbare wie Werkzeuge/Waffen) werden zu
  einem Entity mit "x15"-Namensschild zusammengefasst. Beim Aufsammeln gibt's trotzdem die volle Menge.
- **Mob-Stacking**: konfigurierbare Mob-Typen (standardmäßig Zombie, Husk, Drowned, Skelett, Stray,
  Wither-Skelett, Spinne, Creeper) werden periodisch zu einem reglosen Mob mit "xN"-Namensschild
  zusammengefasst. Ein tödlicher Treffer zieht nur einen vom Stapel ab, mit normalem Loot/XP.
- **Weniger Kollisions-Rechenlast**: Mobs weit weg von jedem Spieler bekommen ihre Kollisionsabfrage
  abgeschaltet.

## Nicht enthalten

- **Chunk-Vorladen/-Generieren**: dafür bitte das bewährte [Chunky](https://modrinth.com/plugin/chunky)
  installieren, statt das hier nachzubauen.
- **Echtes "Entity Culling"** (Mobs hinter Wänden ausblenden) ist clientseitig und kann ein
  Server-Plugin grundsätzlich nicht leisten.

## Konfiguration

Alles steht in `plugins/NeptunOptimize/config.yml`, mit Kommentaren. Nach Änderungen reicht
`/neptunoptimize` (lädt neu, braucht `neptunoptimize.admin`, Standard: OP).

Einzelne Mobs vom Stacking ausnehmen: `/tag <ziel> add no_stack`.

## Bauen

```
mvn clean package
```

Die fertige Datei liegt danach in `target/NeptunOptimize-1.0.0.jar`. Über GitHub Actions
(`.github/workflows/build.yml`) baut sich das auch automatisch bei jedem Push.
