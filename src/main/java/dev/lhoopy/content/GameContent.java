package dev.lhoopy.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameContent {
    private final Map<String, SlimeDef> slimes;
    private final Map<String, LocationDef> locations;
    private final Map<String, FoodDef> foods;
    private final Map<String, PlantDef> plants;
    private final Map<String, PlortDef> plorts;
    private final Map<String, ResourceDef> resources;
    private final Map<String, PenDef> pens;
    private final Map<String, RecipeDef> recipes;

    public GameContent(
            Map<String, SlimeDef> slimes,
            Map<String, LocationDef> locations,
            Map<String, FoodDef> foods,
            Map<String, PlantDef> plants,
            Map<String, PlortDef> plorts,
            Map<String, ResourceDef> resources,
            Map<String, PenDef> pens,
            Map<String, RecipeDef> recipes
    ) {
        this.slimes = immutableCopy(slimes);
        this.locations = immutableCopy(locations);
        this.foods = immutableCopy(foods);
        this.plants = immutableCopy(plants);
        this.plorts = immutableCopy(plorts);
        this.resources = immutableCopy(resources);
        this.pens = immutableCopy(pens);
        this.recipes = immutableCopy(recipes);
    }

    public Map<String, SlimeDef> getSlimes() {
        return this.slimes;
    }

    public Map<String, LocationDef> getLocations() {
        return this.locations;
    }

    public Map<String, FoodDef> getFoods() {
        return this.foods;
    }

    public Map<String, PlantDef> getPlants() {
        return this.plants;
    }

    public Map<String, PlortDef> getPlorts() {
        return this.plorts;
    }

    public Map<String, ResourceDef> getResources() {
        return this.resources;
    }

    public Map<String, PenDef> getPens() {
        return this.pens;
    }

    public Map<String, RecipeDef> getRecipes() {
        return this.recipes;
    }

    private static <T> Map<String, T> immutableCopy(Map<String, T> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
