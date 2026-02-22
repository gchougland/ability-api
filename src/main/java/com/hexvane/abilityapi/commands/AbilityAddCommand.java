package com.hexvane.abilityapi.commands;

import com.hexvane.abilityapi.AbilityAPIPlugin;
import com.hexvane.abilityapi.ability.AbilityDefinition;
import com.hexvane.abilityapi.ability.AbilityRegistry;
import com.hexvane.abilityapi.ability.AbilityType;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hexvane.abilityapi.systems.AbilityStatService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Grant an ability to a player.
 * Usage: /ability add <ability_id> [value]
 */
public class AbilityAddCommand extends AbstractPlayerCommand {
    private static final Pattern SPACES = Pattern.compile("\\s+");

    public AbilityAddCommand(@Nonnull AbilityAPIPlugin plugin) {
        super("add", "Grant an ability");
        this.setAllowsExtraArguments(true);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        String rawArgs = CommandUtil.stripCommandName(context.getInputString()).trim();
        if (rawArgs.startsWith("add")) {
            rawArgs = rawArgs.substring(3).trim();
        }
        String[] parts = SPACES.split(rawArgs, 3);
        if (parts.length < 1 || parts[0].isEmpty()) {
            context.sendMessage(Message.raw("Usage: /ability add <ability_id> [value]"));
            return;
        }
        String abilityId = parts[0];
        AbilityDefinition def = AbilityRegistry.get(abilityId);
        if (def == null) {
            context.sendMessage(Message.raw("Unknown ability: " + abilityId));
            return;
        }
        Object value;
        if (def.type() == AbilityType.BINARY) {
            value = Boolean.TRUE;
        } else {
            if (parts.length < 2 || parts[1].isEmpty()) {
                value = def.defaultValue();
            } else {
                try {
                    double v = Double.parseDouble(parts[1]);
                    if (v < def.min() || v > def.max()) {
                        context.sendMessage(Message.raw("Value must be between " + def.min() + " and " + def.max()));
                        return;
                    }
                    value = v;
                } catch (NumberFormatException e) {
                    context.sendMessage(Message.raw("Invalid value: " + parts[1]));
                    return;
                }
            }
        }
        PlayerAbilityStorage.setAbility(playerRef.getUuid(), world.getName(), abilityId, value);
        AbilityStatService.applyForPlayer(ref, store, world);
        if (def.type() == AbilityType.NUMERIC) {
            context.sendMessage(Message.raw("Granted " + abilityId + " with value " + value));
        } else {
            context.sendMessage(Message.raw("Granted " + abilityId));
        }
    }
}
