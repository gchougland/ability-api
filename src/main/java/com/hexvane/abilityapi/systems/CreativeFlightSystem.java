package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Periodically applies creative flight to players with the creative_flight ability in Adventure mode.
 */
public class CreativeFlightSystem extends EntityTickingSystem<EntityStore> {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();
    private static final int CHECK_INTERVAL = 20;

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

        Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
        if (playerComponent == null) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        MovementManager movementManager = store.getComponent(ref, EntityModule.get().getMovementManagerComponentType());
        if (movementManager == null) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        boolean hasAbility = PlayerAbilityStorage.hasAbility(playerRefComponent.getUuid(), world.getName(), "creative_flight");
        boolean isCreative = playerComponent.getGameMode() == GameMode.Creative;

        if (hasAbility && !isCreative) {
            movementManager.getDefaultSettings().canFly = true;
            movementManager.getSettings().canFly = true;
            movementManager.update(playerRefComponent.getPacketHandler());
        } else if (!hasAbility && !isCreative) {
            movementManager.refreshDefaultSettings(ref, store);
            movementManager.applyDefaultSettings();
            movementManager.update(playerRefComponent.getPacketHandler());
        }
    }
}
