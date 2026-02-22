package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.ability.AbilityValue;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.hypixel.hytale.component.SystemGroup;

/**
 * Applies the attacker's strength ability: damage dealt is multiplied by (1 + value).
 * Value > 0 = deal more damage; value < 0 = deal less (weakness).
 * Query uses EntityStatMap when available so we run for any damage victim (player or NPC); else PlayerRef only.
 */
public class AbilityStrengthSystem extends DamageEventSystem {
    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        var dm = DamageModule.get();
        return dm != null ? dm.getFilterDamageGroup() : null;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        var damageModule = DamageModule.get();
        var gatherGroup = damageModule != null ? damageModule.getGatherDamageGroup() : null;
        if (gatherGroup == null) return Set.of();
        return Set.of(new SystemGroupDependency<>(Order.AFTER, gatherGroup));
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        try {
            if (com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule.get() != null) {
                var t = EntityStatMap.getComponentType();
                if (t != null) return Query.and(t);
            }
        } catch (Throwable ignored) { }
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull com.hypixel.hytale.component.Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage) {
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) return;

        Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
        if (attackerPlayer == null) return;

        PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (attackerPlayerRef == null) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        AbilityValue strengthAbility = PlayerAbilityStorage.getAbility(attackerPlayerRef.getUuid(), world.getName(), "strength");
        if (strengthAbility == null || !strengthAbility.isPresent()) return;

        Object raw = strengthAbility.getRaw();
        if (!(raw instanceof Number n)) return;

        double value = n.doubleValue();
        if (value == 0) return;

        // damage *= (1 + value); clamp so result is non-negative
        float currentAmount = damage.getAmount();
        float newAmount = (float) (currentAmount * (1.0 + value));
        if (newAmount < 0) newAmount = 0;
        damage.setAmount(newAmount);
    }
}
