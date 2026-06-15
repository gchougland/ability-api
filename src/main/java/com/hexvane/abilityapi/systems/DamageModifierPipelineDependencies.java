package com.hexvane.abilityapi.systems;

import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCalculatorSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Dependencies for {@link com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem}s
 * that must mutate {@link com.hypixel.hytale.server.core.modules.entity.damage.Damage} before
 * {@link DamageSystems.ApplyDamage} applies it to health. Systems that only use
 * {@code AFTER} gather + filter run in an undefined order relative to {@code ApplyDamage}, so damage
 * reduction can run after HP is already subtracted.
 */
final class DamageModifierPipelineDependencies {
    private DamageModifierPipelineDependencies() {}

    /**
     * After filter-phase modifiers and {@link DamageCalculatorSystems.SequenceModifier}, before
     * {@link DamageSystems.ApplyDamage}.
     */
    @Nonnull
    static Set<Dependency<EntityStore>> afterFilterBeforeApplyDamage() {
        var damageModule = DamageModule.get();
        if (damageModule == null) {
            return Set.of();
        }
        var gatherGroup = damageModule.getGatherDamageGroup();
        var filterGroup = damageModule.getFilterDamageGroup();
        if (gatherGroup == null || filterGroup == null) {
            return Set.of();
        }
        return Set.of(
                new SystemGroupDependency<>(Order.AFTER, gatherGroup),
                new SystemGroupDependency<>(Order.AFTER, filterGroup),
                new SystemDependency<>(Order.AFTER, DamageCalculatorSystems.SequenceModifier.class),
                new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class));
    }
}
