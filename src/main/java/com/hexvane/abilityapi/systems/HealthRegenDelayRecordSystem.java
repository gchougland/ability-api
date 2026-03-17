package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.data.HealthRegenDelayStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Records last damage game time for any player who takes damage, so that
 * health_regen can enforce a delay before regen starts (same idea as stamina regen delay).
 */
public class HealthRegenDelayRecordSystem extends DamageEventSystem {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        var damageModule = DamageModule.get();
        var gatherGroup = damageModule.getGatherDamageGroup();
        var filterGroup = damageModule.getFilterDamageGroup();
        if (gatherGroup == null || filterGroup == null) {
            return Set.of();
        }
        return Set.of(
                new SystemGroupDependency<>(Order.AFTER, gatherGroup),
                new SystemGroupDependency<>(Order.AFTER, filterGroup)
        );
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage) {
        Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
        if (playerComponent == null) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
        if (worldTimeResource == null) return;

        HealthRegenDelayStore.recordDamage(playerRefComponent.getUuid(), worldTimeResource.getGameTime());
    }
}
