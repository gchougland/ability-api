package com.hexvane.abilityapi.data;

import com.hexvane.abilityapi.ability.AbilityValue;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages persistent file-based storage of player abilities.
 * Data is saved to JSON in the plugin's data directory.
 */
public final class PlayerAbilityStorage {
    private static final Logger LOGGER = Logger.getLogger(PlayerAbilityStorage.class.getName());
    private static final String DATA_FILE_NAME = "player_abilities.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path dataDirectory;
    // player UUID -> world name -> ability id -> value (Boolean or Number)
    private static final Map<UUID, Map<String, Map<String, Object>>> STORAGE = new ConcurrentHashMap<>();

    private PlayerAbilityStorage() {}

    public static void initialize(@Nonnull Path pluginDataDirectory) {
        dataDirectory = pluginDataDirectory;
        try {
            Files.createDirectories(dataDirectory);
            loadData();
            LOGGER.info("PlayerAbilityStorage initialized. Loaded data for " + STORAGE.size() + " players");
        } catch (IOException e) {
            LOGGER.severe("Failed to initialize PlayerAbilityStorage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void saveAll() {
        saveData();
    }

    @Nullable
    public static AbilityValue getAbility(@Nonnull UUID playerId, @Nonnull String worldName, @Nonnull String abilityId) {
        Map<String, Map<String, Object>> worldData = STORAGE.get(playerId);
        if (worldData == null) return null;
        Map<String, Object> abilities = worldData.get(worldName);
        if (abilities == null) return null;
        Object value = abilities.get(abilityId);
        return value != null ? new AbilityValue(value) : null;
    }

    public static boolean hasAbility(@Nonnull UUID playerId, @Nonnull String worldName, @Nonnull String abilityId) {
        AbilityValue v = getAbility(playerId, worldName, abilityId);
        return v != null && v.isPresent() && v.asBoolean();
    }

    public static void setAbility(@Nonnull UUID playerId, @Nonnull String worldName, @Nonnull String abilityId, @Nonnull Object value) {
        STORAGE
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
                .put(abilityId, value);
        saveData();
    }

    public static void removeAbility(@Nonnull UUID playerId, @Nonnull String worldName, @Nonnull String abilityId) {
        Map<String, Map<String, Object>> worldData = STORAGE.get(playerId);
        if (worldData == null) return;
        Map<String, Object> abilities = worldData.get(worldName);
        if (abilities == null) return;
        abilities.remove(abilityId);
        if (abilities.isEmpty()) worldData.remove(worldName);
        if (worldData.isEmpty()) STORAGE.remove(playerId);
        saveData();
    }

    @Nonnull
    public static Map<String, AbilityValue> getAllAbilities(@Nonnull UUID playerId, @Nonnull String worldName) {
        Map<String, Map<String, Object>> worldData = STORAGE.get(playerId);
        if (worldData == null) return Collections.emptyMap();
        Map<String, Object> abilities = worldData.get(worldName);
        if (abilities == null) return Collections.emptyMap();
        Map<String, AbilityValue> result = new HashMap<>();
        for (Map.Entry<String, Object> e : abilities.entrySet()) {
            result.put(e.getKey(), new AbilityValue(e.getValue()));
        }
        return result;
    }

    private static void loadData() {
        if (dataDirectory == null) return;
        Path dataFile = dataDirectory.resolve(DATA_FILE_NAME);
        if (!Files.exists(dataFile)) return;
        try {
            String json = Files.readString(dataFile);
            if (json == null || json.trim().isEmpty()) return;
            var typeToken = new TypeToken<Map<String, Map<String, Map<String, Object>>>>() {};
            Map<String, Map<String, Map<String, Object>>> loaded = GSON.fromJson(json, typeToken.getType());
            if (loaded != null) {
                STORAGE.clear();
                for (Map.Entry<String, Map<String, Map<String, Object>>> playerEntry : loaded.entrySet()) {
                    UUID playerId = UUID.fromString(playerEntry.getKey());
                    Map<String, Map<String, Object>> worldData = new ConcurrentHashMap<>();
                    for (Map.Entry<String, Map<String, Object>> worldEntry : playerEntry.getValue().entrySet()) {
                        Map<String, Object> abilities = worldEntry.getValue();
                        if (abilities != null) {
                            worldData.put(worldEntry.getKey(), new ConcurrentHashMap<>(abilities));
                        }
                    }
                    if (!worldData.isEmpty()) {
                        STORAGE.put(playerId, worldData);
                    }
                }
                LOGGER.info("Loaded ability data for " + STORAGE.size() + " players");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load ability data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void saveData() {
        if (dataDirectory == null) return;
        try {
            Map<String, Map<String, Map<String, Object>>> toSave = new HashMap<>();
            for (Map.Entry<UUID, Map<String, Map<String, Object>>> playerEntry : STORAGE.entrySet()) {
                Map<String, Map<String, Object>> worldData = new HashMap<>();
                for (Map.Entry<String, Map<String, Object>> worldEntry : playerEntry.getValue().entrySet()) {
                    worldData.put(worldEntry.getKey(), new HashMap<>(worldEntry.getValue()));
                }
                toSave.put(playerEntry.getKey().toString(), worldData);
            }
            Path dataFile = dataDirectory.resolve(DATA_FILE_NAME);
            String json = GSON.toJson(toSave);
            Files.writeString(dataFile, json);
        } catch (Exception e) {
            LOGGER.severe("Failed to save ability data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
