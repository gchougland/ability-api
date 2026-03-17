package com.hexvane.abilityapi.data;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Tracks last damage time (game time) per player for health_regen delay.
 * Regen is blocked for a configurable period after taking damage; game time is used
 * so the delay does not count down when the world is paused.
 */
public final class HealthRegenDelayStore {
    private static final Map<UUID, Instant> lastDamageGameTime = new ConcurrentHashMap<>();

    private HealthRegenDelayStore() {}

    /** Records that the player just took damage; used to block health regen for the delay period. */
    public static void recordDamage(@Nonnull UUID playerId, @Nonnull Instant gameTime) {
        lastDamageGameTime.put(playerId, gameTime);
    }

    /** Returns true if the player is still within the delay after damage (regen should be blocked). */
    public static boolean isWithinDelay(@Nonnull UUID playerId, @Nonnull Instant now, int delaySeconds) {
        Instant last = lastDamageGameTime.get(playerId);
        if (last == null) return false;
        return now.isBefore(last.plus(delaySeconds, ChronoUnit.SECONDS));
    }
}
