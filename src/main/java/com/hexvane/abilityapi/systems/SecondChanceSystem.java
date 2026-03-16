package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.data.SecondChanceCooldownStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * When a player with second_chance would take lethal damage, prevents death and restores
 * them to 20% of max health. Enforces a 5-minute cooldown per player.
 */
public class SecondChanceSystem extends DamageEventSystem {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();
    private static final float RESTORE_PERCENT = 0.2f;

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
        Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(index);
        if (targetRef == null || !targetRef.isValid()) return;

        Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
        if (playerComponent == null) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        if (!AbilityConditionService.isAbilityActive(targetRef, store, world, playerRefComponent.getUuid(), "second_chance")) {
            return;
        }
        if (SecondChanceCooldownStore.isOnCooldown(playerRefComponent.getUuid())) {
            return;
        }

        EntityStatMap statMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (statMap == null) return;

        int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
        if (healthIndex < 0 || healthIndex >= statMap.size()) return;

        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) return;

        float currentHealth = healthStat.get();
        float maxHealth = healthStat.getMax();
        if (maxHealth <= 0f) return;

        float damageAmount = damage.getAmount();
        if (damageAmount < currentHealth) {
            return; // non-lethal
        }

        // Trigger second chance: prevent death and restore to 20% max health
        float targetHealth = RESTORE_PERCENT * maxHealth;
        float newDamage = currentHealth - targetHealth;

        if (newDamage <= 0f) {
            damage.setAmount(0f);
            float delta = targetHealth - currentHealth;
            statMap.addStatValue(healthIndex, delta);
        } else {
            damage.setAmount(newDamage);
        }

        SecondChanceCooldownStore.recordUse(playerRefComponent.getUuid());
    }
}
