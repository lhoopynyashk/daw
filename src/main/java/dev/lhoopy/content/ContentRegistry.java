package dev.lhoopy.content;

import dev.lhoopy.core.config.ConfigProvider;
import org.bukkit.Material;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ContentRegistry {
    private final ConfigProvider<GameContent> contentProvider;
    private final Map<String, SlimeDef> slimes = new LinkedHashMap<>();
    private final Map<String, LocationDef> locations = new LinkedHashMap<>();
    private final Map<String, FoodDef> foods = new LinkedHashMap<>();
    private final Map<Material, FoodDef> foodsByMaterial = new LinkedHashMap<>();
    private final Map<String, PlantDef> plants = new LinkedHashMap<>();
    private final Map<String, PlantDef> plantsBySeed = new LinkedHashMap<>();
    private final Map<String, PlortDef> plorts = new LinkedHashMap<>();
    private final Map<String, ResourceDef> resources = new LinkedHashMap<>();
    private final Map<String, PenDef> pens = new LinkedHashMap<>();
    private final Map<String, RecipeDef> recipes = new LinkedHashMap<>();

    public ContentRegistry(ConfigProvider<GameContent> contentProvider) {
        this.contentProvider = contentProvider;
    }

    public void load() {
        GameContent content = this.contentProvider.load();
        this.slimes.clear();
        this.locations.clear();
        this.foods.clear();
        this.foodsByMaterial.clear();
        this.plants.clear();
        this.plantsBySeed.clear();
        this.plorts.clear();
        this.resources.clear();
        this.pens.clear();
        this.recipes.clear();
        this.slimes.putAll(content.getSlimes());
        this.locations.putAll(content.getLocations());
        this.foods.putAll(content.getFoods());
        for (FoodDef food : content.getFoods().values()) {
            for (Material material : food.getMaterials()) {
                this.foodsByMaterial.put(material, food);
            }
        }
        this.plants.putAll(content.getPlants());
        for (PlantDef plant : this.plants.values()) {
            this.plantsBySeed.put(normalize(plant.getSeedId()), plant);
        }
        this.plorts.putAll(content.getPlorts());
        this.resources.putAll(content.getResources());
        this.pens.putAll(content.getPens());
        this.recipes.putAll(content.getRecipes());
    }

    public SlimeDef getSlime(String id) {
        if (id == null) {
            return null;
        }
        return this.slimes.get(normalize(id));
    }

    public SlimeDef getDefaultSlime() {
        if (this.slimes.isEmpty()) {
            return null;
        }
        return this.slimes.values().iterator().next();
    }

    public Collection<SlimeDef> slimes() {
        return Collections.unmodifiableCollection(this.slimes.values());
    }

    public LocationDef getLocation(String id) {
        if (id == null) {
            return null;
        }
        return this.locations.get(normalize(id));
    }

    public Collection<LocationDef> locations() {
        return Collections.unmodifiableCollection(this.locations.values());
    }

    public FoodDef getFood(String id) {
        if (id == null) {
            return null;
        }
        return this.foods.get(normalize(id));
    }

    public FoodDef getFoodByMaterial(Material material) {
        return material == null ? null : this.foodsByMaterial.get(material);
    }

    public Collection<FoodDef> foods() {
        return Collections.unmodifiableCollection(this.foods.values());
    }

    public PlantDef getPlant(String id) {
        if (id == null) {
            return null;
        }
        return this.plants.get(normalize(id));
    }

    public PlantDef getPlantBySeed(String seedId) {
        if (seedId == null) {
            return null;
        }
        return this.plantsBySeed.get(normalize(seedId));
    }

    public PlantDef getPlantOrSeed(String plantOrSeedId) {
        PlantDef plant = getPlant(plantOrSeedId);
        if (plant != null) {
            return plant;
        }
        return getPlantBySeed(plantOrSeedId);
    }

    public Collection<PlantDef> plants() {
        return Collections.unmodifiableCollection(this.plants.values());
    }

    public PlortDef getPlort(String id) {
        if (id == null) {
            return null;
        }
        return this.plorts.get(normalize(id));
    }

    public Collection<PlortDef> plorts() {
        return Collections.unmodifiableCollection(this.plorts.values());
    }

    public ResourceDef getResource(String id) {
        if (id == null) {
            return null;
        }
        return this.resources.get(normalize(id));
    }

    public boolean hasResource(String id) {
        return getResource(id) != null;
    }

    public Collection<ResourceDef> resources() {
        return Collections.unmodifiableCollection(this.resources.values());
    }

    public PenDef getPen(String id) {
        if (id == null) {
            return null;
        }
        return this.pens.get(normalize(id));
    }

    public Collection<PenDef> pens() {
        return Collections.unmodifiableCollection(this.pens.values());
    }

    public RecipeDef getRecipe(String id) {
        if (id == null) {
            return null;
        }
        return this.recipes.get(normalize(id));
    }

    public Collection<RecipeDef> recipes() {
        return Collections.unmodifiableCollection(this.recipes.values());
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
