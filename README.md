# AbilityAPI

A library mod for Hytale that adds a shared set of player abilities. Abilities can be granted and removed via commands. Other mods (e.g. Orbis Origins) can use AbilityAPI as a shared source of abilities.

## Features

- **Creative Flight** — Allow flight in Adventure mode
- **Waterbreathing** — Oxygen bar stays full when underwater
- **Extra Oxygen** — Increases maximum oxygen (numeric value = extra seconds of breath)
- **Fall Damage Immunity** — Immune to fall damage
- **Move Speed** — Movement speed multiplier (numeric, 0.5-3.0)
- **Mining Haste** — Faster block breaking (numeric, level 1-5; stub, requires game API)
- **Resistances/Weaknesses** — Per-damage-type resistance or weakness (e.g. `resistance_physical`, `resistance_fall`; value -1 to 1: 0=normal, positive=resistance, negative=weakness)

## Commands

- `/ability add <ability_id> [value]` — Grant an ability (e.g. `/ability add creative_flight`, `/ability add oxygen 10`)
- `/ability remove <ability_id>` — Remove an ability
- `/ability list` — List your abilities
- `/ability available` — List all available ability IDs and descriptions

## Installation

1. Build the mod: `./gradlew build`
2. Place the JAR from `build/libs/` in your Hytale server plugins directory
3. Restart the server

## Configuration

See [Docs/PLAN.md](Docs/PLAN.md) for the full mod plan and [Docs/TODO.md](Docs/TODO.md) for the implementation checklist.

### Gradle

If you installed Hytale in a non-standard location, create `%USERPROFILE%/.gradle/gradle.properties`:

```properties
hytale.install_dir=path/to/Hytale
hytale.decompile_partial=true
```
