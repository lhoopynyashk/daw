package dev.lhoopy.content;

import dev.lhoopy.core.config.ConfigValidationException;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ContentValidator {
    private static final Set<String> PLOT_TYPES = Set.of("basic", "wet", "mycelium", "hot", "crystal", "sky");

    public void validate(GameContent content) {
        requireNotEmpty(content.getSlimes(), "slimes");
        requireNotEmpty(content.getLocations(), "locations");
        requireNotEmpty(content.getFoods(), "foods");
        requireNotEmpty(content.getPlorts(), "plorts");
        requireNotEmpty(content.getResources(), "resources");
        requireNotEmpty(content.getPens(), "pens");

        Map<Material, String> materialOwners = new HashMap<>();
        for (FoodDef food : content.getFoods().values()) {
            for (Material material : food.getMaterials()) {
                String previous = materialOwners.put(material, food.getId());
                if (previous != null) {
                    throw new ConfigValidationException("Material " + material
                            + " is claimed by two foods: " + previous + " and " + food.getId());
                }
            }
        }
        for (SlimeDef slime : content.getSlimes().values()) {
            if (!materialOwners.containsKey(slime.getFavoriteFood())) {
                throw new ConfigValidationException("Slime '" + slime.getId()
                        + "' has favorite-food " + slime.getFavoriteFood()
                        + ", but no food in foods.yml lists this material.");
            }
        }

        Set<String> seedIds = new HashSet<>();
        for (PlantDef plant : content.getPlants().values()) {
            requireNotBlank(plant.getSeedId(), "Plant '" + plant.getId() + "' has empty seed-id.");
            requireNotBlank(plant.getPlotTypeId(), "Plant '" + plant.getId() + "' has empty plot-type.");
            if (!PLOT_TYPES.contains(plant.getPlotTypeId())) {
                throw new ConfigValidationException("Plant '" + plant.getId() + "' has unsupported plot-type: " + plant.getPlotTypeId());
            }
            requireKnown(content.getFoods(), plant.getOutputFoodId(), "Plant '" + plant.getId() + "' has unknown output-food.");
            requirePositive(plant.getGrowthSeconds(), "Plant '" + plant.getId() + "' has invalid growth-seconds.");
            requireNonNegative(plant.getSeedPrice(), "Plant '" + plant.getId() + "' has negative seed-price.");
            requirePositive(plant.getHarvestAmount(), "Plant '" + plant.getId() + "' has invalid harvest-amount.");
            if (!seedIds.add(plant.getSeedId())) {
                throw new ConfigValidationException("Duplicate plant seed-id: " + plant.getSeedId());
            }
        }

        requireKnown(content.getPlorts(), ContentIds.FALLBACK_PLORT_ID, "Content must define fallback plort.");

        for (LocationDef location : content.getLocations().values()) {
            requireNonNegative(location.getTier(), "Location '" + location.getId() + "' has invalid tier.");
            if (location.getNormalSlimeIds().isEmpty()) {
                throw new ConfigValidationException("Location '" + location.getId() + "' has no normal-slimes.");
            }
            for (String slimeId : location.getNormalSlimeIds()) {
                requireKnown(content.getSlimes(), slimeId, "Location '" + location.getId() + "' has unknown normal slime.");
            }
            for (String slimeId : location.getSecretSlimeIds()) {
                requireKnown(content.getSlimes(), slimeId, "Location '" + location.getId() + "' has unknown secret slime.");
            }
            for (String resourceId : location.getResourceIds()) {
                requireKnown(content.getResources(), resourceId, "Location '" + location.getId() + "' has unknown resource.");
            }
        }

        for (PlortDef plort : content.getPlorts().values()) {
            requireNonNegative(plort.getBasePrice(), "Plort '" + plort.getId() + "' has negative base-price.");
        }

        for (ResourceDef resource : content.getResources().values()) {
            requireNotBlank(resource.getDisplayName(), "Resource '" + resource.getId() + "' has empty display-name.");
            requireNotBlank(resource.getLocationId(), "Resource '" + resource.getId() + "' has empty location.");
            requireKnown(content.getLocations(), resource.getLocationId(), "Resource '" + resource.getId() + "' has unknown location.");
        }

        for (PenDef pen : content.getPens().values()) {
            requirePositive(pen.getBaseCapacity(), "Pen '" + pen.getId() + "' has invalid base-capacity.");
            requireNonNegative(pen.getUpgradePrice(), "Pen '" + pen.getId() + "' has negative upgrade-price.");
        }

        Set<String> farmerTableSeeds = new HashSet<>();
        for (RecipeDef recipe : content.getRecipes().values()) {
            requirePositive(recipe.getResultAmount(), "Recipe '" + recipe.getId() + "' has invalid result-amount.");
            requireNotBlank(recipe.getResultId(), "Recipe '" + recipe.getId() + "' has empty result.");
            requireNotBlank(recipe.getStationId(), "Recipe '" + recipe.getId() + "' has empty station.");
            requireNotBlank(recipe.getCategoryId(), "Recipe '" + recipe.getId() + "' has empty category.");
            requireNonNegative(recipe.getCoinCost(), "Recipe '" + recipe.getId() + "' has negative coin-cost.");
            requirePositive(recipe.getMaxCraftsPerAction(), "Recipe '" + recipe.getId() + "' has invalid max-crafts-per-action.");
            if (recipe.getSuccessChance() < 0.0D || recipe.getSuccessChance() > 1.0D) {
                throw new ConfigValidationException("Recipe '" + recipe.getId() + "' has invalid success-chance.");
            }
            if (recipe.getIngredients().isEmpty()) {
                throw new ConfigValidationException("Recipe '" + recipe.getId() + "' has no ingredients.");
            }
            requireKnownItem(content, recipe.getResultId(), "Recipe '" + recipe.getId() + "' has unknown result.");
            if (recipe.getStationId().equals("farmer_table")) {
                PlantDef plant = isSeed(content, recipe.getResultId());
                if (plant == null) {
                    throw new ConfigValidationException("Farmer table recipe '" + recipe.getId() + "' must result in known plant seed. ID: " + recipe.getResultId());
                }
                String expectedCategory = ContentIds.plotRecipeCategory(plant.getPlotTypeId());
                if (!recipe.getCategoryId().equals(expectedCategory)) {
                    throw new ConfigValidationException("Farmer table recipe '" + recipe.getId() + "' must use category '" + expectedCategory + "'.");
                }
                if (!farmerTableSeeds.add(recipe.getResultId())) {
                    throw new ConfigValidationException("Duplicate farmer table recipe for seed: " + recipe.getResultId());
                }
            }
            for (Map.Entry<String, Integer> ingredient : recipe.getIngredients().entrySet()) {
                requirePositive(ingredient.getValue(), "Recipe '" + recipe.getId() + "' has invalid ingredient amount for '" + ingredient.getKey() + "'.");
                requireKnownItem(content, ingredient.getKey(), "Recipe '" + recipe.getId() + "' has unknown ingredient.");
            }
        }

        for (PlantDef plant : content.getPlants().values()) {
            if (!farmerTableSeeds.contains(plant.getSeedId())) {
                throw new ConfigValidationException("Plant '" + plant.getId()
                        + "' cannot be obtained: farmer table recipe for seed '" + plant.getSeedId() + "' is missing.");
            }
        }
    }

    private static void requireNotEmpty(Map<?, ?> map, String name) {
        if (map.isEmpty()) {
            throw new ConfigValidationException("Content section '" + name + "' must not be empty.");
        }
    }

    private static void requireKnown(Map<String, ?> map, String id, String message) {
        if (!map.containsKey(id)) {
            throw new ConfigValidationException(message + " ID: " + id);
        }
    }

    private static void requireKnownItem(GameContent content, String id, String message) {
        if (content.getFoods().containsKey(id)
                || content.getPlorts().containsKey(id)
                || content.getResources().containsKey(id)
                || isSeed(content, id) != null
        ) {
            return;
        }
        throw new ConfigValidationException(message + " ID: " + id);
    }

    private static PlantDef isSeed(GameContent content, String id) {
        for (PlantDef plant : content.getPlants().values()) {
            if (plant.getSeedId().equals(id)) {
                return plant;
            }
        }
        return null;
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigValidationException(message);
        }
    }

    private static void requirePositive(int value, String message) {
        if (value <= 0) {
            throw new ConfigValidationException(message);
        }
    }

    private static void requireNonNegative(int value, String message) {
        if (value < 0) {
            throw new ConfigValidationException(message);
        }
    }

    private static void requireNonNegative(long value, String message) {
        if (value < 0L) {
            throw new ConfigValidationException(message);
        }
    }
}
