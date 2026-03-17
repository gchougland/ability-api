package com.hexvane.abilityapi.ability;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Specifies a condition for when an ability is active (e.g. in_zone with one or more zone ids).
 * Persisted with ability data; evaluated at runtime by AbilityConditionService.
 * <p>
 * For {@link #TYPE_IN_ZONE}: use {@link #zoneIds()} when non-null (multiple allowed zone ids),
 * otherwise {@link #param()} is the single allowed zone id (backward compatible).
 * For {@link #TYPE_HEALTH_BELOW}: {@link #param()} is the health percentage threshold (0-100);
 * condition passes when current health % is below that value.
 * For {@link #TYPE_HEALTH_ABOVE}: {@link #param()} is the health percentage threshold (0-100);
 * condition passes when current health % is at or above that value.
 * For {@link #TYPE_TARGET_HEALTH_BELOW} / {@link #TYPE_TARGET_HEALTH_ABOVE}: same semantics but applied to the
 * <b>target</b> of an action (e.g. damage victim). Only meaningful when evaluating in a damage context with a target ref.
 * For {@link #TYPE_IN_SUNLIGHT}: no param; passes when it is daytime and the entity has open sky above
 * (effective sunlight = skyLight × sunlightFactor above threshold).
 */
public record AbilityConditionSpec(
        @Nonnull String type,
        int param,
        @Nullable List<Integer> zoneIds
) {
    public static final String TYPE_IN_ZONE = "in_zone";
    public static final String TYPE_HEALTH_BELOW = "health_below";
    public static final String TYPE_HEALTH_ABOVE = "health_above";
    public static final String TYPE_TARGET_HEALTH_BELOW = "target_health_below";
    public static final String TYPE_TARGET_HEALTH_ABOVE = "target_health_above";
    public static final String TYPE_IN_SUNLIGHT = "in_sunlight";

    /** Backward-compatible constructor: single param only (zoneIds = null). */
    public AbilityConditionSpec(@Nonnull String type, int param) {
        this(type, param, null);
    }

    /** Returns the list of allowed zone ids for in_zone; if null, use {@link #param()} as single id. */
    @Nonnull
    public List<Integer> allowedZoneIds() {
        if (zoneIds != null && !zoneIds.isEmpty()) return zoneIds;
        return Collections.singletonList(param);
    }
}
