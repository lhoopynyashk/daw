package dev.lhoopy.crafting;

import dev.lhoopy.content.PlantDef;
import dev.lhoopy.content.RecipeDef;

public final class SeedRecipeEntry {
    private final PlotSeedCategory category;
    private final RecipeDef recipe;
    private final PlantDef plant;
    private final CraftingResult craftState;
    private final int seedsInStorage;
    private final int foodInStorage;

    public SeedRecipeEntry(
            PlotSeedCategory category,
            RecipeDef recipe,
            PlantDef plant,
            CraftingResult craftState,
            int seedsInStorage,
            int foodInStorage
    ) {
        this.category = category;
        this.recipe = recipe;
        this.plant = plant;
        this.craftState = craftState;
        this.seedsInStorage = Math.max(0, seedsInStorage);
        this.foodInStorage = Math.max(0, foodInStorage);
    }

    public PlotSeedCategory getCategory() {
        return this.category;
    }

    public RecipeDef getRecipe() {
        return this.recipe;
    }

    public PlantDef getPlant() {
        return this.plant;
    }

    public CraftingResult getCraftState() {
        return this.craftState;
    }

    public int getSeedsInStorage() {
        return this.seedsInStorage;
    }

    public int getFoodInStorage() {
        return this.foodInStorage;
    }

    public boolean isCraftable() {
        return this.craftState.isSuccess();
    }
}
