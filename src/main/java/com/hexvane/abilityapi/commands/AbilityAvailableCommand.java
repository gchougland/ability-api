package com.hexvane.abilityapi.commands;

import com.hexvane.abilityapi.AbilityAPIPlugin;
import com.hexvane.abilityapi.ability.AbilityDefinition;
import com.hexvane.abilityapi.ability.AbilityRegistry;
import com.hexvane.abilityapi.ability.AbilityType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;

/**
 * List all available abilities from the registry.
 * Usage: /ability available
 */
public class AbilityAvailableCommand extends CommandBase {

    public AbilityAvailableCommand(@Nonnull AbilityAPIPlugin plugin) {
        super("available", "List all available ability IDs and descriptions");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        Set<String> ids = new TreeSet<>(AbilityRegistry.getAllIds());
        if (ids.isEmpty()) {
            context.sendMessage(Message.raw("No abilities registered."));
            return;
        }
        for (String id : ids) {
            AbilityDefinition def = AbilityRegistry.get(id);
            if (def == null) continue;
            String line = formatAbility(def);
            context.sendMessage(Message.raw(line));
        }
    }

    private static String formatAbility(@Nonnull AbilityDefinition def) {
        StringBuilder sb = new StringBuilder();
        sb.append(def.id());
        if (def.type() == AbilityType.BINARY) {
            sb.append(" (binary)");
        } else {
            double min = def.min();
            double max = def.max();
            sb.append(" (numeric, ");
            if (min == (long) min && max == (long) max) {
                sb.append((long) min).append("-").append((long) max);
            } else {
                sb.append(min).append("-").append(max);
            }
            Object dv = def.defaultValue();
            if (dv instanceof Number n) {
                sb.append(", default ").append(n);
            }
            sb.append(")");
        }
        sb.append(": ").append(def.description());
        return sb.toString();
    }
}
