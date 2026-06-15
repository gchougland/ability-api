package com.hexvane.abilityapi.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import javax.annotation.Nonnull;

/**
 * Shared usage and --help text for /ability commands.
 */
public final class AbilityCommandHelp {

    private static final String REQUIRED = "#C1E0FF";
    private static final String OPTIONAL = "#7E9EBC";

    public static final String ADD_USAGE = "<ability_id> [value] [conditions...] [player]";
    public static final String REMOVE_USAGE = "<ability_id> [player]";
    public static final String LIST_USAGE = "[player]";
    public static final String AVAILABLE_USAGE = "";

    private AbilityCommandHelp() {}

    @Nonnull
    public static Message usageShort(@Nonnull AbstractCommand command, @Nonnull String argsSpec, boolean fullyQualify) {
        String name = fullyQualify ? command.getFullyQualifiedName() : command.getName();
        Message message = Message.raw(name);
        if (!argsSpec.isEmpty()) {
            message.insert(" ").insert(Message.raw(argsSpec).color(REQUIRED));
        }
        return message;
    }

    @Nonnull
    public static Message rootHelp() {
        return Message.raw("/ability")
                .insert("\n\nManage player abilities (grant, remove, list, browse).\n\n")
                .insert("Subcommands:\n")
                .insert("  add       ").insert(Message.raw(ADD_USAGE).color(OPTIONAL)).insert(" — grant an ability\n")
                .insert("  remove    ").insert(Message.raw(REMOVE_USAGE).color(OPTIONAL)).insert(" — remove an ability\n")
                .insert("  list      ").insert(Message.raw(LIST_USAGE).color(OPTIONAL)).insert(" — list granted abilities\n")
                .insert("  available — list all registered ability IDs\n\n")
                .insert("Use ")
                .insert(Message.raw("/ability <subcommand> --help").color(OPTIONAL))
                .insert(" for details on a subcommand.");
    }

    @Nonnull
    public static Message addHelp(@Nonnull AbstractCommand command) {
        return header(command, "Grant an ability to yourself or another online player.")
                .insert("\n\nUsage:\n  ")
                .insert(Message.raw("/" + command.getFullyQualifiedName() + " " + ADD_USAGE).color(REQUIRED))
                .insert("\n\n")
                .insert("Arguments:\n")
                .insert("  ").insert(Message.raw("ability_id").color(REQUIRED)).insert(" — ability ID (see /ability available)\n")
                .insert("  ").insert(Message.raw("value").color(OPTIONAL)).insert(" — numeric value for numeric abilities; omitted uses the default\n")
                .insert("  ").insert(Message.raw("conditions").color(OPTIONAL)).insert(" — optional activation conditions (see below)\n")
                .insert("  ").insert(Message.raw("player").color(OPTIONAL)).insert(" — online player to grant to; defaults to yourself\n\n")
                .insert("Conditions:\n")
                .insert("  ").insert(Message.raw("zone <id> [id...]").color(OPTIONAL)).insert(" — active in world zone(s)\n")
                .insert("  ").insert(Message.raw("sunlight").color(OPTIONAL)).insert(" — active in open sunlight during daytime\n")
                .insert("  ").insert(Message.raw("health_below <percent>").color(OPTIONAL)).insert(" — your health below %\n")
                .insert("  ").insert(Message.raw("health_above <percent>").color(OPTIONAL)).insert(" — your health at or above %\n")
                .insert("  ").insert(Message.raw("target_health_below <percent>").color(OPTIONAL)).insert(" — damage target health below %\n")
                .insert("  ").insert(Message.raw("target_health_above <percent>").color(OPTIONAL)).insert(" — damage target health at or above %\n\n")
                .insert("Examples:\n")
                .insert("  /ability add creative_flight\n")
                .insert("  /ability add oxygen 20\n")
                .insert("  /ability add stamina_regen 1.5 zone 3\n")
                .insert("  /ability add strength 0.25 health_below 50\n")
                .insert("  /ability add creative_flight Steve\n")
                .insert("  /ability add oxygen 20 Alice");
    }

    @Nonnull
    public static Message removeHelp(@Nonnull AbstractCommand command) {
        return header(command, "Remove a granted ability from yourself or another online player.")
                .insert("\n\nUsage:\n  ")
                .insert(Message.raw("/" + command.getFullyQualifiedName() + " " + REMOVE_USAGE).color(REQUIRED))
                .insert("\n\n")
                .insert("Arguments:\n")
                .insert("  ").insert(Message.raw("ability_id").color(REQUIRED)).insert(" — ability ID to remove\n")
                .insert("  ").insert(Message.raw("player").color(OPTIONAL)).insert(" — online player to remove from; defaults to yourself\n\n")
                .insert("Examples:\n")
                .insert("  /ability remove creative_flight\n")
                .insert("  /ability remove strength\n")
                .insert("  /ability remove creative_flight Steve");
    }

    @Nonnull
    public static Message listHelp(@Nonnull AbstractCommand command) {
        return header(command, "List abilities currently granted to a player.")
                .insert("\n\nUsage:\n  ")
                .insert(Message.raw("/" + command.getFullyQualifiedName() + " " + LIST_USAGE).color(REQUIRED))
                .insert("\n\n")
                .insert("Arguments:\n")
                .insert("  ").insert(Message.raw("player").color(OPTIONAL)).insert(" — online player to inspect; defaults to yourself\n\n")
                .insert("Examples:\n")
                .insert("  /ability list\n")
                .insert("  /ability list Steve");
    }

    @Nonnull
    public static Message availableHelp(@Nonnull AbstractCommand command) {
        return header(command, "List every ability ID registered in AbilityAPI with type, range, and description.")
                .insert("\n\nUsage:\n  ")
                .insert(Message.raw("/" + command.getFullyQualifiedName()).color(REQUIRED))
                .insert("\n\n")
                .insert("Examples:\n")
                .insert("  /ability available");
    }

    @Nonnull
    private static Message header(@Nonnull AbstractCommand command, @Nonnull String description) {
        return Message.raw("/" + command.getFullyQualifiedName())
                .insert("\n\n")
                .insert(description);
    }
}
