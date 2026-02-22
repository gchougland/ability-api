package com.hexvane.abilityapi.commands;

import com.hexvane.abilityapi.AbilityAPIPlugin;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import javax.annotation.Nonnull;

/**
 * Parent command for ability management.
 * Usage: /ability add|remove|list ...
 */
public class AbilityCommand extends AbstractCommandCollection {
    public AbilityCommand(@Nonnull AbilityAPIPlugin plugin) {
        super("ability", "Ability management commands");
        this.addSubCommand(new AbilityAddCommand(plugin));
        this.addSubCommand(new AbilityRemoveCommand(plugin));
        this.addSubCommand(new AbilityListCommand(plugin));
        this.addSubCommand(new AbilityAvailableCommand(plugin));
    }
}
