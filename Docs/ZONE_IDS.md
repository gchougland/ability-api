# Zone IDs and Ability Conditions

AbilityAPI’s **in_zone** condition uses the **numeric zone id** returned by Hytale’s worldgen at the player’s position. One “logical” zone (e.g. Zone 3 / Borea) can be made up of **multiple zone ids** (e.g. 7, 19, 20), so you can list several ids for one condition.

## Where zone IDs come from

The numeric zone id returned at runtime is the **position (index) in the `MaskMapping` array** in the world’s zone config.

- **Source file (default world):** `Assets/Server/World/Default/Zones.json`
- **`MaskMapping`** is an object mapping hex colors to zone name(s). The **order of keys** in that object defines the zone id: first key = id 0, second = id 1, and so on. So the id is the index of that entry in `MaskMapping`, not a value stored in the JSON.
- **At runtime**: the game uses the world’s **ChunkGenerator** and **ZoneBiomeResult** at the player’s block position to resolve which mask/color applies; that maps to the same index, which AbilityAPI logs as **currentZone**.

So to know which ids belong to “Zone 3”, you can either (1) count the position of the relevant entries in `Zones.json`’s `MaskMapping`, or (2) use the **currentZone** values printed in AbilityAPI’s logs as you walk the area and list those ids in the condition.

## Using multiple zone IDs

Use **one** in_zone condition with **several ids** so the ability is active in any of those zones:

```text
/ability add stamina_regen 1.5 zone 7 19 20
```

That enables the bonus whenever **currentZone** is 7, 19, or 20. `/ability list` will show something like `stamina_regen=1.5 (zone 7 19 20)`.

Single zone still works: `zone 19` means only that id.

## Finding your zone ids

1. Add the ability with a **dummy** zone (e.g. `zone 0`) so condition checks run.
2. Watch the log line: `Ability zone check: currentZone=X, allowedZones=..., active=...`
3. Walk through the full area you want (e.g. all of “Zone 3”); note every **currentZone** value you see (e.g. 7, 19, 20).
4. Re-add the ability with those ids: `/ability add <ability_id> [value] zone 7 19 20`.

Those ids are the ones configured for your world’s zones; they are not biome ids (different biomes can share the same zone id).
