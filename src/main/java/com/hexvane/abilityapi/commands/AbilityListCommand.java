package com.hexvane.abilityapi.commands;

import com.hexvane.abilityapi.AbilityAPIPlugin;
import com.hexvane.abilityapi.ability.AbilityConditionSpec;
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
        Map<String, AbilityValue> abilities = PlayerAbilityStorage.getAllAbilities(playerRef.getUuid());
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
            var conditions = PlayerAbilityStorage.getConditions(playerRef.getUuid(), e.getKey());
            if (conditions != null && !conditions.isEmpty()) {
                sb.append(" (");
                for (int i = 0; i < conditions.size(); i++) {
                    if (i > 0) sb.append("; ");
                    AbilityConditionSpec c = conditions.get(i);
                    if (AbilityConditionSpec.TYPE_IN_ZONE.equals(c.type())) {
                        sb.append("zone ").append(c.allowedZoneIds().size() == 1 ? c.param() : String.join(" ", c.allowedZoneIds().stream().map(String::valueOf).toList()));
                    } else if (AbilityConditionSpec.TYPE_IN_SUNLIGHT.equals(c.type())) {
                        sb.append("sunlight");
                    } else {
                        sb.append(c.type()).append("=").append(c.param());
                    }
                }
                sb.append(")");
            }
            first = false;
        }
        context.sendMessage(Message.raw(sb.toString()));
    }
}
