package dev.lhoopy.content;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.config.ConfigProvider;
import dev.lhoopy.core.config.ConfigValidationException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ContentLoader implements ConfigProvider<GameContent> {
    private static final String SLIMES_CONFIG_FILE = "slimes.yml";
    private static final String LOCATIONS_CONFIG_FILE = "locations.yml";
    private static final String FOODS_CONFIG_FILE = "foods.yml";
    private static final String PLANTS_CONFIG_FILE = "plants.yml";
    private static final String PLORTS_CONFIG_FILE = "plorts.yml";
    private static final String RESOURCES_CONFIG_FILE = "resources.yml";
    private static final String PENS_CONFIG_FILE = "pens.yml";
    private static final String RECIPES_CONFIG_FILE = "recipes.yml";

    private final SlimesPlugin plugin;
    private final ContentValidator validator = new ContentValidator();

    public ContentLoader(SlimesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public GameContent load() {
        GameContent content = new GameContent(
                loadSlimes(),
                loadLocations(),
                loadFoods(),
                loadPlants(),
                loadPlorts(),
                loadResources(),
                loadPens(),
                loadRecipes()
        );
        this.validator.validate(content);
        return content;
    }

    private Map<String, SlimeDef> loadSlimes() {
        File file = new File(this.plugin.getDataFolder(), SLIMES_CONFIG_FILE);
        if (!file.isFile()) {
            this.plugin.saveResource(SLIMES_CONFIG_FILE, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("slimes");
        if (section == null) {
            throw new ConfigValidationException("Missing 'slimes' section in " + SLIMES_CONFIG_FILE);
        }

        Map<String, SlimeDef> definitions = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection slime = section.getConfigurationSection(id);
            if (slime == null) {
                continue;
            }

            String normalizedId = normalize(id);
            String displayName = slime.getString("display-name", id);
            Material material = Material.matchMaterial(slime.getString("item-material", "SLIME_BALL"));
            if (material == null) {
                throw new ConfigValidationException("Unknown item-material for slime '" + id + "'.");
            }

            Material favoriteFood = Material.matchMaterial(slime.getString("favorite-food", "APPLE"));
            if (favoriteFood == null) {
                throw new ConfigValidationException("Unknown favorite-food for slime '" + id + "'.");
            }

            String rarity = slime.getString("rarity", "common");
            int captureDifficulty = slime.getInt("capture-difficulty", 1);
            if (captureDifficulty < 1 || captureDifficulty > 10) {
                throw new ConfigValidationException("Slime '" + id + "' has invalid capture-difficulty " + captureDifficulty + ". Expected 1..10.");
            }

            int sellPrice = slime.getInt("sell-price", 10);
            if (sellPrice < 0) {
                throw new ConfigValidationException("Slime '" + id + "' has negative sell-price.");
            }

            int interestSeconds = slime.getInt("interest-seconds", 30);
            if (interestSeconds < 1 || interestSeconds > 300) {
                throw new ConfigValidationException("Slime '" + id + "' has invalid interest-seconds " + interestSeconds + ". Expected 1..300.");
            }

            int size = slime.getInt("size", 2);
            if (size < 1 || size > 4) {
                throw new ConfigValidationException("Slime '" + id + "' has invalid size " + size + ". Expected 1..4.");
            }

            definitions.put(normalizedId, new SlimeDef(
                    normalizedId,
                    displayName,
                    material,
                    favoriteFood,
                    rarity,
                    captureDifficulty,
                    sellPrice,
                    interestSeconds,
                    size
            ));
        }

        if (definitions.isEmpty()) {
            throw new ConfigValidationException("No slime definitions found in " + SLIMES_CONFIG_FILE);
        }
        return definitions;
    }

    private Map<String, LocationDef> loadLocations() {
        YamlConfiguration config = loadConfig(LOCATIONS_CONFIG_FILE);
        ConfigurationSection section = requireSection(config, "locations", LOCATIONS_CONFIG_FILE);
        Map<String, LocationDef> definitions = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection location = section.getConfigurationSection(id);
            if (location == null) {
                continue;
            }
            String normalizedId = normalize(id);
            definitions.put(normalizedId, new LocationDef(
                    normalizedId,
                    location.getString("display-name", id),
                    location.getInt("tier", 1),
                    normalizeList(location.getStringList("normal-slimes")),
                    normalizeList(location.getStringList("secret-slimes")),
                    normalizeList(location.getStringList("resources")),
                    normalizeList(location.getStringList("unlock-requirements")),
                    normalize(location.getString("completion-reward", ""))
            ));
        }
        return definitions;
    }

    private Map<String, FoodDef> loadFoods() {
        YamlConfiguration config = loadConfig(FOODS_CONFIG_FILE);
        ConfigurationSection section = requireSection(config, "foods", FOODS_CONFIG_FILE);
        Map<String, FoodDef> definitions = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection food = section.getConfigurationSection(id);
            if (food == null) {
                continue;
            }
            String normalizedId = normalize(id);
            definitions.put(normalizedId, new FoodDef(
                    normalizedId,
                    food.getString("display-name", id),
                    normalize(food.getString("type", "generic")),
                    readMaterials(food, normalizedId)
            ));
        }
        return definitions;
    }

    private Map<String, PlantDef> loadPlants() {
        YamlConfiguration config = loadConfig(PLANTS_CONFIG_FILE);
        ConfigurationSection section = requireSection(config, "plants", PLANTS_CONFIG_FILE);
        Map<String, PlantDef> definitions = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection plant = section.getConfigurationSection(id);
            if (plant == null) {
                continue;
            }
            String normalizedId = normalize(id);
            definitions.put(normalizedId, new PlantDef(
                    normalizedId,
                    plant.getString("display-name", id),
                    normalize(plant.getString("seed-id", "seed_" + normalizedId)),
                    normalize(plant.getString("plot-type", "basic")),
                    normalize(plant.getString("output-food", "plant_sweetroot")),
                    plant.getInt("growth-seconds", 300),
                    plant.getInt("seed-price", 5),
                    plant.getInt("harvest-amount", 3)
            ));
        }
        return definitions;
    }

    private Map<String, PlortDef> loadPlorts() {
        YamlConfiguration config = loadConfig(PLORTS_CONFIG_FILE);
        ConfigurationSection section = requireSection(config, "plorts", PLORTS_CONFIG_FILE);
        Map<String, PlortDef> definitions = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection plort = section.getConfigurationSection(id);
            if (plort == null) {
                continue;
            }
            String normalizedId = normalize(id);
            definitions.put(normalizedId, new PlortDef(
                    normalizedId,
                    plort.getString("display-name", id),
                    plort.getInt("base-price", 10)
            ));
        }
        return definitions;
    }

    private Map<String, ResourceDef> loadResources() {
        YamlConfiguration config = loadConfig(RESOURCES_CONFIG_FILE);
        ConfigurationSection section = requireSection(config, "resources", RESOURCES_CONFIG_FILE);
        Map<String, ResourceDef> definitions = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection resource = section.getConfigurationSection(id);
            if (resource == null) {
                continue;
            }
            String normalizedId = normalize(id);
            definitions.put(normalizedId, new ResourceDef(
                    normalizedId,
                    resource.getString("display-name", id),
                    normalize(resource.getString("location", "unknown")),
                    normalize(resource.getString("rarity", "common")),
                    resource.getString("spawn-zone", ""),
                    resource.getString("use", "")
            ));
        }
        return definitions;
    }

    private Map<String, PenDef> loadPens() {
        YamlConfiguration config = loadConfig(PENS_CONFIG_FILE);
        ConfigurationSection section = requireSection(config, "pens", PENS_CONFIG_FILE);
        Map<String, PenDef> definitions = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection pen = section.getConfigurationSection(id);
            if (pen == null) {
                continue;
            }
            String normalizedId = normalize(id);
            definitions.put(normalizedId, new PenDef(
                    normalizedId,
                    pen.getString("display-name", id),
                    pen.getInt("base-capacity", 6),
                    pen.getInt("upgrade-price", 250)
            ));
        }
        return definitions;
    }

    private Map<String, RecipeDef> loadRecipes() {
        YamlConfiguration config = loadConfig(RECIPES_CONFIG_FILE);
        ConfigurationSection section = requireSection(config, "recipes", RECIPES_CONFIG_FILE);
        Map<String, RecipeDef> definitions = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection recipe = section.getConfigurationSection(id);
            if (recipe == null) {
                continue;
            }
            String normalizedId = normalize(id);
            Map<String, Integer> ingredients = new LinkedHashMap<>();
            ConfigurationSection ingredientsSection = recipe.getConfigurationSection("ingredients");
            if (ingredientsSection != null) {
                for (String ingredientId : ingredientsSection.getKeys(false)) {
                    ingredients.put(normalize(ingredientId), ingredientsSection.getInt(ingredientId));
                }
            }
            definitions.put(normalizedId, new RecipeDef(
                    normalizedId,
                    normalize(recipe.getString("station", "farmer_table")),
                    normalize(recipe.getString("category", "general")),
                    normalize(recipe.getString("result", "")),
                    recipe.getInt("result-amount", 1),
                    ingredients,
                    normalizeList(recipe.getStringList("unlock-requirements")),
                    normalizeList(recipe.getStringList("flag-requirements")),
                    recipe.getLong("coin-cost", 0L),
                    recipe.getDouble("success-chance", 1.0D),
                    recipe.getInt("max-crafts-per-action", 64)
            ));
        }
        return definitions;
    }

    private static List<Material> readMaterials(ConfigurationSection food, String foodId) {
        List<Material> materials = new ArrayList<>();
        for (String raw : food.getStringList("materials")) {
            Material material = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
            if (material == null) {
                throw new ConfigValidationException(
                        "Food '" + foodId + "' has unknown material: " + raw);
            }
            materials.add(material);
        }
        return materials;
    }

    private YamlConfiguration loadConfig(String fileName) {
        File file = new File(this.plugin.getDataFolder(), fileName);
        if (!file.isFile()) {
            this.plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private static ConfigurationSection requireSection(YamlConfiguration config, String sectionName, String fileName) {
        ConfigurationSection section = config.getConfigurationSection(sectionName);
        if (section == null) {
            throw new ConfigValidationException("Missing '" + sectionName + "' section in " + fileName);
        }
        return section;
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static java.util.List<String> normalizeList(java.util.List<String> values) {
        java.util.List<String> normalized = new java.util.ArrayList<>();
        for (String value : values) {
            normalized.add(normalize(value));
        }
        return normalized;
    }
}
