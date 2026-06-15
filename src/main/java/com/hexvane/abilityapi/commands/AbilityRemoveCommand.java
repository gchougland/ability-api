package com.hexvane.abilityapi.commands;

import com.hexvane.abilityapi.AbilityAPIPlugin;
import com.hexvane.abilityapi.ability.AbilityRegistry;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hexvane.abilityapi.systems.AbilityStatService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
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
 * Usage: /ability remove &lt;ability_id&gt; [player]
 */
public class AbilityRemoveCommand extends AbstractPlayerCommand {
    private static final Pattern SPACES = Pattern.compile("\\s+");

    public AbilityRemoveCommand(@Nonnull AbilityAPIPlugin plugin) {
        super("remove", "Remove an ability");
        this.setAllowsExtraArguments(true);
    }

    @Nonnull
    @Override
    public Message getUsageString(@Nonnull CommandSender sender) {
        return AbilityCommandHelp.removeHelp(this);
    }

    @Nonnull
    @Override
    public Message getUsageShort(@Nonnull CommandSender sender, boolean fullyQualify) {
        return AbilityCommandHelp.usageShort(this, AbilityCommandHelp.REMOVE_USAGE, fullyQualify);
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
            context.sendMessage(AbilityCommandHelp.removeHelp(this));
            return;
        }
        String[] parts = SPACES.split(rawArgs.trim());
        String abilityId = parts[0];
        if (!AbilityRegistry.isValid(abilityId)) {
            context.sendMessage(Message.raw("Unknown ability: " + abilityId));
            return;
        }
        if (parts.length > 2) {
            context.sendMessage(AbilityCommandHelp.removeHelp(this));
            return;
        }
        PlayerRef targetPlayerRef = playerRef;
        if (parts.length == 2) {
            PlayerRef found = AbilityCommandTargets.findOnlinePlayer(parts[1]);
            if (found == null) {
                context.sendMessage(Message.raw("Unknown player: " + parts[1]));
                return;
            }
            targetPlayerRef = found;
        }
        PlayerAbilityStorage.removeAbility(targetPlayerRef.getUuid(), abilityId);
        Ref<EntityStore> targetRef = targetPlayerRef.getReference();
        if (targetRef != null && targetRef.isValid()) {
            Store<EntityStore> targetStore = targetRef.getStore();
            AbilityStatService.applyForPlayer(targetRef, targetStore, targetStore.getExternalData().getWorld());
        }
        boolean other = !targetPlayerRef.getUuid().equals(playerRef.getUuid());
        String targetLabel = other ? " from " + targetPlayerRef.getUsername() : "";
        context.sendMessage(Message.raw("Removed " + abilityId + targetLabel));
    }
}
