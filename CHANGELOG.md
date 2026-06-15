# Changelog

All notable changes to **AbilityAPI** are documented in this file.

## [1.2.2] - 2026-06-15

### Fixed

- **Modify Abilities of Others** Fixed issue where the player can't modify the abilities of other players.

## [1.2.1] - 2026-04-1

### Fixed

- **Damage ability ordering** — `FallDamageImmunitySystem`, `InvulnerabilitySystem`, `SecondChanceSystem`, and `HealthRegenDelayRecordSystem` now depend on running **after** `DamageCalculatorSystems.SequenceModifier` and **before** `DamageSystems.ApplyDamage`. Previously they only ran after the filter group with no edge to `ApplyDamage`, so the scheduler could run them **after** health was already subtracted, making immunity and fall protection unreliable.

## [1.2.0] - 2026-03-31

### Added

- **`invulnerability` (binary)** — `InvulnerabilitySystem` sets incoming entity damage to zero when the ability is active (registered after `FallDamageImmunitySystem`, before `SecondChanceSystem`).
- **Mining fortune** — `Ore_Thorium_Mud` added to the fortune block config (`mining_fortune_blocks.json`).

## [1.1.0] - 2026-03-26

### Changed

- **Hytale API compatibility** — Updated for the latest Hytale server release.
- **Stat modifiers** — `AbilityStatService` now triggers stat recalculation via `EntityStatMap.getStatModifiersManager().scheduleRecalculate()` instead of the removed `Player.getStatModifiersManager().setRecalculate(...)` API.
- **Imports** — Removed unused `LivingEntityInventoryChangeEvent` import from `AbilityAPIPlugin` (class no longer exists in the current API). Inventory-related events now use `com.hypixel.hytale.server.core.inventory.InventoryChangeEvent` if you extend the mod with inventory listeners.
