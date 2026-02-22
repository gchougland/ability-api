package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.ability.AbilityValue;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Applies ability-derived stat modifiers and movement speed for a single player.
 * Call when abilities change (add/remove) or on player init.
 */
public final class AbilityStatService {
    private static final String KEY_ABILITY_ADDITIVE = "Ability_ADDITIVE";
    private static final String KEY_ABILITY_MULTIPLICATIVE = "Ability_MULTIPLICATIVE";
    /** Oxygen units per second (approx 20 for ~15s base breath). Extra seconds add this much to max. */
    private static final float OXYGEN_PER_SECOND = 20.0f;

    private AbilityStatService() {}

    /**
     * Applies stat modifiers and movement speed from current abilities for the given player.
     * Safe to call from command (world thread) or from a system.
     */
    public static void applyForPlayer(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull World world) {
        if (!ref.isValid()) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        String worldName = world.getName();
        UUID uuid = playerRefComponent.getUuid();

        // --- Oxygen: ADDITIVE modifier on max ---
        AbilityValue oxygenAbility = PlayerAbilityStorage.getAbility(uuid, worldName, "oxygen");
        float extraOxygen = oxygenAbility != null && oxygenAbility.isPresent() && oxygenAbility.asNumber() > 0
                ? (float) (oxygenAbility.asNumber() * OXYGEN_PER_SECOND)
                : 0f;

        int oxygenIndex = DefaultEntityStatTypes.getOxygen();
        if (oxygenIndex >= 0 && oxygenIndex < statMap.size()) {
            if (extraOxygen > 0) {
                statMap.putModifier(EntityStatMap.Predictable.SELF, oxygenIndex, KEY_ABILITY_ADDITIVE,
                        new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, extraOxygen));
            } else {
                statMap.removeModifier(EntityStatMap.Predictable.SELF, oxygenIndex, KEY_ABILITY_ADDITIVE);
            }
        }

        // --- Move speed and swim speed: apply to MovementSettings ---
        MovementManager movementManager = store.getComponent(ref, EntityModule.get().getMovementManagerComponentType());
        if (movementManager != null) {
            AbilityValue moveSpeedAbility = PlayerAbilityStorage.getAbility(uuid, worldName, "move_speed");
            AbilityValue swimSpeedAbility = PlayerAbilityStorage.getAbility(uuid, worldName, "swim_speed");
            float defaultBaseSpeed = movementManager.getDefaultSettings().baseSpeed;
            double moveMultiplier = 1.0;
            if (moveSpeedAbility != null && moveSpeedAbility.isPresent() && moveSpeedAbility.getRaw() instanceof Number n) {
                moveMultiplier = n.doubleValue();
            }
            double swimMultiplier = 1.0;
            if (swimSpeedAbility != null && swimSpeedAbility.isPresent() && swimSpeedAbility.getRaw() instanceof Number n) {
                swimMultiplier = n.doubleValue();
            }
            boolean swimming = false;
            MovementStatesComponent movementStatesComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
            if (movementStatesComponent != null && movementStatesComponent.getMovementStates() != null) {
                swimming = movementStatesComponent.getMovementStates().swimming;
            }
            double multiplier = swimming ? moveMultiplier * swimMultiplier : moveMultiplier;
            movementManager.getSettings().baseSpeed = (float) (defaultBaseSpeed * multiplier);
            movementManager.update(playerRefComponent.getPacketHandler());
        }

        player.getStatModifiersManager().setRecalculate(true);
    }
}
