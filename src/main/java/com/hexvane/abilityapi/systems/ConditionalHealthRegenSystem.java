package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.ability.AbilityValue;
import com.hexvane.abilityapi.data.HealthRegenDelayStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Applies conditional health regen (health_regen ability) when conditions pass.
 * <p>
 * Smooth per-tick regen: amount = value * dt (HP per second). Regen is blocked for a delay
 * after the player takes damage (game time), matching the stamina regen delay feel.
 */
public class ConditionalHealthRegenSystem extends com.hypixel.hytale.component.system.tick.EntityTickingSystem<EntityStore> {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();
    /** Seconds of game time after taking damage before health regen is allowed. */
    private static final int DELAY_SECONDS = 5;

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
        World world = store.getExternalData().getWorld();
        if (world == null) return;

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
        if (healthIndex < 0 || healthIndex >= statMap.size()) return;

        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) return;

        WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
        if (worldTimeResource != null
                && HealthRegenDelayStore.isWithinDelay(playerRefComponent.getUuid(), worldTimeResource.getGameTime(), DELAY_SECONDS)) {
            return;
        }

        AbilityValue activeValue = AbilityConditionService.getActiveAbilityValue(ref, store, world, playerRefComponent.getUuid(), "health_regen");
        if (activeValue == null || !activeValue.isPresent()) return;

        float valuePerSecond = activeValue.getRaw() instanceof Number n ? n.floatValue() : 0f;
        if (valuePerSecond <= 0f) return;

        float current = healthStat.get();
        float max = healthStat.getMax();
        if (max <= 0f) return;
        if (current >= max) return;

        float amount = valuePerSecond * dt;
        amount = Math.min(amount, max - current);
        if (amount > 0f) {
            statMap.addStatValue(healthIndex, amount);
        }
    }
}
