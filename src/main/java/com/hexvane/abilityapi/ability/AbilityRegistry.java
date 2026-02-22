package com.hexvane.abilityapi.ability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Central registry of ability definitions.
 */
public final class AbilityRegistry {
    private static final Map<String, AbilityDefinition> REGISTRY = new HashMap<>();

    private AbilityRegistry() {}

    public static void register(@Nonnull AbilityDefinition def) {
        REGISTRY.put(def.id(), def);
    }

    @Nullable
    public static AbilityDefinition get(@Nonnull String id) {
        return REGISTRY.get(id);
    }

    public static boolean isValid(@Nonnull String id) {
        return REGISTRY.containsKey(id);
    }

    @Nonnull
    public static Set<String> getAllIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
