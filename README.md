# AbilityAPI

AbilityAPI is a **library mod for Hytale** that provides a shared set of player abilities (flight, waterbreathing, resistances, movement and combat modifiers, etc.) plus a clean API and command set to manage them.

- **Server admins** can grant and remove abilities via `/ability` commands.
- **Other mods** (e.g. Orbis Origins) can depend on AbilityAPI to give players consistent, reusable gameplay perks without re‑implementing the logic.

For a full usage guide (commands, ability list, and integration examples), see:  
➡️ `[Docs/USAGE.md](Docs/USAGE.md)`

---

## Features

### Core systems

- **Ability registry** – central list of all ability IDs, types, default values, min/max, and descriptions.
- **Persistent player storage** – per‑player ability state is saved to `player_abilities.json` and automatically re‑applied on login/server restart.
- **Condition system** – supports context‑sensitive abilities via `AbilityConditionSpec` (e.g. `in_zone`, `in_sunlight`, `health_below`, `target_health_below`, and their `*_above` variants).
- **ECS‑based handlers** – movement, stats, combat, and damage effects are implemented as Hytale entity systems (e.g. `CreativeFlightSystem`, `WaterbreathingSystem`, `AbilityStrengthSystem`, `AbilityDamageResistanceSystem`, `AbilityStatService`).

### Built‑in abilities (high level)

Movement & survival:

- `creative_flight` (binary) – creative‑style flight.
- `waterbreathing` (binary) – oxygen bar stays full underwater.
- `oxygen` (numeric) – extra underwater breath (seconds scaled internally).
- `fall_damage_immunity` (binary) – immune to fall damage.
- `move_speed` (numeric multiplier) – modifies base walk speed.
- `swim_speed` (numeric multiplier) – modifies swim speed.
- `wall_climb` (binary) – climb solid surfaces.

Combat & damage:

- `punch_damage` (numeric multiplier) – unarmed/melee damage multiplier.
- `strength` (numeric multiplier) – global damage dealt multiplier (applies via `AbilityStrengthSystem`).
- `resistance_<type>` (numeric, ‑1 to 1) – per‑damage‑type resistance/weakness (0 = neutral, >0 = resistance, <0 = weakness).
- `second_chance` (binary) – prevent death once; restore to low health with cooldown.

Utility & mining:

- `dark_vision` (binary) – improved visibility in darkness (visual effect).
- `mining_haste` (numeric level) – faster block breaking (levels 1–5).
- `mining_fortune` (numeric level) – extra drops from configured blocks (e.g. ores) based on `mining_fortune_blocks.json`.
- `item_magnet` (numeric multiplier) – pulls dropped items from further away.

### Mod integration

- **Public API facade**: `com.hexvane.abilityapi.api.AbilityService`
  - `setAbility(UUID playerId, String abilityId, Object value)`
  - `setConditions(UUID playerId, String abilityId, List<AbilityConditionSpec> conditions)`
  - `removeAbility(UUID playerId, String abilityId)`
  - `applyForPlayer(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, World world)`
- **Condition types** via `AbilityConditionSpec`:
  - `in_zone`, `in_sunlight`, `health_below`, `health_above`, `target_health_below`, `target_health_above`.
- **Example consumer**: Orbis Origins uses AbilityAPI to grant species‑themed abilities (e.g. Kweebec “Photosynthesis”, Goblin “Item Magnet”, Tuluk/Fen Stalker water abilities, Trork/Saurian strength effects).

See `[Docs/USAGE.md](Docs/USAGE.md)` for detailed examples and code snippets.

---

## Commands

All commands are registered under `/ability`:

- `/ability add <ability_id> [value] [extra args…]`  
  - Grant a binary or numeric ability to yourself (or another player when run as them) and optionally attach **conditions** via extra arguments parsed by `AbilityAddCommand`.  
  - Basic examples:
    - `/ability add creative_flight`
    - `/ability add oxygen 10`
    - `/ability add move_speed 1.5`
- `/ability remove <ability_id>`  
  - Remove an ability from yourself.
- `/ability list`  
  - List your current abilities (and values).
- `/ability available`  
  - List all registered ability IDs and their descriptions.

### Example: zone‑based stamina regen

To grant **stamina_regen** that only applies in specific zones, you can attach an `in_zone` condition via the `zone` keyword and a list of zone IDs:

- `/ability add stamina_regen 2.0 zone 12 13 14 15 16 17 18 19 20`  
  → sets `stamina_regen = 2.0` and adds an `in_zone` condition restricting it to zones 12–20.

Other supported condition arguments:

- `sunlight` / `in_sunlight` – add an `in_sunlight` condition.
- `health_below <percent>` / `health_above <percent>` – player health % threshold.
- `target_health_below <percent>` / `target_health_above <percent>` – target’s health % threshold for damage‑based abilities.

See `[Docs/USAGE.md](Docs/USAGE.md)` for full command semantics, condition types, and integration examples.

---

## Installation

1. Place the JAR in your Hytale server `mods`/plugins directory
2. Restart the server

---

## Support

If you need more help, have feature requests, or want to share integrations, you can join the AbilityAPI support Discord (see the mod’s download page for an invite link).