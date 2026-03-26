# Changelog

All notable changes to **AbilityAPI** are documented in this file.

## [1.1.0] - 2026-03-26

### Changed

- **Hytale API compatibility** — Updated for the latest Hytale server release.
- **Stat modifiers** — `AbilityStatService` now triggers stat recalculation via `EntityStatMap.getStatModifiersManager().scheduleRecalculate()` instead of the removed `Player.getStatModifiersManager().setRecalculate(...)` API.
- **Imports** — Removed unused `LivingEntityInventoryChangeEvent` import from `AbilityAPIPlugin` (class no longer exists in the current API). Inventory-related events now use `com.hypixel.hytale.server.core.inventory.InventoryChangeEvent` if you extend the mod with inventory listeners.
