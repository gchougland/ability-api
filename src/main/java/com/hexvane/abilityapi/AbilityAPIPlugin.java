package com.hexvane.abilityapi;

import com.hexvane.abilityapi.ability.AbilityDefinition;
import com.hexvane.abilityapi.ability.AbilityRegistry;
import com.hexvane.abilityapi.ability.AbilityType;
import com.hexvane.abilityapi.commands.AbilityCommand;
import com.hexvane.abilityapi.config.MiningFortuneConfig;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hexvane.abilityapi.systems.AbilityConditionService;
import com.hexvane.abilityapi.systems.AbilityDamageResistanceSystem;
import com.hexvane.abilityapi.systems.ConditionZoneCheckSystem;
import com.hexvane.abilityapi.systems.AbilityInitSystem;
import com.hexvane.abilityapi.systems.AbilityPunchDamageSystem;
import com.hexvane.abilityapi.systems.AbilityStrengthSystem;
import com.hexvane.abilityapi.systems.CreativeFlightSystem;
import com.hexvane.abilityapi.systems.DarkVisionSystem;
import com.hexvane.abilityapi.systems.FallDamageImmunitySystem;
import com.hexvane.abilityapi.systems.MiningFortuneEventSystem;
import com.hexvane.abilityapi.systems.MiningHasteEventSystem;
import com.hexvane.abilityapi.systems.ConditionalHealthRegenSystem;
import com.hexvane.abilityapi.systems.ConditionalStatSystem;
import com.hexvane.abilityapi.systems.HealthRegenDelayRecordSystem;
import com.hexvane.abilityapi.systems.ItemMagnetSystem;
import com.hexvane.abilityapi.systems.MovementAbilitiesReapplySystem;
import com.hexvane.abilityapi.systems.SecondChanceSystem;
import com.hexvane.abilityapi.systems.WallClimbSystem;
import com.hexvane.abilityapi.systems.WaterbreathingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import java.util.logging.Level;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/**
 * AbilityAPI - Library mod for player abilities.
 */
