package com.hexvane.abilityapi.data;

import com.hexvane.abilityapi.ability.AbilityConditionSpec;
import com.hexvane.abilityapi.ability.AbilityValue;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.JsonArray;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages persistent file-based storage of player abilities.
 * Data is stored globally per server (one set of abilities per player, not per world).
 * Saved to JSON in the plugin's data directory.
 */
public final class PlayerAbilityStorage {
    private static final Logger LOGGER = Logger.getLogger(PlayerAbilityStorage.class.getName());
    private static final String DATA_FILE_NAME = "player_abilities.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(AbilityConditionSpec.class, (JsonDeserializer<AbilityConditionSpec>) (json, typeOfT, context) -> {
                JsonObject o = json.getAsJsonObject();
                String type = o.has("type") ? o.get("type").getAsString() : "";
                int param = o.has("param") ? o.get("param").getAsInt() : 0;
                List<Integer> zoneIds = null;
                if (o.has("zoneIds") && o.get("zoneIds").isJsonArray()) {
                    JsonArray arr = o.get("zoneIds").getAsJsonArray();
                    List<Integer> list = new ArrayList<>(arr.size());
                    for (JsonElement e : arr) list.add(e.getAsInt());
                    zoneIds = list;
                }
                return new AbilityConditionSpec(type, param, zoneIds);
            })
            .create();

    private static Path dataDirectory;
    // player UUID -> ability id -> value (Boolean or Number)
    private static final Map<UUID, Map<String, Object>> STORAGE = new ConcurrentHashMap<>();
    // player UUID -> ability id -> list of conditions
    private static final Map<UUID, Map<String, List<AbilityConditionSpec>>> CONDITION_STORAGE = new ConcurrentHashMap<>();

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
    public static AbilityValue getAbility(@Nonnull UUID playerId, @Nonnull String abilityId) {
        Map<String, Object> abilities = STORAGE.get(playerId);
        if (abilities == null) return null;
        Object value = abilities.get(abilityId);
        return value != null ? new AbilityValue(value) : null;
    }

    public static boolean hasAbility(@Nonnull UUID playerId, @Nonnull String abilityId) {
        AbilityValue v = getAbility(playerId, abilityId);
        return v != null && v.isPresent() && v.asBoolean();
    }

    public static void setAbility(@Nonnull UUID playerId, @Nonnull String abilityId, @Nonnull Object value) {
        STORAGE
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(abilityId, value);
        saveData();
    }

    public static void removeAbility(@Nonnull UUID playerId, @Nonnull String abilityId) {
        Map<String, Object> abilities = STORAGE.get(playerId);
        if (abilities != null) {
            abilities.remove(abilityId);
            if (abilities.isEmpty()) STORAGE.remove(playerId);
        }
        Map<String, List<AbilityConditionSpec>> conditions = CONDITION_STORAGE.get(playerId);
        if (conditions != null) {
            conditions.remove(abilityId);
            if (conditions.isEmpty()) CONDITION_STORAGE.remove(playerId);
        }
        saveData();
    }

    @Nonnull
    public static Map<String, AbilityValue> getAllAbilities(@Nonnull UUID playerId) {
        Map<String, Object> abilities = STORAGE.get(playerId);
        if (abilities == null) return Collections.emptyMap();
        Map<String, AbilityValue> result = new HashMap<>();
        for (Map.Entry<String, Object> e : abilities.entrySet()) {
            result.put(e.getKey(), new AbilityValue(e.getValue()));
        }
        return result;
    }

    @Nullable
    public static List<AbilityConditionSpec> getConditions(@Nonnull UUID playerId, @Nonnull String abilityId) {
        Map<String, List<AbilityConditionSpec>> perPlayer = CONDITION_STORAGE.get(playerId);
        if (perPlayer == null) return null;
        List<AbilityConditionSpec> list = perPlayer.get(abilityId);
        return list != null ? List.copyOf(list) : null;
    }

    public static void setConditions(@Nonnull UUID playerId, @Nonnull String abilityId, @Nonnull List<AbilityConditionSpec> conditions) {
        CONDITION_STORAGE
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(abilityId, new ArrayList<>(conditions));
        saveData();
    }

    private static void loadData() {
        if (dataDirectory == null) return;
        Path dataFile = dataDirectory.resolve(DATA_FILE_NAME);
        if (!Files.exists(dataFile)) return;
        try {
            String json = Files.readString(dataFile);
            if (json == null || json.trim().isEmpty()) return;
            var typeToken = new TypeToken<Map<String, PlayerDataDto>>() {};
            Map<String, PlayerDataDto> loaded = GSON.fromJson(json, typeToken.getType());
            if (loaded != null) {
                STORAGE.clear();
                CONDITION_STORAGE.clear();
                for (Map.Entry<String, PlayerDataDto> e : loaded.entrySet()) {
                    UUID playerId = UUID.fromString(e.getKey());
                    PlayerDataDto dto = e.getValue();
                    if (dto != null && dto.abilities != null && !dto.abilities.isEmpty()) {
                        STORAGE.put(playerId, new ConcurrentHashMap<>(dto.abilities));
                    }
                    if (dto != null && dto.ability_conditions != null && !dto.ability_conditions.isEmpty()) {
                        Map<String, List<AbilityConditionSpec>> condMap = new ConcurrentHashMap<>();
                        for (Map.Entry<String, List<AbilityConditionSpec>> ce : dto.ability_conditions.entrySet()) {
                            if (ce.getValue() != null) {
                                condMap.put(ce.getKey(), new ArrayList<>(ce.getValue()));
                            }
                        }
                        if (!condMap.isEmpty()) {
                            CONDITION_STORAGE.put(playerId, condMap);
                        }
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
            Map<String, PlayerDataDto> toSave = new HashMap<>();
            for (UUID playerId : STORAGE.keySet()) {
                Map<String, Object> abilities = STORAGE.get(playerId);
                Map<String, List<AbilityConditionSpec>> conditions = CONDITION_STORAGE.get(playerId);
                if (abilities != null && !abilities.isEmpty()) {
                    PlayerDataDto dto = new PlayerDataDto();
                    dto.abilities = new HashMap<>(abilities);
                    if (conditions != null && !conditions.isEmpty()) {
                        dto.ability_conditions = new HashMap<>();
                        for (Map.Entry<String, List<AbilityConditionSpec>> ce : conditions.entrySet()) {
                            dto.ability_conditions.put(ce.getKey(), new ArrayList<>(ce.getValue()));
                        }
                    }
                    toSave.put(playerId.toString(), dto);
                }
            }
            Path dataFile = dataDirectory.resolve(DATA_FILE_NAME);
            String json = GSON.toJson(toSave);
            Files.writeString(dataFile, json);
        } catch (Exception e) {
            LOGGER.severe("Failed to save ability data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private static class PlayerDataDto {
        Map<String, Object> abilities;
        Map<String, List<AbilityConditionSpec>> ability_conditions;
    }
}
