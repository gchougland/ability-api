# AbilityAPI – User & Modder Guide

AbilityAPI is a **library mod** that adds a shared set of player abilities (flight, waterbreathing, resistances, movement modifiers, etc.) and a clean way for **server admins** and **other mods** to grant and manage them.

This document explains:

- How to install and configure the mod as a **server admin**
- How to use the **commands** to grant and inspect abilities
- What the **built‑in abilities** do
- How other mods (like **Orbis Origins**) can **integrate** with AbilityAPI

---

## 1. Installation & Requirements

- Drop the **AbilityAPI** JAR into your Hytale server’s `mods` folder.
- Make sure you are using:
  - The same **Hytale server version** as your other mods
  - A Java version compatible with your Hytale tooling (AbilityAPI itself targets Java 25 like Orbis Origins, but Gradle handles the toolchain).

On first run, AbilityAPI will create a data directory similar to:

- `Server/mods/hexvane_AbilityAPI/`
  - `player_abilities.json` — all persisted player abilities
  - `mining_fortune_blocks.json` — configuration for which blocks are affected by `mining_fortune`

No manual configuration is required to get started; defaults are sensible.

---

## 2. Core Concepts

### 2.1 Abilities and values

Each **ability** has:

- A unique **ID** (e.g. `creative_flight`, `waterbreathing`, `move_speed`)
- A **type**:
  - **Binary** (on/off): value is effectively `true`/`false`
  - **Numeric**: value is a number (double)
- Optional **min/max** and a **description** (for help text and validation)

The registry of all abilities is defined in `AbilityAPIPlugin.setup()` and is stable across servers so other mods can depend on the same IDs and behavior.

### 2.2 Player ability storage

Player state is stored centrally in:

- `PlayerAbilityStorage`
  - Per‑player map: `UUID -> (abilityId -> value)`
  - Per‑player map: `UUID -> (abilityId -> List<AbilityConditionSpec>)`
  - Persisted to `player_abilities.json` in the plugin data directory

Any change through commands or via another mod is saved and will survive server restarts.

### 2.3 Conditions

Some abilities are only active when **conditions** are met. Conditions are represented by:

- `AbilityConditionSpec`:
  - `type` (string): one of the predefined constants
  - `param` (int): simple numeric parameter (e.g. health threshold)
  - `zoneIds` (optional list): extra zone IDs for `in_zone`

Built‑in condition types:

- `in_zone` — active when the player is in one of the configured zone IDs
- `in_sunlight` — active when it is daytime and there is open sky above the player
- `health_below` — active when player health % is below `param` (0–100)
- `health_above` — active when player health % is at or above `param` (0–100)
- `target_health_below` — active when the damage **target’s** health % is below `param`
- `target_health_above` — active when the damage **target’s** health % is at or above `param`

AbilityAPI’s internal systems (e.g. `AbilityConditionService`, `AbilityStatService`) evaluate these conditions on demand when applying stats or reacting to events.

---

## 3. Commands for Server Admins

All commands are registered under `/ability`. Exact permission integration depends on your server setup; typically only **ops/admins** should have access to these commands for other players.

### 3.1 Granting abilities

Grant a **binary** ability to yourself:

- `/ability add creative_flight`
- `/ability add waterbreathing`

Grant a **numeric** ability to yourself:

- `/ability add oxygen 10`  
→ +10 “seconds” of breath underwater (internally mapped to extra oxygen units)
- `/ability add move_speed 1.5`  
→ 1.5× movement speed
- `/ability add swim_speed 1.3`
- `/ability add strength 0.25`  
→ 25% more damage dealt

Grant an ability to another player:

- `/ability add <abilityId> <value?> <player>`
- Examples:
  - `/ability add creative_flight Steve`
  - `/ability add oxygen 20 Alice`

Values are validated against each ability’s min/max range; the command will reject out‑of‑range values.

### 3.2 Removing abilities

Remove a specific ability from yourself:

- `/ability remove creative_flight`
- `/ability remove strength`

Remove from another player:

- `/ability remove creative_flight Steve`

### 3.3 Inspecting abilities

List your current abilities:

- `/ability list`

List another player’s abilities:

- `/ability list Steve`

This shows each active ability, its value, and any configured conditions.

### 3.4 Listing available ability IDs

