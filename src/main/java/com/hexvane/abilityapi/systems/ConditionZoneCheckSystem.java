package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.ability.AbilityConditionSpec;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Periodically runs zone (and other) condition checks for players who have abilities with conditions.
 * This ensures we evaluate and log current zone even when no other system needs the ability this tick.
 * Runs every 20 ticks (~1/sec) so logs appear regularly.
 */
public class ConditionZoneCheckSystem extends EntityTickingSystem<EntityStore> {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();
    private static final int CHECK_INTERVAL = 20;

    /** Per-player tick counter so we run the zone check every CHECK_INTERVAL ticks per player. */
    private final Map<UUID, Integer> playerTickCounters = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) return;

        int count = playerTickCounters.merge(playerRef.getUuid(), 1, Integer::sum);
        if (count < CHECK_INTERVAL) return;
        playerTickCounters.put(playerRef.getUuid(), 0);

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        Map<String, ?> abilities = PlayerAbilityStorage.getAllAbilities(playerRef.getUuid());
        if (abilities.isEmpty()) return;

        for (String abilityId : abilities.keySet()) {
            List<AbilityConditionSpec> conditions = PlayerAbilityStorage.getConditions(playerRef.getUuid(), abilityId);
            if (conditions != null && !conditions.isEmpty()) {
                AbilityConditionService.getActiveAbilityValue(ref, store, world, playerRef.getUuid(), abilityId);
                break;
            }
        }
    }
}
