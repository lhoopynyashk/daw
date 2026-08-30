package dev.lhoopy.pen;

import dev.lhoopy.profile.PlayerProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PenStyleCatalog {
    public static final String BASIC_STYLE_ID = "pen_basic";

    private final Map<String, PenStyleDef> definitions = new LinkedHashMap<>();
    private final List<PenStyleDef> casePool = new ArrayList<>();

    public PenStyleCatalog(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "pen-styles.yml");
        if (!file.exists()) {
            plugin.saveResource("pen-styles.yml", false);
        }
        load(YamlConfiguration.loadConfiguration(file));
    }

    private void load(YamlConfiguration config) {
        ConfigurationSection styles = config.getConfigurationSection("styles");
        if (styles == null) {
            throw new IllegalStateException("pen-styles.yml has no styles section");
        }
        double totalChance = 0.0D;
        for (String id : styles.getKeys(false)) {
            ConfigurationSection section = styles.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            PenStyleDef definition = new PenStyleDef(
                    id,
                    section.getString("name", id),
                    section.getString("rarity", "common"),
                    section.getString("description", ""),
                    section.getDouble("chance", 0.0D),
                    section.getDouble("effects.production-multiplier", 1.0D),
                    section.getDouble("effects.sell-multiplier", 1.0D),
                    section.getDouble("effects.extra-plort-chance", 0.0D),
                    section.getDouble("effects.food-use-multiplier", 1.0D),
                    section.getInt("effects.capacity-bonus", 0),
                    section.getBoolean("effects.automatic-collection", false)
            );
            this.definitions.put(id, definition);
            if (definition.getChance() > 0.0D) {
                this.casePool.add(definition);
                totalChance += definition.getChance();
            }
        }
        if (!this.definitions.containsKey(BASIC_STYLE_ID)) {
            throw new IllegalStateException("pen-styles.yml must define " + BASIC_STYLE_ID);
        }
        if (Math.abs(totalChance - 100.0D) > 0.001D) {
            throw new IllegalStateException("Pen case chances must total 100, got " + totalChance);
        }
    }

    public PenStyleDef get(String id) {
        PenStyleDef definition = this.definitions.get(id);
        return definition == null ? this.definitions.get(BASIC_STYLE_ID) : definition;
    }

    public List<PenStyleDef> all() {
        return Collections.unmodifiableList(new ArrayList<>(this.definitions.values()));
    }

    public PenStyleDef select(double roll) {
        double cursor = 0.0D;
        for (PenStyleDef definition : this.casePool) {
            cursor += definition.getChance();
            if (roll < cursor) {
                return definition;
            }
        }
        return this.casePool.get(this.casePool.size() - 1);
    }

    public int effectiveCapacity(PlayerProfile profile) {
        return Math.max(1, profile.getPenCapacity() + get(profile.getActivePenStyleId()).getCapacityBonus());
    }
}
