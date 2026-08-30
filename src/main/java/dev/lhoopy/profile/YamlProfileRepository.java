package dev.lhoopy.profile;

import dev.lhoopy.farm.FarmPlot;
import dev.lhoopy.pen.PenSlime;
import dev.lhoopy.pen.PenFeedQuality;
import dev.lhoopy.quest.QuestProgress;
import dev.lhoopy.storage.StoredItem;
import dev.lhoopy.storage.PlayerStorage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class YamlProfileRepository implements ProfileRepository {
    private final Plugin plugin;
    private final File directory;

    public YamlProfileRepository(Plugin plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "profiles");
    }

    @Override
    public CompletableFuture<PlayerProfile> load(UUID playerId) {
        File file = fileFor(playerId);
        if (!file.exists()) {
            return CompletableFuture.completedFuture(new PlayerProfile(playerId, 0L));
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerProfile profile = new PlayerProfile(playerId, config.getLong("coins", 0L));
        profile.setPenCapacity(config.getInt("farm.pen-capacity", 6));
        profile.setPenCaseKeys(config.getInt("farm.pen-case-keys", 0));
        profile.setActivePenStyleId(config.getString("farm.active-pen-style", "pen_basic"));
        ConfigurationSection blueprints = config.getConfigurationSection("farm.pen-blueprints");
        if (blueprints != null) {
            Map<String, Integer> owned = new LinkedHashMap<>();
            for (String styleId : blueprints.getKeys(false)) {
                owned.put(styleId, blueprints.getInt(styleId, 0));
            }
            profile.setPenBlueprints(owned);
        }
        profile.setCapturedSlimeIds(config.getStringList("farm.captured-slimes"));
        readPenSlimes(config, profile);
        profile.setLastPlortProductionMillis(config.getLong("farm.last-plort-production", 0L));
        readStorage(profile.getPenPlortStorage(), config.getConfigurationSection("farm.pen-plorts"));
        ConfigurationSection plots = config.getConfigurationSection("farm.plots");
        if (plots != null) {
            for (String plotId : plots.getKeys(false)) {
                ConfigurationSection plotSection = plots.getConfigurationSection(plotId);
                if (plotSection == null) {
                    continue;
                }
                FarmPlot plot = profile.getFarmData().getOrCreatePlot(plotId);
                plot.setPlotTypeId(plotSection.getString("plot-type", "basic"));
                plot.setPlantId(plotSection.getString("plant-id"));
                plot.setPlantedAtMillis(plotSection.getLong("planted-at", 0L));
                plot.setWateredUntilMillis(plotSection.getLong("watered-until", 0L));
                plot.setGrowthProgressMillis(plotSection.getLong("growth-progress", 0L));
                plot.setLastGrowthUpdateMillis(plotSection.getLong("last-growth-update", plot.getPlantedAtMillis()));
            }
        }
        readStorage(profile.getStorage(), config.getConfigurationSection("storage"));
        int legacyCapacity = config.getInt("vacpack.capacity", 32);
        profile.setVacpackPlortCapacity(config.getInt("vacpack.limits.plorts", legacyCapacity));
        profile.setVacpackSlimeCapacity(config.getInt("vacpack.limits.slimes", 4));
        profile.setVacpackFoodCapacity(config.getInt("vacpack.limits.food", 64));
        profile.setVacpackSeedCapacity(config.getInt("vacpack.limits.seeds", 64));
        profile.setVacpackResourceCapacity(config.getInt("vacpack.limits.resources", 64));
        profile.setVacpackOtherCapacity(config.getInt("vacpack.limits.other", 32));
        readStorage(profile.getVacpackStorage(), config.getConfigurationSection("vacpack.items"));
        profile.getProgressData().setUnlockedIds(config.getStringList("progression.unlocks"));
        profile.getProgressData().setFlags(config.getStringList("progression.flags"));
        profile.getProgressData().setSkillPoints(config.getInt("progression.skill-points", 0));
        profile.getProgressData().setRebirths(config.getInt("progression.rebirths", 0));
        profile.getQuestData().setBattlePassLevel(config.getInt("quests.battle-pass.level", 0));
        profile.getQuestData().setBattlePassExperience(config.getInt("quests.battle-pass.experience", 0));
        profile.getQuestData().setPremiumBattlePass(config.getBoolean("quests.battle-pass.premium", false));
        profile.getQuestData().setClaimedBattlePassRewards(config.getStringList("quests.battle-pass.claimed-rewards"));
        ConfigurationSection quests = config.getConfigurationSection("quests.entries");
        if (quests != null) {
            profile.getQuestData().clearQuests();
            for (String questId : quests.getKeys(false)) {
                ConfigurationSection questSection = quests.getConfigurationSection(questId);
                if (questSection == null) {
                    continue;
                }
                QuestProgress progress = profile.getQuestData().getOrCreate(questId);
                progress.setValue(questSection.getInt("value", 0));
                progress.setCompleted(questSection.getBoolean("completed", false));
                progress.setRewardClaimed(questSection.getBoolean("reward-claimed", false));
            }
        }
        ConfigurationSection resourceNodes = config.getConfigurationSection("resource-nodes");
        if (resourceNodes != null) {
            long now = System.currentTimeMillis();
            for (String nodeKey : resourceNodes.getKeys(false)) {
                long respawnAt = resourceNodes.getLong(nodeKey, 0L);
                if (respawnAt > now) {
                    profile.setResourceNodeRespawn(nodeKey, respawnAt);
                }
            }
        }
        return CompletableFuture.completedFuture(profile);
    }

    @Override
    public CompletableFuture<Void> save(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            if (!this.directory.exists() && !this.directory.mkdirs()) {
                this.plugin.getLogger().warning("Could not create profile directory: " + this.directory.getAbsolutePath());
                return;
            }

            YamlConfiguration config = new YamlConfiguration();
            config.set("player-id", profile.getPlayerId().toString());
            config.set("coins", profile.getCoins());
            config.set("farm.pen-capacity", profile.getPenCapacity());
            config.set("farm.pen-case-keys", profile.getPenCaseKeys());
            config.set("farm.active-pen-style", profile.getActivePenStyleId());
            profile.getPenBlueprints().forEach((id, amount) -> config.set("farm.pen-blueprints." + id, amount));
            config.set("farm.captured-slimes", copy(profile.getCapturedSlimeIds()));
            config.set("farm.pen-slimes", writePenSlimes(profile));
            config.set("farm.last-plort-production", profile.getLastPlortProductionMillis());
            writeStorage(config, "farm.pen-plorts", profile.getPenPlortStorage());
            for (FarmPlot plot : profile.getFarmData().getPlots()) {
                String path = "farm.plots." + plot.getId();
                config.set(path + ".plot-type", plot.getPlotTypeId());
                config.set(path + ".plant-id", plot.getPlantId());
                config.set(path + ".planted-at", plot.getPlantedAtMillis());
                config.set(path + ".watered-until", plot.getWateredUntilMillis());
                config.set(path + ".growth-progress", plot.getGrowthProgressMillis());
                config.set(path + ".last-growth-update", plot.getLastGrowthUpdateMillis());
            }
            writeStorage(config, "storage", profile.getStorage());
            config.set("vacpack.capacity", profile.getVacpackPlortCapacity());
            config.set("vacpack.limits.plorts", profile.getVacpackPlortCapacity());
            config.set("vacpack.limits.slimes", profile.getVacpackSlimeCapacity());
            config.set("vacpack.limits.food", profile.getVacpackFoodCapacity());
            config.set("vacpack.limits.seeds", profile.getVacpackSeedCapacity());
            config.set("vacpack.limits.resources", profile.getVacpackResourceCapacity());
            config.set("vacpack.limits.other", profile.getVacpackOtherCapacity());
            writeStorage(config, "vacpack.items", profile.getVacpackStorage());
            config.set("progression.unlocks", copy(profile.getProgressData().getUnlockedIds()));
            config.set("progression.flags", copy(profile.getProgressData().getFlags()));
            config.set("progression.skill-points", profile.getProgressData().getSkillPoints());
            config.set("progression.rebirths", profile.getProgressData().getRebirths());
            config.set("quests.battle-pass.level", profile.getQuestData().getBattlePassLevel());
            config.set("quests.battle-pass.experience", profile.getQuestData().getBattlePassExperience());
            config.set("quests.battle-pass.premium", profile.getQuestData().hasPremiumBattlePass());
            config.set("quests.battle-pass.claimed-rewards", copy(profile.getQuestData().getClaimedBattlePassRewards()));
            for (QuestProgress progress : profile.getQuestData().getQuests()) {
                String path = "quests.entries." + progress.getQuestId();
                config.set(path + ".value", progress.getValue());
                config.set(path + ".completed", progress.isCompleted());
                config.set(path + ".reward-claimed", progress.isRewardClaimed());
            }
            profile.removeExpiredResourceNodeRespawns(System.currentTimeMillis());
            for (Map.Entry<String, Long> entry : profile.getResourceNodeRespawns().entrySet()) {
                config.set("resource-nodes." + entry.getKey(), entry.getValue());
            }

            try {
                config.save(fileFor(profile.getPlayerId()));
            } catch (IOException error) {
                this.plugin.getLogger().log(Level.WARNING, "Could not save profile " + profile.getPlayerId(), error);
            }
        });
    }

    private File fileFor(UUID playerId) {
        return new File(this.directory, playerId + ".yml");
    }

    private static void readStorage(PlayerStorage target, ConfigurationSection storage) {
        if (storage == null) {
            return;
        }
        for (String itemId : storage.getKeys(false)) {
            ConfigurationSection item = storage.getConfigurationSection(itemId);
            if (item == null) {
                continue;
            }
            target.set(
                    itemId,
                    item.getInt("amount", 0),
                    item.getBoolean("protected", false)
            );
        }
    }

    private static void writeStorage(YamlConfiguration config, String rootPath, PlayerStorage source) {
        for (StoredItem item : source.getItems()) {
            if (item.getAmount() <= 0 && !item.isProtectedItem()) {
                continue;
            }
            String path = rootPath + "." + item.getItemId();
            config.set(path + ".amount", item.getAmount());
            config.set(path + ".protected", item.isProtectedItem());
        }
    }

    private static void readPenSlimes(YamlConfiguration config, PlayerProfile profile) {
        List<?> raw = config.getList("farm.pen-slimes");
        if (raw == null) {
            profile.setPenSlimeIds(Collections.emptyList());
            return;
        }

        List<PenSlime> penSlimes = new ArrayList<>();
        List<String> legacySlimes = new ArrayList<>();
        for (Object entry : raw) {
            if (entry instanceof Map<?, ?>) {
                Map<?, ?> slime = (Map<?, ?>) entry;
                Object id = slime.get("slime-id");
                if (id == null) {
                    id = slime.get("slimeId");
                }
                if (id != null) {
                    long fedUntil = readLong(slime.get("fed-until"));
                    penSlimes.add(new PenSlime(
                            String.valueOf(id),
                            readLong(slime.get("last-fed")),
                            fedUntil,
                            PenFeedQuality.read(valueAsString(slime.get("feed-quality")),
                                    fedUntil > 0L ? PenFeedQuality.FAVORITE : PenFeedQuality.HUNGRY),
                            readDouble(slime.get("production-remainder"))
                    ));
                }
                continue;
            }
            if (entry != null) {
                String slimeId = readLegacySlimeId(String.valueOf(entry));
                if (slimeId != null) {
                    legacySlimes.add(slimeId);
                }
            }
        }

        if (!penSlimes.isEmpty()) {
            profile.setPenSlimes(penSlimes);
        } else {
            profile.setPenSlimeIds(legacySlimes);
        }
    }

    private static List<Map<String, Object>> writePenSlimes(PlayerProfile profile) {
        List<Map<String, Object>> slimes = new ArrayList<>();
        for (PenSlime slime : profile.getPenSlimes()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("slime-id", slime.getSlimeId());
            entry.put("last-fed", slime.getLastFedMillis());
            entry.put("fed-until", slime.getFedUntilMillis());
            entry.put("feed-quality", slime.getStoredFeedQuality().name());
            entry.put("production-remainder", slime.getProductionRemainder());
            slimes.add(entry);
        }
        return slimes;
    }

    private static long readLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static double readDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return 0.0D;
            }
        }
        return 0.0D;
    }

    private static String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String readLegacySlimeId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        String slimeId = readBetween(trimmed, "slimeId=", ",");
        if (slimeId == null) {
            slimeId = readBetween(trimmed, "slime-id=", ",");
        }
        if (slimeId != null) {
            return cleanLegacySlimeId(slimeId);
        }
        if (trimmed.contains("fedUntil=") || trimmed.contains("lastFed=") || trimmed.contains("data={")) {
            return null;
        }
        return cleanLegacySlimeId(trimmed);
    }

    private static String readBetween(String value, String prefix, String suffix) {
        int start = value.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = value.indexOf(suffix, start);
        if (end < 0) {
            end = value.indexOf('}', start);
        }
        if (end < 0) {
            end = value.length();
        }
        return value.substring(start, end);
    }

    private static String cleanLegacySlimeId(String slimeId) {
        String cleaned = slimeId == null ? "" : slimeId.trim();
        while (cleaned.endsWith("}") || cleaned.endsWith("]")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static List<String> copy(Iterable<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        java.util.ArrayList<String> copy = new java.util.ArrayList<>();
        for (String value : values) {
            copy.add(value);
        }
        return copy;
    }
}
