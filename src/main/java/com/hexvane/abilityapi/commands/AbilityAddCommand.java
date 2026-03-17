package com.hexvane.abilityapi.commands;

import com.hexvane.abilityapi.AbilityAPIPlugin;
import com.hexvane.abilityapi.ability.AbilityConditionSpec;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Grant an ability to a player.
 * Usage: /ability add &lt;ability_id&gt; [value] [condition key value...]
 * Example: /ability add stamina_regen 1.5 zone 3
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
            context.sendMessage(Message.raw("Usage: /ability add <ability_id> [value] [zone <id> [id...]] [sunlight] [health_below|health_above|target_health_below|target_health_above <percent>]. target_* = victim's health % (for damage abilities)."));
            return;
        }
        String abilityId = parts[0];
        AbilityDefinition def = AbilityRegistry.get(abilityId);
        if (def == null) {
            context.sendMessage(Message.raw("Unknown ability: " + abilityId));
            return;
        }
        int valueEndIndex;
        Object value;
        if (def.type() == AbilityType.BINARY) {
            value = Boolean.TRUE;
            valueEndIndex = 1;
        } else {
            if (parts.length < 2 || parts[1].isEmpty()) {
                value = def.defaultValue();
                valueEndIndex = 1;
            } else {
                try {
                    double v = Double.parseDouble(parts[1]);
                    if (v < def.min() || v > def.max()) {
                        context.sendMessage(Message.raw("Value must be between " + def.min() + " and " + def.max()));
                        return;
                    }
                    value = v;
                    valueEndIndex = 2;
                } catch (NumberFormatException e) {
                    context.sendMessage(Message.raw("Invalid value: " + parts[1]));
                    return;
                }
            }
        }
        PlayerAbilityStorage.setAbility(playerRef.getUuid(), abilityId, value);

        String conditionRest = valueEndIndex < parts.length ? String.join(" ", Arrays.asList(parts).subList(valueEndIndex, parts.length)) : null;
        List<AbilityConditionSpec> conditions = parseConditions(conditionRest);
        PlayerAbilityStorage.setConditions(playerRef.getUuid(), abilityId, conditions != null ? conditions : List.of());

        AbilityStatService.applyForPlayer(ref, store, world);
        boolean hasConditions = conditions != null && !conditions.isEmpty();
        if (def.type() == AbilityType.NUMERIC) {
            context.sendMessage(Message.raw("Granted " + abilityId + " with value " + value + (hasConditions ? " (with conditions)" : "")));
        } else {
            context.sendMessage(Message.raw("Granted " + abilityId + (hasConditions ? " (with conditions)" : "")));
        }
    }

    /**
     * Parses optional condition key-value pairs from the remainder of the command.
     * Supported: "zone &lt;id&gt; [id...]" -> in_zone; "sunlight" / "in_sunlight" (no value); "health_below" / "health_above" / "target_health_below" / "target_health_above" &lt;percent&gt; (0-100).
     */
    @Nonnull
    private static List<AbilityConditionSpec> parseConditions(String rest) {
        List<AbilityConditionSpec> out = new ArrayList<>();
        if (rest == null || rest.isBlank()) return out;
        String[] tokens = SPACES.split(rest.trim());
        int i = 0;
        while (i < tokens.length) {
            String key = tokens[i].toLowerCase();
            if ("sunlight".equals(key) || "in_sunlight".equals(key)) {
                out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_IN_SUNLIGHT, 0));
                i += 1;
                continue;
            }
            if (i + 1 >= tokens.length) break;
            if ("zone".equals(key)) {
                List<Integer> ids = new ArrayList<>();
                int j = i + 1;
                while (j < tokens.length) {
                    try {
                        int z = Integer.parseInt(tokens[j]);
                        if (z >= 0 && z <= 9999) {
                            ids.add(z);
                            j++;
                        } else break;
                    } catch (NumberFormatException e) {
                        break;
                    }
                }
                if (!ids.isEmpty()) {
                    if (ids.size() == 1) {
                        out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_IN_ZONE, ids.get(0)));
                    } else {
                        out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_IN_ZONE, ids.get(0), ids));
                    }
                }
                i = j;
            } else if ("health_below".equals(key)) {
                try {
                    int percent = Integer.parseInt(tokens[i + 1]);
                    if (percent >= 0 && percent <= 100) {
                        out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_HEALTH_BELOW, percent));
                    }
                } catch (NumberFormatException ignored) {
                    // skip invalid health_below value
                }
                i += 2;
            } else if ("health_above".equals(key)) {
                try {
                    int percent = Integer.parseInt(tokens[i + 1]);
                    if (percent >= 0 && percent <= 100) {
                        out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_HEALTH_ABOVE, percent));
                    }
                } catch (NumberFormatException ignored) {
                    // skip invalid health_above value
                }
                i += 2;
            } else if ("target_health_below".equals(key)) {
                try {
                    int percent = Integer.parseInt(tokens[i + 1]);
                    if (percent >= 0 && percent <= 100) {
                        out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_TARGET_HEALTH_BELOW, percent));
                    }
                } catch (NumberFormatException ignored) {
                    // skip invalid target_health_below value
                }
                i += 2;
            } else if ("target_health_above".equals(key)) {
                try {
                    int percent = Integer.parseInt(tokens[i + 1]);
                    if (percent >= 0 && percent <= 100) {
                        out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_TARGET_HEALTH_ABOVE, percent));
                    }
                } catch (NumberFormatException ignored) {
                    // skip invalid target_health_above value
                }
                i += 2;
            } else {
                i += 2;
            }
        }
        return out;
    }
}
