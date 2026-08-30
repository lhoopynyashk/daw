package dev.lhoopy.crafting;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.PlantDef;
import dev.lhoopy.content.RecipeDef;
import dev.lhoopy.profile.PlayerProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FarmerTableService {
    private static final String STATION_ID = "farmer_table";

    private final ContentRegistry contentRegistry;
    private final CraftingService craftingService;
    private final Map<String, PlotSeedCategory> categories = new LinkedHashMap<>();
    private final Map<String, PlotSeedCategory> categoriesByPlotType = new LinkedHashMap<>();

    public FarmerTableService(ContentRegistry contentRegistry, CraftingService craftingService) {
        this.contentRegistry = contentRegistry;
        this.craftingService = craftingService;
        registerCategory("plot_basic", "basic", "Обычная");
        registerCategory("plot_wet", "wet", "Влажная");
        registerCategory("plot_mycelium", "mycelium", "Грибница");
        registerCategory("plot_hot", "hot", "Горячая");
        registerCategory("plot_crystal", "crystal", "Кристальная");
        registerCategory("plot_sky", "sky", "Небесная");
    }

    public boolean craft(PlayerProfile profile, String recipeId) {
        return craftDetailed(profile, recipeId).isSuccess();
    }

    public CraftingResult craftDetailed(PlayerProfile profile, String recipeId) {
        return this.craftingService.craft(profile, recipeId, STATION_ID);
    }

    public CraftingResult craftDetailed(PlayerProfile profile, String recipeId, int amount) {
        return this.craftingService.craft(profile, recipeId, STATION_ID, amount);
    }

    public CraftingResult validate(PlayerProfile profile, String recipeId, int amount) {
        return this.craftingService.validate(profile, recipeId, STATION_ID, amount);
    }

    public List<PlotSeedCategory> getCategories() {
        return Collections.unmodifiableList(new ArrayList<>(this.categories.values()));
    }

    public PlotSeedCategory getCategory(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        return this.categories.get(normalize(categoryId));
    }

    public PlotSeedCategory getCategoryByPlotType(String plotTypeId) {
        if (plotTypeId == null) {
            return null;
        }
        return this.categoriesByPlotType.get(normalize(plotTypeId));
    }

    public List<RecipeDef> getSeedRecipes() {
        List<RecipeDef> result = new ArrayList<>();
        for (RecipeDef recipe : this.contentRegistry.recipes()) {
            if (isSeedRecipe(recipe)) {
                result.add(recipe);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<RecipeDef> getSeedRecipesForCategory(String categoryId) {
        PlotSeedCategory category = getCategory(categoryId);
        if (category == null) {
            return Collections.emptyList();
        }
        return getSeedRecipesForPlotType(category.getPlotTypeId());
    }

    public List<RecipeDef> getSeedRecipesForPlotType(String plotTypeId) {
        String normalizedPlotType = normalize(plotTypeId);
        List<RecipeDef> result = new ArrayList<>();
        for (RecipeDef recipe : getSeedRecipes()) {
            PlantDef plant = this.contentRegistry.getPlantBySeed(recipe.getResultId());
            if (plant != null && plant.getPlotTypeId().equals(normalizedPlotType)) {
                result.add(recipe);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<SeedRecipeEntry> listSeedRecipes(PlayerProfile profile) {
        List<SeedRecipeEntry> result = new ArrayList<>();
        for (RecipeDef recipe : getSeedRecipes()) {
            SeedRecipeEntry entry = createEntry(profile, recipe);
            if (entry != null) {
                result.add(entry);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<SeedRecipeEntry> listSeedRecipes(PlayerProfile profile, String categoryId) {
        List<SeedRecipeEntry> result = new ArrayList<>();
        for (RecipeDef recipe : getSeedRecipesForCategory(categoryId)) {
            SeedRecipeEntry entry = createEntry(profile, recipe);
            if (entry != null) {
                result.add(entry);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public String getStationId() {
        return STATION_ID;
    }

    private SeedRecipeEntry createEntry(PlayerProfile profile, RecipeDef recipe) {
        PlantDef plant = this.contentRegistry.getPlantBySeed(recipe.getResultId());
        if (plant == null) {
            return null;
        }
        PlotSeedCategory category = getCategoryByPlotType(plant.getPlotTypeId());
        if (category == null) {
            return null;
        }
        CraftingResult craftState = this.craftingService.validate(profile, recipe.getId(), STATION_ID, 1);
        return new SeedRecipeEntry(
                category,
                recipe,
                plant,
                craftState,
                profile.getStorage().getAmount(plant.getSeedId()),
                profile.getStorage().getAmount(plant.getOutputFoodId())
        );
    }

    private boolean isSeedRecipe(RecipeDef recipe) {
        return recipe.getStationId().equals(STATION_ID)
                && this.contentRegistry.getPlantBySeed(recipe.getResultId()) != null;
    }

    private void registerCategory(String id, String plotTypeId, String title) {
        PlotSeedCategory category = new PlotSeedCategory(id, plotTypeId, title);
        this.categories.put(category.getId(), category);
        this.categoriesByPlotType.put(category.getPlotTypeId(), category);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
