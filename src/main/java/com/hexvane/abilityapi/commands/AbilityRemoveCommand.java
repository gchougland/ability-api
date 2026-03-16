package com.hexvane.abilityapi.commands;

import com.hexvane.abilityapi.AbilityAPIPlugin;
import com.hexvane.abilityapi.ability.AbilityRegistry;
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
 * Remove an ability from a player.
 * Usage: /ability remove <ability_id>
 */
public class AbilityRemoveCommand extends AbstractPlayerCommand {
    private static final Pattern SPACES = Pattern.compile("\\s+");

    public AbilityRemoveCommand(@Nonnull AbilityAPIPlugin plugin) {
        super("remove", "Remove an ability");
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
        if (rawArgs.startsWith("remove")) {
            rawArgs = rawArgs.substring(6).trim();
        }
        if (rawArgs.isEmpty()) {
            context.sendMessage(Message.raw("Usage: /ability remove <ability_id>"));
            return;
        }
        String abilityId = rawArgs.split("\\s")[0];
        if (!AbilityRegistry.isValid(abilityId)) {
            context.sendMessage(Message.raw("Unknown ability: " + abilityId));
            return;
        }
        PlayerAbilityStorage.removeAbility(playerRef.getUuid(), abilityId);
        AbilityStatService.applyForPlayer(ref, store, world);
        context.sendMessage(Message.raw("Removed " + abilityId));
    }
}
