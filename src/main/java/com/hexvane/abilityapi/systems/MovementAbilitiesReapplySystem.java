package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Periodically re-applies AbilityStatService for players with move_speed or swim_speed
 * so that transitions in/out of water update baseSpeed promptly.
 */
public class MovementAbilitiesReapplySystem extends EntityTickingSystem<EntityStore> {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();
    private static final int CHECK_INTERVAL = 10;

    private int tickCounter = 0;

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
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        if (!PlayerAbilityStorage.hasAbility(playerRefComponent.getUuid(), "move_speed")
                && !PlayerAbilityStorage.hasAbility(playerRefComponent.getUuid(), "swim_speed")) {
            return;
        }

        AbilityStatService.applyForPlayer(ref, store, world);
    }
}
