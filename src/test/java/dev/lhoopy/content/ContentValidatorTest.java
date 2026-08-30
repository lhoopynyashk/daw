package dev.lhoopy.content;

import dev.lhoopy.core.config.ConfigValidationException;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ContentValidatorTest {
    private final ContentValidator validator = new ContentValidator();

    @Test
    void acceptsFarmerTableRecipeForMatchingSeedAndPlotType() {
        assertDoesNotThrow(() -> this.validator.validate(content(builder -> {
        })));
    }

    @Test
    void rejectsFarmerTableRecipeWithUnknownSeed() {
        assertThrows(ConfigValidationException.class, () -> this.validator.validate(content(builder ->
                builder.recipes.put("recipe_seed_ghost", recipe("recipe_seed_ghost", "seed_ghost", "plot_basic", "food_meadow_carrot"))
        )));
    }

    @Test
    void rejectsFarmerTableRecipeInWrongPlotCategory() {
        assertThrows(ConfigValidationException.class, () -> this.validator.validate(content(builder ->
                builder.recipes.put("recipe_seed_wrong_plot", recipe("recipe_seed_wrong_plot", "seed_meadow_carrot", "plot_hot", "food_meadow_carrot"))
        )));
    }

    @Test
    void rejectsDuplicatePlantSeedIds() {
        assertThrows(ConfigValidationException.class, () -> this.validator.validate(content(builder ->
                builder.plants.put("plant_second_carrot", new PlantDef(
                        "plant_second_carrot",
                        "Second carrot",
                        "seed_meadow_carrot",
                        "basic",
                        "food_meadow_carrot",
                        60,
                        5,
                        1
                ))
        )));
    }

    @Test
    void rejectsPlantWithoutFarmerTableRecipe() {
        assertThrows(ConfigValidationException.class, () -> this.validator.validate(content(builder ->
                builder.plants.put("plant_cloud_berry", new PlantDef(
                        "plant_cloud_berry",
                        "Cloud Berry",
                        "seed_cloud_berry",
                        "sky",
                        "food_meadow_carrot",
                        90,
                        8,
                        2
                ))
        )));
    }

    @Test
    void rejectsUnsupportedPlotType() {
        assertThrows(ConfigValidationException.class, () -> this.validator.validate(content(builder -> {
            builder.plants.clear();
            builder.plants.put("plant_meadow_carrot", new PlantDef(
                    "plant_meadow_carrot", "Meadow Carrot", "seed_meadow_carrot", "void",
                    "food_meadow_carrot", 60, 5, 2
            ));
        })));
    }

    @Test
    void rejectsLocationWithUnknownSlime() {
        assertThrows(ConfigValidationException.class, () -> this.validator.validate(content(builder ->
                builder.locations.put("loc_meadow", new LocationDef(
                        "loc_meadow",
                        "Meadow",
                        1,
                        List.of("slime_missing"),
                        Collections.emptyList(),
                        List.of("resource_pebble"),
                        Collections.emptyList(),
                        ""
                ))
        )));
    }

    @Test
    void rejectsRecipeWithUnknownIngredient() {
        assertThrows(ConfigValidationException.class, () -> this.validator.validate(content(builder ->
                builder.recipes.put("recipe_bad_ingredient", recipe("recipe_bad_ingredient", "seed_meadow_carrot", "plot_basic", "resource_missing"))
        )));
    }

    private static GameContent content(ContentMutation mutation) {
        ContentBuilder builder = new ContentBuilder();
        mutation.apply(builder);
        return builder.build();
    }

    private static RecipeDef recipe(String id, String resultId, String categoryId, String ingredientId) {
        return new RecipeDef(
                id,
                "farmer_table",
                categoryId,
                resultId,
                1,
                Map.of(ingredientId, 1),
                Collections.emptyList(),
                Collections.emptyList(),
                0L,
                1.0D,
                16
        );
    }

    private interface ContentMutation {
        void apply(ContentBuilder builder);
    }

    private static final class ContentBuilder {
        private final Map<String, SlimeDef> slimes = new LinkedHashMap<>();
        private final Map<String, LocationDef> locations = new LinkedHashMap<>();
        private final Map<String, FoodDef> foods = new LinkedHashMap<>();
        private final Map<String, PlantDef> plants = new LinkedHashMap<>();
        private final Map<String, PlortDef> plorts = new LinkedHashMap<>();
        private final Map<String, ResourceDef> resources = new LinkedHashMap<>();
        private final Map<String, PenDef> pens = new LinkedHashMap<>();
        private final Map<String, RecipeDef> recipes = new LinkedHashMap<>();

        private ContentBuilder() {
            this.slimes.put("slime_pink", new SlimeDef(
                    "slime_pink",
                    "Pink Slime",
                    Material.SLIME_BALL,
                    Material.CARROT_ITEM,
                    "common",
                    1,
                    10,
                    15,
                    2
            ));
            this.foods.put("food_meadow_carrot", new FoodDef("food_meadow_carrot", "Meadow Carrot", "vegetable"));
            this.plants.put("plant_meadow_carrot", new PlantDef(
                    "plant_meadow_carrot",
                    "Meadow Carrot",
                    "seed_meadow_carrot",
                    "basic",
                    "food_meadow_carrot",
                    60,
                    5,
                    2
            ));
            this.plorts.put("plort_pink", new PlortDef("plort_pink", "Pink Plort", 7));
            this.locations.put("loc_meadow", new LocationDef(
                    "loc_meadow",
                    "Meadow",
                    1,
                    List.of("slime_pink"),
                    Collections.emptyList(),
                    List.of("resource_pebble"),
                    Collections.emptyList(),
                    ""
            ));
            this.resources.put("resource_pebble", new ResourceDef(
                    "resource_pebble",
                    "Pebble",
                    "loc_meadow",
                    "common",
                    "surface",
                    "crafting"
            ));
            this.pens.put("pen_basic", new PenDef("pen_basic", "Basic Pen", 6, 100));
            this.recipes.put("recipe_seed_meadow_carrot", recipe(
                    "recipe_seed_meadow_carrot",
                    "seed_meadow_carrot",
                    "plot_basic",
                    "food_meadow_carrot"
            ));
        }

        private GameContent build() {
            return new GameContent(this.slimes, this.locations, this.foods, this.plants, this.plorts, this.resources, this.pens, this.recipes);
        }
    }
}
