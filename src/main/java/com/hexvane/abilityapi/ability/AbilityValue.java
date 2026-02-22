package com.hexvane.abilityapi.ability;

import javax.annotation.Nullable;

/**
 * Wrapper for ability value (Boolean for BINARY, Number for NUMERIC).
 */
public final class AbilityValue {
    private final Object value;

    public AbilityValue(@Nullable Object value) {
        this.value = value;
    }

    public boolean asBoolean() {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.doubleValue() != 0;
        }
        return false;
    }

    public double asNumber() {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof Boolean b) {
            return b ? 1.0 : 0.0;
        }
        return 0.0;
    }

    @Nullable
    public Object getRaw() {
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }
}
