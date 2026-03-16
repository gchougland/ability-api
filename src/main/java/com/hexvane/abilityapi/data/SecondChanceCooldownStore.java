package com.hexvane.abilityapi.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * In-memory cooldown store for the second_chance ability.
 * Tracks last use time per player; cooldown duration is 5 minutes.
 */
public final class SecondChanceCooldownStore {
    private static final long COOLDOWN_MS = 300_000L; // 5 minutes

    private static final Map<UUID, Long> lastUseMillis = new ConcurrentHashMap<>();

    private SecondChanceCooldownStore() {}

    /** Returns true if the player is still on cooldown and cannot trigger second chance. */
    public static boolean isOnCooldown(@Nonnull UUID playerId) {
        Long last = lastUseMillis.get(playerId);
        if (last == null) return false;
        return System.currentTimeMillis() - last < COOLDOWN_MS;
    }

    /** Records that the player just used second chance; starts the 5-minute cooldown. */
    public static void recordUse(@Nonnull UUID playerId) {
        lastUseMillis.put(playerId, System.currentTimeMillis());
    }
}
