package dev.lhoopy.content;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FarmContentCatalogTest {
    private static final int EXPECTED_PLANTS = 31;

    @Test
    void everyConfiguredPlantHasFoodAndFarmerTableRecipe() {
        ConfigurationSection plants = section(load("plants.yml"), "plants");
        ConfigurationSection foods = section(load("foods.yml"), "foods");
        ConfigurationSection recipes = section(load("recipes.yml"), "recipes");

        Map<String, ConfigurationSection> recipesBySeed = new HashMap<>();
        for (String recipeId : recipes.getKeys(false)) {
            ConfigurationSection recipe = recipes.getConfigurationSection(recipeId);
            if (recipe != null && "farmer_table".equals(recipe.getString("station"))) {
                String seedId = recipe.getString("result");
                assertTrue(!recipesBySeed.containsKey(seedId), "Duplicate recipe for " + seedId);
                recipesBySeed.put(seedId, recipe);
            }
        }

        assertEquals(EXPECTED_PLANTS, plants.getKeys(false).size(), "Unexpected plant catalog size");
        assertEquals(EXPECTED_PLANTS, recipesBySeed.size(), "Every plant needs one seed recipe");
        for (String plantId : plants.getKeys(false)) {
            ConfigurationSection plant = plants.getConfigurationSection(plantId);
            assertNotNull(plant, plantId);
            String seedId = plant.getString("seed-id");
            String outputFood = plant.getString("output-food");
            String plotType = plant.getString("plot-type");
            assertTrue(foods.isConfigurationSection(outputFood), plantId + " has no output food " + outputFood);

            ConfigurationSection recipe = recipesBySeed.get(seedId);
            assertNotNull(recipe, plantId + " has no farmer table recipe for " + seedId);
            assertEquals("plot_" + plotType, recipe.getString("category"), plantId + " recipe category");
        }
    }

    private static YamlConfiguration load(String name) {
        InputStream stream = FarmContentCatalogTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static ConfigurationSection section(YamlConfiguration configuration, String path) {
        ConfigurationSection section = configuration.getConfigurationSection(path);
        assertNotNull(section, path);
        return section;
    }
}
