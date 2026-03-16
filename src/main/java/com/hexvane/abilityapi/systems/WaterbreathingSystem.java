package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.systems.AbilityConditionService;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Keeps oxygen bar full when underwater for players with the waterbreathing ability.
 * Runs every 10 ticks to reduce oxygen bar flicker from rapid sync changes.
 */
public class WaterbreathingSystem extends EntityTickingSystem<EntityStore> {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();
    private static final int CHECK_INTERVAL = 10;
    private static int tickCounter = 0;

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
            @Nonnull com.hypixel.hytale.component.Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (index == 0) {
            tickCounter = (tickCounter + 1) % CHECK_INTERVAL;
        }
        if (tickCounter != 0) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        if (!AbilityConditionService.isAbilityActive(ref, store, world, playerRefComponent.getUuid(), "waterbreathing")) {
            return;
        }

        long packed = LivingEntity.getPackedMaterialAndFluidAtBreathingHeight(ref, commandBuffer);
        int fluidId = MathUtil.unpackRight(packed);
        if (fluidId == 0) return;

        EntityStatMap statMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (statMap == null) return;

        var oxygenStatValue = statMap.get(DefaultEntityStatTypes.getOxygen());
        if (oxygenStatValue == null) return;

        float current = oxygenStatValue.get();
        float max = oxygenStatValue.getMax();
        if (current < max) {
            statMap.addStatValue(DefaultEntityStatTypes.getOxygen(), max - current);
        }
    }
}
