package dev.lhoopy.quest.battlepass;

import gg.cristalix.wada.common.economy.Quality;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BattlePassContentLoader {
    private static final String SETTINGS_FILE = "battlepass-settings.yml";
    private static final String REWARDS_FILE = "battlepass-rewards.yml";
    private static final String QUESTS_FILE = "battlepass-quests.yml";

    private final Plugin plugin;

    public BattlePassContentLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public BattlePassContent load() {
        saveMissing(SETTINGS_FILE);
        saveMissing(REWARDS_FILE);
        saveMissing(QUESTS_FILE);

        YamlConfiguration settings = loadYaml(SETTINGS_FILE);
        YamlConfiguration rewards = loadYaml(REWARDS_FILE);
        YamlConfiguration quests = loadYaml(QUESTS_FILE);

        return new BattlePassContent(
                settings.getString("title", "SlimeRancher Pass"),
                settings.getInt("max-level", 30),
                settings.getInt("experience-per-level", 1000),
                settings.getInt("duration-days", 30),
                settings.getInt("purchase.price", 0),
                settings.getInt("gift.price", 0),
                settings.getInt("special.price", 0),
                readRewards(rewards),
                readQuests(quests)
        );
    }

    private List<BattlePassRewardDef> readRewards(YamlConfiguration config) {
        List<BattlePassRewardDef> result = new ArrayList<>();
        ConfigurationSection levels = config.getConfigurationSection("levels");
        if (levels == null) {
            return result;
        }

        for (String levelKey : levels.getKeys(false)) {
            int level = parseInt(levelKey, 1);
            ConfigurationSection levelSection = levels.getConfigurationSection(levelKey);
            if (levelSection == null) {
                continue;
            }
            readTrack(result, level, BattlePassRewardTrack.DEFAULT, levelSection.getConfigurationSection("default"));
            readTrack(result, level, BattlePassRewardTrack.PREMIUM, levelSection.getConfigurationSection("premium"));
        }
        return result;
    }

    private void readTrack(List<BattlePassRewardDef> result, int level, BattlePassRewardTrack track, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        int index = 0;
        for (String key : section.getKeys(false)) {
            ConfigurationSection reward = section.getConfigurationSection(key);
            if (reward == null) {
                continue;
            }
            result.add(new BattlePassRewardDef(
                    level,
                    index++,
                    track,
                    reward.getString("title", "Награда"),
                    reward.getString("description", ""),
                    quality(reward.getString("quality", "common")),
                    material(reward.getString("icon", "CHEST")),
                    readActions(reward.getConfigurationSection("actions"))
            ));
        }
    }

    private List<BattlePassRewardAction> readActions(ConfigurationSection section) {
        List<BattlePassRewardAction> result = new ArrayList<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection action = section.getConfigurationSection(key);
            if (action == null) {
                continue;
            }
            result.add(new BattlePassRewardAction(
                    action.getString("type", "storage"),
                    action.getString("item-id"),
                    action.getInt("amount", 1)
            ));
        }
        return result;
    }

    private List<BattlePassQuestDef> readQuests(YamlConfiguration config) {
        List<BattlePassQuestDef> result = new ArrayList<>();
        ConfigurationSection quests = config.getConfigurationSection("quests");
        if (quests == null) {
            return result;
        }
        for (String questId : quests.getKeys(false)) {
            ConfigurationSection quest = quests.getConfigurationSection(questId);
            if (quest == null) {
                continue;
            }
            result.add(new BattlePassQuestDef(
                    questId,
                    quest.getString("category", "daily"),
                    quest.getString("title", questId),
                    quest.getString("description", ""),
                    quest.getInt("target", 1),
                    quest.getInt("experience", 100)
            ));
        }
        return result;
    }

    private void saveMissing(String resourcePath) {
        File file = new File(this.plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            this.plugin.saveResource(resourcePath, false);
        }
    }

    private YamlConfiguration loadYaml(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(this.plugin.getDataFolder(), fileName));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Material material(String value) {
        Material material = Material.matchMaterial(value == null ? "CHEST" : value.toUpperCase(Locale.ROOT));
        return material == null ? Material.CHEST : material;
    }

    private static Quality quality(String value) {
        if (value == null) {
            return Quality.COMMON;
        }
        switch (value.toLowerCase(Locale.ROOT)) {
            case "uncommon":
                return Quality.UNCOMMON;
            case "rare":
                return Quality.RARE;
            case "epic":
                return Quality.EPIC;
            case "legendary":
                return Quality.LEGENDARY;
            case "incredible":
                return Quality.INCREDIBLE;
            case "common":
            default:
                return Quality.COMMON;
        }
    }
}
