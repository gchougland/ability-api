package com.hexvane.abilityapi.ability;

import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import javax.annotation.Nonnull;

/**
 * Describes a single ability's contribution to an entity stat (additive or multiplicative).
 * Used by AbilityStatService to compute and apply modifiers.
 */
public record StatContribution(
        @Nonnull String abilityId,
        @Nonnull String statKey,
        @Nonnull StaticModifier.CalculationType calculationType,
        float amount
) {}