See all registered ability IDs and their descriptions:

- `/ability available`

Use this list as the authoritative source when configuring other mods or writing integration code.

---

## 4. Built‑In Abilities (Overview)

Below is a brief summary of the most important built‑in abilities. See `PLAN.md` for a full design document and internal planning details.

### 4.1 Movement & survival

- `**creative_flight` (binary)**  
  - Enables creative‑style flight for the player.
- `**waterbreathing` (binary)**  
  - Player can breathe underwater indefinitely.
- `**oxygen` (numeric)**  
  - Extra underwater breath. Each point adds more oxygen units to the player’s max oxygen stat (roughly “seconds” of breath, see code comments for exact scaling).
- `**fall_damage_immunity` (binary)**  
  - Completely negates fall damage.
- `**move_speed` (numeric multiplier)**  
  - Modifies base movement speed. `1.0` = normal; `>1` faster, `<1` slower.
- `**swim_speed` (numeric multiplier)**  
  - Multiplier applied when the player is swimming.
- `**wall_climb` (binary)**  
  - Allows climbing solid surfaces via the dedicated `WallClimbSystem`.

### 4.2 Combat & damage

- `**punch_damage` (numeric multiplier)**  
  - Multiplier for unarmed/melee damage.
- `**strength` (numeric multiplier)**  
  - Global damage dealt multiplier. Implemented by `AbilityStrengthSystem`:
    - Damage is multiplied by `(1 + value)`.
    - `value > 0` → extra damage; `value < 0` → reduced damage (weakness).
- `**resistance_<type>` (numeric, -1 to 1)**  
  - Per‑damage‑type resistance or weakness:
    - `0` = neutral
    - `<0` = weakness (take more damage)
    - `>0` = resistance (take less damage)
  - Types are derived from Hytale’s `DamageCause` assets at startup; see `AbilityAPIPlugin.registerResistanceAbilitiesFromDamageCauses()`.
- `**second_chance` (binary)**  
  - Prevents death once, restoring the player to low health with a cooldown. Used for “Undead” style species or special perks.

### 4.3 Utility & quality of life

- `**dark_vision` (binary)**  
  - Grants improved visibility in darkness via a client‑visible effect. (Not entirely happy with it, use at your own discretion)
- `**mining_haste` (numeric level)**  
  - Faster block breaking. Levels typically map to increasing speed (1–5).
- `**mining_fortune` (numeric level)**  
  - Extra drops from certain blocks (e.g. ores). Behavior is configured in `mining_fortune_blocks.json`; higher levels give more extra roll attempts.
- `**item_magnet` (numeric range multiplier)**  
  - Pulls dropped items in from further away. Higher values increase pickup radius.

---

## 5. Conditions in Practice

While commands can set raw abilities, **conditions** are most useful when another mod configures abilities for players.

Examples of how conditions are used (e.g. by Orbis Origins):

- **Zone‑based stamina regen**  
  - Ability: `stamina_regen` (numeric multiplier)  
  - Condition: `in_zone` with `zoneIds = [7, 8, 9, 10, 11]`  
  - Behavior: stamina regen multiplier only applies when the player is in those zones.
- **Photosynthesis health regen**  
  - Ability: `health_regen` (numeric per‑second value)  
  - Condition: `in_sunlight`  
  - Behavior: passive health regen only while standing in sunlight with open sky.
- **Low‑health berserker strength**  
  - Ability: `strength` (e.g. `0.2` = +20% damage)  
  - Condition: `health_below` with `param = 50`  
  - Behavior: bonus damage only when the player’s health is below 50%.
- **Predator’s instinct vs injured targets**  
  - Ability: `strength`  
  - Condition: `target_health_below` with `param = 50`  
  - Behavior: extra damage only when the target’s health is below 50%.

Conditions are created programmatically by other mods using `AbilityConditionSpec` (see §6).

---

## 6. Integrating AbilityAPI from Other Mods

AbilityAPI is designed to be **consumed by other mods** as a library. The recommended integration layer is the public `AbilityService` facade.

### 6.1 Dependency setup

In your consuming mod’s `build.gradle.kts`:

- If AbilityAPI is a sibling project:

```kotlin
dependencies {
    implementation(project(":AbilityAPI"))
}
```

- If you depend on a built JAR:

