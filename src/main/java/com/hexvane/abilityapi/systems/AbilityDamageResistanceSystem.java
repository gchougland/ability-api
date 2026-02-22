package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.hypixel.hytale.component.SystemGroup;

/**
 * Applies per-damage-type resistance or weakness from ability values.
 * Ability ID format: resistance_&lt;DamageCauseId&gt; (e.g. resistance_physical, resistance_fall).
 * Value semantics: 0 = normal; 0 to 1 = resistance (reduced damage); -1 to 0 = weakness (increased damage).
 * Formula: damage *= (1 - value)
 */
public class AbilityDamageResistanceSystem extends DamageEventSystem {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();

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
        return QUERY;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull com.hypixel.hytale.component.Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage) {
        Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
        if (playerComponent == null) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null) return;

        String causeId = damageCause.getId();
        if (causeId == null) return;
        String abilityId = "resistance_" + causeId.toLowerCase();
        var abilityValue = PlayerAbilityStorage.getAbility(playerRefComponent.getUuid(), world.getName(), abilityId);
        if (abilityValue == null || !abilityValue.isPresent()) return;

        Object raw = abilityValue.getRaw();
        if (!(raw instanceof Number n)) return;

        double value = n.doubleValue();
        if (value == 0) return;

        float currentAmount = damage.getAmount();
        float newAmount = (float) (currentAmount * (1.0 - value));
        damage.setAmount(newAmount);
    }
}
