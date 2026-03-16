package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.systems.AbilityConditionService;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Applies mining_haste ability by multiplying block damage in DamageBlockEvent.
 * Level 1 = 1.2x damage, level 5 = 2.0x (20% per level).
 */
public class MiningHasteEventSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {
    private static final double HASTE_PER_LEVEL = 0.2;

    public MiningHasteEventSystem() {
        super(DamageBlockEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull DamageBlockEvent event) {
        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        var abilityValue = AbilityConditionService.getActiveAbilityValue(ref, store, world, playerRefComponent.getUuid(), "mining_haste");
        if (abilityValue == null || !abilityValue.isPresent() || !(abilityValue.getRaw() instanceof Number n)) return;

        int level = n.intValue();
        if (level < 1) return;

        double multiplier = 1.0 + HASTE_PER_LEVEL * level;
        float damage = event.getDamage();
        event.setDamage((float) (damage * multiplier));
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