```kotlin
dependencies {
    implementation(files("./libs/AbilityAPI-1.0.0.jar"))
}
```

Make sure you **gate all runtime usage** behind the Hytale `PluginManager` so your mod can still run when AbilityAPI is missing:

```java
PluginIdentifier abilityApiId = PluginIdentifier.fromString("hexvane:AbilityAPI");
PluginManager manager = PluginManager.get();
boolean abilityApiPresent = manager != null && manager.getPlugin(abilityApiId) != null;
```

### 6.2 Public API: `AbilityService`

Use `com.hexvane.abilityapi.api.AbilityService` from your mod:

```java
import com.hexvane.abilityapi.api.AbilityService;
import com.hexvane.abilityapi.ability.AbilityConditionSpec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
```

Key methods:

- **Grant or update an ability:**

```java
AbilityService.setAbility(playerUuid, "move_speed", 1.3);
AbilityService.setAbility(playerUuid, "creative_flight", Boolean.TRUE);
```

- **Attach conditions:**

```java
List<AbilityConditionSpec> conditions = List.of(
    new AbilityConditionSpec(AbilityConditionSpec.TYPE_IN_ZONE, 7),
    new AbilityConditionSpec(AbilityConditionSpec.TYPE_IN_SUNLIGHT, 0)
);
AbilityService.setConditions(playerUuid, "stamina_regen", conditions);
```

- **Remove an ability:**

```java
AbilityService.removeAbility(playerUuid, "move_speed");
```

- **Re‑apply stats and movement for a player:**

Call this after changing abilities so movement speed, oxygen, and stat modifiers are recalculated:

```java
AbilityService.applyForPlayer(ref, store, world);
```

Parameters:

- `Ref<EntityStore> ref` — reference to the entity
- `ComponentAccessor<EntityStore> store` — the store/accessor from your system/command context
- `World world` — the world the player is in

### 6.3 Example: Species‑based abilities (Orbis Origins)

Orbis Origins is the primary consumer of AbilityAPI and serves as a practical reference:

- Each species JSON defines an `abilities` array with:
  - `id`, `value`, `condition`, `metadata`, `name`, `description`
- When a species is selected:
  - Old species abilities are removed (`AbilityService.removeAbility`)
  - New species abilities are granted (`AbilityService.setAbility`)
  - Conditions are attached via `AbilityConditionSpec`
  - `AbilityService.applyForPlayer` is invoked so stats/movement update immediately

For more detail, see:

- `OrbisOrigins/src/main/java/com/hexvane/orbisorigins/ability/AbilityApiBridge.java`
- `OrbisOrigins/src/main/resources/Species/*.json`

---

## 7. Troubleshooting & Tips

### 7.1 Abilities not applying

- Check `/ability list` to confirm the player actually has the ability.
- Ensure your integration calls `AbilityService.applyForPlayer(...)` after changing abilities.
- Verify that the ability ID is exactly one of the registered IDs from `/ability available`.

### 7.2 Conditions not behaving as expected

- Confirm the **condition type string** matches one of:
- `in_zone`, `in_sunlight`, `health_below`, `health_above`, `target_health_below`, `target_health_above`
- Check that metadata keys are spelled correctly:
  - `zones` for `in_zone`
  - `healthThreshold` (0.0–1.0) for `health_below`
  - `enemyHealthThreshold` (0.0–1.0) for `target_health_below`

### 7.3 Performance considerations

- Avoid spamming ability changes every tick. Grant/remove abilities on discrete events (login, species selection, equipment change) and let AbilityAPI handle the rest.
- Use conditions rather than constantly toggling abilities for state‑based behavior.

---

## 8. Where to Go Next

- **For server admins:**
  - Experiment with `/ability add` and `/ability remove` to give yourself movement or combat perks.
  - Combine AbilityAPI with Orbis Origins to give species‑themed powers.
- **For mod authors:**
  - Use `AbilityService` to centralize any perk/bonus logic instead of re‑implementing movement/health/damage tweaks.
  - Use condition specs to keep your logic data‑driven.

If you extend AbilityAPI or build a mod that uses it, consider mirroring the patterns in Orbis Origins so players get a consistent experience across mods.

If you need more help, have feature requests, or want to share integrations, you can join the AbilityAPI support Discord (see the mod’s download page for an invite link).