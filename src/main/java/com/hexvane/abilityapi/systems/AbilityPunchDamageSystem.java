package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.ability.AbilityValue;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
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
 * Applies the attacker's punch_damage ability: multiplies PHYSICAL damage dealt by the player.
 * Query uses EntityStatMap when available so we run for any damage victim (player or NPC); else PlayerRef only.
 */
public class AbilityPunchDamageSystem extends DamageEventSystem {
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
        DamageCause cause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (cause == null) return;
        // Only apply to PHYSICAL damage (unarmed/melee) — use cause id to avoid deprecated DamageCause.PHYSICAL
        String causeId = cause.getId();
        if (causeId == null || !causeId.equalsIgnoreCase("physical")) return;

        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) return;

        Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
        if (attackerPlayer == null) return;

        // Only apply punch_damage when attacking with fists (empty hand / no weapon)
        if (!ItemStack.isEmpty(attackerPlayer.getInventory().getItemInHand())) return;

        PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (attackerPlayerRef == null) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(index);
        AbilityValue punchDamageAbility = AbilityConditionService.getActiveAbilityValue(attackerRef, store, world, attackerPlayerRef.getUuid(), "punch_damage", targetRef);
        if (punchDamageAbility == null || !punchDamageAbility.isPresent()) return;

        Object raw = punchDamageAbility.getRaw();
        if (!(raw instanceof Number n)) return;

        double multiplier = n.doubleValue();
        if (multiplier == 1.0) return;

        float currentAmount = damage.getAmount();
        float newAmount = (float) (currentAmount * multiplier);
        if (newAmount < 0) newAmount = 0;
        damage.setAmount(newAmount);
    }
}
