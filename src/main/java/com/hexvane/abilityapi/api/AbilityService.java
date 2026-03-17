package com.hexvane.abilityapi.api;

import com.hexvane.abilityapi.ability.AbilityConditionSpec;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hexvane.abilityapi.systems.AbilityStatService;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Stable public-facing facade for other mods to interact with AbilityAPI.
 */
public final class AbilityService {

    private AbilityService() {
    }

    public static void setAbility(@Nonnull UUID playerId, @Nonnull String abilityId, @Nonnull Object value) {
        PlayerAbilityStorage.setAbility(playerId, abilityId, value);
    }

    public static void setConditions(@Nonnull UUID playerId, @Nonnull String abilityId, @Nonnull List<AbilityConditionSpec> conditions) {
        PlayerAbilityStorage.setConditions(playerId, abilityId, conditions);
    }

    public static void removeAbility(@Nonnull UUID playerId, @Nonnull String abilityId) {
        PlayerAbilityStorage.removeAbility(playerId, abilityId);
    }

    public static void applyForPlayer(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull World world
    ) {
        AbilityStatService.applyForPlayer(ref, store, world);
    }
}

