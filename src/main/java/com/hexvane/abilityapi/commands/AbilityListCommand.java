package com.hexvane.abilityapi.commands;

import com.hexvane.abilityapi.AbilityAPIPlugin;
import com.hexvane.abilityapi.ability.AbilityValue;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * List abilities for a player.
 * Usage: /ability list
 */
public class AbilityListCommand extends AbstractPlayerCommand {

    public AbilityListCommand(@Nonnull AbilityAPIPlugin plugin) {
        super("list", "List your abilities");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Map<String, AbilityValue> abilities = PlayerAbilityStorage.getAllAbilities(playerRef.getUuid(), world.getName());
        if (abilities.isEmpty()) {
            context.sendMessage(Message.raw("No abilities granted."));
            return;
        }
        StringBuilder sb = new StringBuilder("Abilities: ");
        boolean first = true;
        for (Map.Entry<String, AbilityValue> e : abilities.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getKey());
            AbilityValue v = e.getValue();
            if (v != null && v.getRaw() instanceof Number n) {
                sb.append("=").append(n);
            }
            first = false;
        }
        context.sendMessage(Message.raw(sb.toString()));
    }
}
