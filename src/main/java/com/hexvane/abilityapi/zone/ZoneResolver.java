package com.hexvane.abilityapi.zone;

import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import javax.annotation.Nonnull;

/**
 * Resolves the world-generation zone index at a given block position.
 * Used by ability conditions (e.g. "in_zone" with param 3).
 * <p>
 * Uses the world's ChunkGenerator (when available) to compute zone and biome
 * at (blockX, blockZ) via the same API as the game's BiomeDataSystem.
 */
public final class ZoneResolver {
    /** Returned when the world has no zone-based generator (e.g. flat world). */
    public static final int NO_ZONE = -1;

    private ZoneResolver() {}

    /**
     * Returns the game's zone id at the given block coordinates (e.g. 7, 19, 20 on Orbis—not necessarily 0–4).
     * Uses the world seed and ChunkGenerator.getZoneBiomeResultAt(seed, x, z). Zone ids are configured in
     * worldgen/assets (see Docs/ZONE_IDS.md). Use the logged currentZone value(s) when setting a zone condition;
     * one logical zone can map to multiple ids—use: /ability add &lt;ability&gt; [value] zone &lt;id&gt; [id...]
     *
     * @return zone id from the generator, or {@link #NO_ZONE} if the world does not use
     *         a ChunkGenerator (e.g. flat or dummy generator)
     */
    public static int getZoneAt(@Nonnull World world, int blockX, int blockZ) {
        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator generator)) {
            return NO_ZONE;
        }
        long seedLong = world.getWorldConfig().getSeed();
        int seed = (int) seedLong;
        ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, blockX, blockZ);
        if (result == null || result.getZoneResult() == null || result.getZoneResult().getZone() == null) {
            return NO_ZONE;
        }
        return result.getZoneResult().getZone().id();
    }
}
