package com.hexvane.abilityapi.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreathingCheckEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Allows players with the waterbreathing ability to breathe in fluids.
 * In 0.5+, drowning damage is driven by {@code BreathingComponent.isSuffocating()} after
 * {@link BreathingCheckEvent}; topping the oxygen stat alone is not sufficient.
 */
public class WaterbreathingEventSystem extends EntityEventSystem<EntityStore, BreathingCheckEvent> {

    public WaterbreathingEventSystem() {
        super(BreathingCheckEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreathingCheckEvent event) {
        if (event.getFluidId() == 0) return;

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        if (AbilityConditionService.isAbilityActive(ref, store, world, playerRefComponent.getUuid(), "waterbreathing")) {
            event.setCanBreathe(true);
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