public class AbilityAPIPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public AbilityAPIPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("AbilityAPI v%s loaded", this.getManifest().getVersion());
    }

    @Override
    protected void setup() {
        PlayerAbilityStorage.initialize(this.getDataDirectory());
        MiningFortuneConfig.initialize(this.getDataDirectory());

        //AbilityConditionService.getLogger().setLevel(Level.FINE);

        AbilityRegistry.register(new AbilityDefinition(
                "creative_flight", AbilityType.BINARY, Boolean.TRUE, 0, 1, "Allow creative-style flight"));
        AbilityRegistry.register(new AbilityDefinition(
                "waterbreathing", AbilityType.BINARY, Boolean.TRUE, 0, 1, "Breathe underwater"));
        AbilityRegistry.register(new AbilityDefinition(
                "oxygen", AbilityType.NUMERIC, 10.0, 0, 600, "Extra seconds of breath underwater"));
        AbilityRegistry.register(new AbilityDefinition(
                "fall_damage_immunity", AbilityType.BINARY, Boolean.TRUE, 0, 1, "Immune to fall damage"));
        AbilityRegistry.register(new AbilityDefinition(
                "move_speed", AbilityType.NUMERIC, 1.0, 0.5, 3.0, "Movement speed multiplier"));
        AbilityRegistry.register(new AbilityDefinition(
                "mining_haste", AbilityType.NUMERIC, 1.0, 1, 5, "Faster block breaking (level 1-5)"));
        AbilityRegistry.register(new AbilityDefinition(
                "mining_fortune", AbilityType.NUMERIC, 1.0, 1, 5, "Extra drops from mining configured blocks (e.g. ores); level = extra roll attempts"));
        // Phase 4 — Movement & combat
        AbilityRegistry.register(new AbilityDefinition(
                "punch_damage", AbilityType.NUMERIC, 1.0, 0.5, 3.0, "Unarmed/melee damage multiplier"));
        AbilityRegistry.register(new AbilityDefinition(
                "swim_speed", AbilityType.NUMERIC, 1.0, 0.5, 3.0, "Swimming speed multiplier"));
        AbilityRegistry.register(new AbilityDefinition(
                "strength", AbilityType.NUMERIC, 0.0, -1.0, 2.0, "Global damage dealt multiplier (attacker)"));
        AbilityRegistry.register(new AbilityDefinition(
                "wall_climb", AbilityType.BINARY, Boolean.TRUE, 0, 1, "Climb any solid surface"));
        AbilityRegistry.register(new AbilityDefinition(
                "dark_vision", AbilityType.BINARY, Boolean.TRUE, 0, 1, "See in darkness (screen effect)"));
        AbilityRegistry.register(new AbilityDefinition(
                "stamina_regen", AbilityType.NUMERIC, 1.0, 1.0, 3.0, "Stamina regen multiplier when conditions met (1=normal, >1=faster; no slowdown)"));
        AbilityRegistry.register(new AbilityDefinition(
                "second_chance", AbilityType.BINARY, Boolean.TRUE, 0, 1, "Prevent death once; restore to 20% health; 5 min cooldown"));
        AbilityRegistry.register(new AbilityDefinition(
                "health_regen", AbilityType.NUMERIC, 0.5, 0.1, 5.0, "Health regenerated per second when conditions met"));
        AbilityRegistry.register(new AbilityDefinition(
                "item_magnet", AbilityType.NUMERIC, 1.5, 1.0, 5.0, "Pull items from further away"));
        // Per-damage-type resistance/weakness: 0=normal, 0..1=resistance, -1..0=weakness (all damage types from game)
        registerResistanceAbilitiesFromDamageCauses();

        this.getEntityStoreRegistry().registerSystem(new CreativeFlightSystem());
        this.getEntityStoreRegistry().registerSystem(new WaterbreathingSystem());
        this.getEntityStoreRegistry().registerSystem(new AbilityInitSystem());
        this.getEntityStoreRegistry().registerSystem(new MovementAbilitiesReapplySystem());
        this.getEntityStoreRegistry().registerSystem(new FallDamageImmunitySystem());
        this.getEntityStoreRegistry().registerSystem(new SecondChanceSystem());
        this.getEntityStoreRegistry().registerSystem(new WallClimbSystem());
        this.getEntityStoreRegistry().registerSystem(new AbilityDamageResistanceSystem());
        this.getEntityStoreRegistry().registerSystem(new AbilityStrengthSystem());
        this.getEntityStoreRegistry().registerSystem(new AbilityPunchDamageSystem());
        this.getEntityStoreRegistry().registerSystem(new MiningHasteEventSystem());
        this.getEntityStoreRegistry().registerSystem(new MiningFortuneEventSystem());
        this.getEntityStoreRegistry().registerSystem(new DarkVisionSystem());
        this.getEntityStoreRegistry().registerSystem(new ConditionalStatSystem());
        this.getEntityStoreRegistry().registerSystem(new HealthRegenDelayRecordSystem());
        this.getEntityStoreRegistry().registerSystem(new ConditionalHealthRegenSystem());
        this.getEntityStoreRegistry().registerSystem(new ConditionZoneCheckSystem());
        this.getEntityStoreRegistry().registerSystem(new ItemMagnetSystem());

        this.getCommandRegistry().registerCommand(new AbilityCommand(this));
        LOGGER.atInfo().log("AbilityAPI setup complete");
    }

    private static void registerResistanceAbilitiesFromDamageCauses() {
        var damageCauseMap = DamageCause.getAssetMap();
        int registered = 0;
        for (int i = 0; i < damageCauseMap.getNextIndex(); i++) {
            var cause = damageCauseMap.getAsset(i);
            if (cause != null) {
                String id = cause.getId();
                if (id != null && !id.isEmpty()) {
                    registerResistanceAbility(id);
                    registered++;
                }
            }
        }
        if (registered == 0) {
            for (String id : new String[]{"Physical", "Projectile", "Bludgeoning", "Slashing", "Elemental", "Fire", "Ice", "Poison", "Environment", "Drowning", "Fall", "OutOfWorld", "Suffocation", "Environmental", "Command"}) {
                registerResistanceAbility(id);
            }
        }
    }

    private static void registerResistanceAbility(String damageTypeId) {
        String abilityId = "resistance_" + damageTypeId.toLowerCase();
        AbilityRegistry.register(new AbilityDefinition(
                abilityId, AbilityType.NUMERIC, 0.0, -1, 1, damageTypeId + " damage resistance or weakness"));
    }

    @Override
    protected void shutdown() {
        PlayerAbilityStorage.saveAll();
        LOGGER.atInfo().log("AbilityAPI shutdown");
    }
}
