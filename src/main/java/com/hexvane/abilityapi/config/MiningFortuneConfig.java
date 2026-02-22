package com.hexvane.abilityapi.config;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Loads the set of block type IDs that mining_fortune applies to (e.g. ores).
 * Config file: mining_fortune_blocks.json in plugin data directory, containing a JSON array of block IDs.
 * If the file does not exist, it is created with default base-game ore block IDs.
 */
public final class MiningFortuneConfig {
    private static final String CONFIG_FILE_NAME = "mining_fortune_blocks.json";
    private static final Gson GSON = new Gson();

    /** Default base-game ore block IDs (from game BlockTypeList/Ores.json). */
    private static final String[] DEFAULT_ORE_BLOCK_IDS = {
        "Ore_Adamantite_Basalt", "Ore_Adamantite_Shale", "Ore_Adamantite_Slate", "Ore_Adamantite_Stone", "Ore_Adamantite_Volcanic",
        "Ore_Cobalt_Basalt", "Ore_Cobalt_Sandstone", "Ore_Cobalt_Shale", "Ore_Cobalt_Slate", "Ore_Cobalt_Stone", "Ore_Cobalt_Volcanic",
        "Ore_Copper_Basalt", "Ore_Copper_Sandstone", "Ore_Copper_Shale", "Ore_Copper_Stone", "Ore_Copper_Volcanic",
        "Ore_Gold_Basalt", "Ore_Gold_Sandstone", "Ore_Gold_Shale", "Ore_Gold_Stone", "Ore_Gold_Volcanic",
        "Ore_Iron_Basalt", "Ore_Iron_Sandstone", "Ore_Iron_Shale", "Ore_Iron_Slate", "Ore_Iron_Stone", "Ore_Iron_Volcanic",
        "Ore_Mithril_Basalt", "Ore_Mithril_Magma", "Ore_Mithril_Slate", "Ore_Mithril_Stone", "Ore_Mithril_Volcanic",
        "Ore_Onyxium_Basalt", "Ore_Onyxium_Sandstone", "Ore_Onyxium_Shale", "Ore_Onyxium_Stone", "Ore_Onyxium_Volcanic",
        "Ore_Silver_Basalt", "Ore_Silver_Sandstone", "Ore_Silver_Shale", "Ore_Silver_Slate", "Ore_Silver_Stone", "Ore_Silver_Volcanic",
        "Ore_Thorium_Basalt", "Ore_Thorium_Sandstone", "Ore_Thorium_Shale", "Ore_Thorium_Stone", "Ore_Thorium_Volcanic"
    };

    private static Set<String> affectedBlockIds = Collections.emptySet();

    private MiningFortuneConfig() {}

    /**
     * Load config from the plugin data directory. Call once at startup.
     * If the file does not exist, it is created with default base-game ores.
     */
    public static void initialize(@Nonnull Path pluginDataDirectory) {
        Path configPath = pluginDataDirectory.resolve(CONFIG_FILE_NAME);
        if (!Files.isRegularFile(configPath)) {
            affectedBlockIds = new HashSet<>(Arrays.asList(DEFAULT_ORE_BLOCK_IDS));
            try {
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, GSON.toJson(Arrays.asList(DEFAULT_ORE_BLOCK_IDS)));
            } catch (IOException ignored) {
                // use in-memory default even if write fails
            }
            return;
        }
        try {
            String json = Files.readString(configPath);
            List<String> list = GSON.fromJson(json, new TypeToken<List<String>>() {}.getType());
            affectedBlockIds = list != null ? new HashSet<>(list) : new HashSet<>();
        } catch (IOException e) {
            affectedBlockIds = new HashSet<>(Arrays.asList(DEFAULT_ORE_BLOCK_IDS));
        }
    }

    @Nonnull
    public static Set<String> getAffectedBlockIds() {
        return affectedBlockIds;
    }
}
