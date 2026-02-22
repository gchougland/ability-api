package com.hexvane.abilityapi.ability;

import javax.annotation.Nonnull;

/**
 * Immutable definition of an ability.
 */
public record AbilityDefinition(
        @Nonnull String id,
        @Nonnull AbilityType type,
        @Nonnull Object defaultValue,
        double min,
        double max,
        @Nonnull String description
) {
    public boolean isValidValue(Object value) {
        if (type == AbilityType.BINARY) {
            return value instanceof Boolean;
        }
        if (type == AbilityType.NUMERIC && value instanceof Number num) {
            double v = num.doubleValue();
            return v >= min && v <= max;
        }
        return false;
    }
}
