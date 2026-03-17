package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.ability.AbilityConditionSpec;
import com.hexvane.abilityapi.ability.AbilityValue;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hexvane.abilityapi.zone.ZoneResolver;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Evaluates whether an ability is currently active for a player, taking into account
 * optional conditions (e.g. in_zone). Use this for abilities that have conditions;
 * systems use this so that zone and other conditions are respected.
 * <p>
 * Debug logging: set the log level for this class to FINE (e.g. in your logging config)
 * to see when conditions are evaluated and whether they pass (e.g. in_zone(3): currentZone=2, passed=false).
 */
public final class AbilityConditionService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private AbilityConditionService() {}

    /** Returns the logger used by this service (e.g. to set level to FINE from plugin setup). */
    @Nonnull
    public static HytaleLogger getLogger() {
        return LOGGER;
    }

    /**
     * Returns true if the ability is set for the player and all its conditions pass
     * in the current context (e.g. player position for zone).
     */
    public static boolean isAbilityActive(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull World world,
            @Nonnull java.util.UUID playerId,
            @Nonnull String abilityId) {
        return getActiveAbilityValue(ref, store, world, playerId, abilityId) != null;
    }

    /**
     * Returns the ability value if the ability is set and all conditions pass; otherwise null.
     * Use this when applying conditional abilities (e.g. stamina_regen in zone 3).
     */
    @Nullable
    public static AbilityValue getActiveAbilityValue(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull World world,
            @Nonnull java.util.UUID playerId,
            @Nonnull String abilityId) {
        return getActiveAbilityValue(ref, store, world, playerId, abilityId, null);
    }

    /**
     * Like {@link #getActiveAbilityValue(Ref, ComponentAccessor, World, java.util.UUID, String)} but supports
     * target-based conditions (e.g. target_health_below). Pass the <b>target</b> entity ref when applying
     * the ability in a damage context (e.g. the entity being damaged). If targetRef is null, target-based
     * conditions fail.
     */
    @Nullable
    public static AbilityValue getActiveAbilityValue(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull World world,
            @Nonnull java.util.UUID playerId,
            @Nonnull String abilityId,
            @Nullable Ref<EntityStore> targetRef) {
        AbilityValue value = PlayerAbilityStorage.getAbility(playerId, abilityId);
        if (value == null || !value.isPresent()) return null;

        List<AbilityConditionSpec> conditions = PlayerAbilityStorage.getConditions(playerId, abilityId);
        if (conditions == null || conditions.isEmpty()) return value;

        LOGGER.at(Level.FINE).log("Evaluating %d condition(s) for ability '%s'", conditions.size(), abilityId);
        for (AbilityConditionSpec cond : conditions) {
            boolean passed = evaluate(ref, store, world, abilityId, cond, targetRef);
            LOGGER.at(Level.FINE).log("  Condition %s(%s): passed=%s", cond.type(), cond.param(), passed);
            if (!passed) return null;
        }
        LOGGER.at(Level.FINE).log("All conditions passed for ability '%s' -> active", abilityId);
        return value;
    }

    private static boolean evaluate(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull World world,
            @Nonnull String abilityId,
            @Nonnull AbilityConditionSpec cond,
            @Nullable Ref<EntityStore> targetRef) {
        if (AbilityConditionSpec.TYPE_IN_ZONE.equals(cond.type())) {
            return evaluateInZone(ref, store, world, abilityId, cond.allowedZoneIds());
        }
        if (AbilityConditionSpec.TYPE_HEALTH_BELOW.equals(cond.type())) {
            return evaluateHealthBelow(ref, store, abilityId, cond.param());
        }
        if (AbilityConditionSpec.TYPE_HEALTH_ABOVE.equals(cond.type())) {
            return evaluateHealthAbove(ref, store, abilityId, cond.param());
        }
        if (AbilityConditionSpec.TYPE_TARGET_HEALTH_BELOW.equals(cond.type())) {
            return evaluateTargetHealthBelow(store, abilityId, cond.param(), targetRef);
        }
        if (AbilityConditionSpec.TYPE_TARGET_HEALTH_ABOVE.equals(cond.type())) {
            return evaluateTargetHealthAbove(store, abilityId, cond.param(), targetRef);
        }
        if (AbilityConditionSpec.TYPE_IN_SUNLIGHT.equals(cond.type())) {
            return evaluateInSunlight(ref, store, world, abilityId);
        }
        LOGGER.at(Level.FINE).log("  Unknown condition type '%s' for ability '%s' -> false", cond.type(), abilityId);
        return false;
    }

    /** Min sunlight factor (0–1) for daytime; below this = night. */
    private static final double MIN_SUNLIGHT_FACTOR = 0.2;
    /** Min effective sunlight (skyLight * sunlightFactor) for "in sunlight"; 0–15 scale. */
    private static final int MIN_EFFECTIVE_SUNLIGHT = 10;

    private static boolean evaluateInSunlight(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull World world,
            @Nonnull String abilityId) {
        WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
        if (worldTimeResource == null) {
            LOGGER.at(Level.FINE).log("  in_sunlight: no WorldTimeResource -> false");
            return false;
        }
        if (worldTimeResource.getSunlightFactor() < MIN_SUNLIGHT_FACTOR) {
            LOGGER.at(Level.FINE).log("  in_sunlight: sunlightFactor=%.2f < %.2f (night) -> false",
                    worldTimeResource.getSunlightFactor(), MIN_SUNLIGHT_FACTOR);
            return false;
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            LOGGER.at(Level.FINE).log("  in_sunlight: no transform -> false");
            return false;
        }
        var pos = transform.getPosition();
        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockX, blockZ));
        if (chunk == null) {
            LOGGER.at(Level.FINE).log("  in_sunlight: chunk not in memory (%d, %d) -> false", blockX, blockZ);
            return false;
        }
        BlockChunk blockChunk = chunk.getBlockChunk();
        byte skyLight = blockChunk.getSkyLight(blockX, blockY, blockZ);
        int effectiveSunlight = (int) (skyLight * worldTimeResource.getSunlightFactor());
        boolean passed = effectiveSunlight >= MIN_EFFECTIVE_SUNLIGHT;
        LOGGER.at(Level.FINE).log("  in_sunlight: skyLight=%d, sunlightFactor=%.2f, effective=%d, passed=%s",
                skyLight, worldTimeResource.getSunlightFactor(), effectiveSunlight, passed);
        return passed;
    }

    private static boolean evaluateHealthBelow(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull String abilityId,
            int thresholdPercent) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            LOGGER.at(Level.FINE).log("  health_below(%d): no EntityStatMap -> false", thresholdPercent);
            return false;
        }
        int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
        if (healthIndex < 0 || healthIndex >= statMap.size()) {
            LOGGER.at(Level.FINE).log("  health_below(%d): no Health stat -> false", thresholdPercent);
            return false;
        }
        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) {
            LOGGER.at(Level.FINE).log("  health_below(%d): null Health stat -> false", thresholdPercent);
            return false;
        }
        float current = healthStat.get();
        float max = healthStat.getMax();
        if (max <= 0f) {
            LOGGER.at(Level.FINE).log("  health_below(%d): maxHealth<=0 -> false", thresholdPercent);
            return false;
        }
        float percent = (current / max) * 100f;
        boolean passed = percent < thresholdPercent;
        LOGGER.at(Level.FINE).log("  health_below(%d): current=%.1f%%, passed=%s", thresholdPercent, percent, passed);
        return passed;
    }

    private static boolean evaluateHealthAbove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull String abilityId,
            int thresholdPercent) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            LOGGER.at(Level.FINE).log("  health_above(%d): no EntityStatMap -> false", thresholdPercent);
            return false;
        }
        int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
        if (healthIndex < 0 || healthIndex >= statMap.size()) {
            LOGGER.at(Level.FINE).log("  health_above(%d): no Health stat -> false", thresholdPercent);
            return false;
        }
        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) {
            LOGGER.at(Level.FINE).log("  health_above(%d): null Health stat -> false", thresholdPercent);
            return false;
        }
        float current = healthStat.get();
        float max = healthStat.getMax();
        if (max <= 0f) {
            LOGGER.at(Level.FINE).log("  health_above(%d): maxHealth<=0 -> false", thresholdPercent);
            return false;
        }
        float percent = (current / max) * 100f;
        boolean passed = percent >= thresholdPercent;
        LOGGER.at(Level.FINE).log("  health_above(%d): current=%.1f%%, passed=%s", thresholdPercent, percent, passed);
        return passed;
    }

    private static boolean evaluateTargetHealthBelow(
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull String abilityId,
            int thresholdPercent,
            @Nullable Ref<EntityStore> targetRef) {
        if (targetRef == null || !targetRef.isValid()) {
            LOGGER.at(Level.FINE).log("  target_health_below(%d): no target ref -> false", thresholdPercent);
            return false;
        }
        EntityStatMap statMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            LOGGER.at(Level.FINE).log("  target_health_below(%d): no EntityStatMap -> false", thresholdPercent);
            return false;
        }
        int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
        if (healthIndex < 0 || healthIndex >= statMap.size()) {
            LOGGER.at(Level.FINE).log("  target_health_below(%d): no Health stat -> false", thresholdPercent);
            return false;
        }
        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) {
            LOGGER.at(Level.FINE).log("  target_health_below(%d): null Health stat -> false", thresholdPercent);
            return false;
        }
        float current = healthStat.get();
        float max = healthStat.getMax();
        if (max <= 0f) {
            LOGGER.at(Level.FINE).log("  target_health_below(%d): maxHealth<=0 -> false", thresholdPercent);
            return false;
        }
        float percent = (current / max) * 100f;
        boolean passed = percent < thresholdPercent;
        LOGGER.at(Level.FINE).log("  target_health_below(%d): target=%.1f%%, passed=%s", thresholdPercent, percent, passed);
        return passed;
    }

    private static boolean evaluateTargetHealthAbove(
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull String abilityId,
            int thresholdPercent,
            @Nullable Ref<EntityStore> targetRef) {
        if (targetRef == null || !targetRef.isValid()) {
            LOGGER.at(Level.FINE).log("  target_health_above(%d): no target ref -> false", thresholdPercent);
            return false;
        }
        EntityStatMap statMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            LOGGER.at(Level.FINE).log("  target_health_above(%d): no EntityStatMap -> false", thresholdPercent);
            return false;
        }
        int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
        if (healthIndex < 0 || healthIndex >= statMap.size()) {
            LOGGER.at(Level.FINE).log("  target_health_above(%d): no Health stat -> false", thresholdPercent);
            return false;
        }
        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) {
            LOGGER.at(Level.FINE).log("  target_health_above(%d): null Health stat -> false", thresholdPercent);
            return false;
        }
        float current = healthStat.get();
        float max = healthStat.getMax();
        if (max <= 0f) {
            LOGGER.at(Level.FINE).log("  target_health_above(%d): maxHealth<=0 -> false", thresholdPercent);
            return false;
        }
        float percent = (current / max) * 100f;
        boolean passed = percent >= thresholdPercent;
        LOGGER.at(Level.FINE).log("  target_health_above(%d): target=%.1f%%, passed=%s", thresholdPercent, percent, passed);
        return passed;
    }

    private static boolean evaluateInZone(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull World world,
            @Nonnull String abilityId,
            @Nonnull List<Integer> allowedZoneIds) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            LOGGER.at(Level.FINE).log("  in_zone(%s): no transform -> false", allowedZoneIds);
            return false;
        }
        var pos = transform.getPosition();
        int blockX = (int) Math.floor(pos.x);
        int blockZ = (int) Math.floor(pos.z);
        int currentZone = ZoneResolver.getZoneAt(world, blockX, blockZ);
        boolean passed = allowedZoneIds.contains(currentZone);
        LOGGER.at(Level.FINE).log("  in_zone(%s): currentZone=%d (block %d, %d) -> %s",
                allowedZoneIds, currentZone, blockX, blockZ, passed);
        LOGGER.at(Level.INFO).log("Ability zone check: currentZone=%d, allowedZones=%s, active=%s (block %d, %d)",
                currentZone, allowedZoneIds, passed, blockX, blockZ);
        return passed;
    }
}
