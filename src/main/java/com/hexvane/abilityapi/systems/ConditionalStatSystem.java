package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.ability.AbilityValue;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Applies conditional stat effects (e.g. stamina_regen only when in a given zone).
 * <p>
 * Stamina regen: adds a smooth per-tick bonus (scaled by dt) so it matches base-game feel.
 * Only adds when the base game would allow stamina regen: <b>not sprinting</b> and
 * <b>StaminaRegenDelay &gt;= 0</b>. The delay stat is set negative after consuming stamina
 * (sprint stop, attacks, guard, etc.) and ticks back to 0; regen is blocked until then.
 * Extra per second = (multiplier - 1) * base rate; value range is 1.0–3.0 (no slowdown).
 */
public class ConditionalStatSystem extends com.hypixel.hytale.component.system.tick.EntityTickingSystem<EntityStore> {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();
    /** Base stamina regen rate (per second) used to scale the bonus; extra = (mult - 1) * this * dt. */
    private static final float BASE_STAMINA_REGEN_PER_SECOND = 4.0f;

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
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        World world = store.getExternalData().getWorld();
        if (world == null) return;

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        MovementStatesComponent movementStates = store.getComponent(ref, MovementStatesComponent.getComponentType());
        if (movementStates != null && movementStates.getMovementStates().sprinting) return;

        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        int staminaIndex = DefaultEntityStatTypes.getStamina();
        if (staminaIndex < 0 || staminaIndex == Integer.MIN_VALUE || staminaIndex >= statMap.size()) return;

        int delayIndex = EntityStatType.getAssetMap().getIndex("StaminaRegenDelay");
        if (delayIndex >= 0 && delayIndex < statMap.size()) {
            EntityStatValue delayStat = statMap.get(delayIndex);
            if (delayStat != null && delayStat.get() < 0f) return;
        }

        AbilityValue activeValue = AbilityConditionService.getActiveAbilityValue(ref, store, world, playerRefComponent.getUuid(), "stamina_regen");
        float multiplier = activeValue != null && activeValue.getRaw() instanceof Number n
                ? n.floatValue()
                : 0f;

        if (multiplier > 1f) {
            float extraPerTick = (multiplier - 1f) * BASE_STAMINA_REGEN_PER_SECOND * dt;
            if (extraPerTick > 0f) {
                statMap.addStatValue(EntityStatMap.Predictable.SELF, staminaIndex, extraPerTick);
            }
        }
    }
}
