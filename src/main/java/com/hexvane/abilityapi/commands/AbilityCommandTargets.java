package com.hexvane.abilityapi.commands;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Resolves optional target players for /ability commands.
 */
public final class AbilityCommandTargets {

    private AbilityCommandTargets() {}

    public record ParsedSuffix(@Nullable PlayerRef target, @Nullable String remainder) {}

    @Nullable
    public static PlayerRef findOnlinePlayer(@Nonnull String username) {
        return Universe.get().getPlayerByUsername(username, NameMatching.DEFAULT);
    }

    /**
     * If the last whitespace-separated token of {@code rest} matches an online player, returns that
     * player and the rest without that token. Otherwise returns a null target and the original rest.
     */
    @Nonnull
    public static ParsedSuffix stripTrailingPlayerName(@Nullable String rest) {
        if (rest == null || rest.isBlank()) {
            return new ParsedSuffix(null, rest);
        }
        String trimmed = rest.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        String candidate = lastSpace < 0 ? trimmed : trimmed.substring(lastSpace + 1);
        PlayerRef target = findOnlinePlayer(candidate);
        if (target == null) {
            return new ParsedSuffix(null, trimmed);
        }
        String without = lastSpace < 0 ? null : trimmed.substring(0, lastSpace).trim();
        if (without != null && without.isEmpty()) {
            without = null;
        }
        return new ParsedSuffix(target, without);
    }
}
