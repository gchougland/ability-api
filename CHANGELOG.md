# Changelog

All notable changes to **AbilityAPI** are documented in this file.

## [1.2.0] - 2026-05-26

### Changed

- **Hytale 0.5.0** — Targets Hytale server `^0.5.0` (semver `ServerVersion` in manifest; no longer uses the legacy `YYYY.MM.DD-<sha>` pin).
- **Waterbreathing** — Uses `BreathingCheckEvent` so players with the ability can breathe in fluids under the 0.5 `BreathingComponent` / suffocation pipeline (oxygen-stat top-up alone is insufficient).
- **Math types** — `Vector3d` / block positions use `org.joml` types; rotations use `Rotation3fc` / `Rotation3f` where the server API changed.
- **Punch damage** — Uses `InventoryComponent.getItemInHand` instead of the deprecated `Inventory.getItemInHand()`.

## [1.1.0] - 2026-03-26

### Changed

- **Hytale API compatibility** — Updated for the latest Hytale server release.
- **Stat modifiers** — `AbilityStatService` now triggers stat recalculation via `EntityStatMap.getStatModifiersManager().scheduleRecalculate()` instead of the removed `Player.getStatModifiersManager().setRecalculate(...)` API.
- **Imports** — Removed unused `LivingEntityInventoryChangeEvent` import from `AbilityAPIPlugin` (class no longer exists in the current API). Inventory-related events now use `com.hypixel.hytale.server.core.inventory.InventoryChangeEvent` if you extend the mod with inventory listeners.
