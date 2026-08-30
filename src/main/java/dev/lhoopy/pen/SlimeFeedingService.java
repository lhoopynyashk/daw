package dev.lhoopy.pen;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.ContentIds;
import dev.lhoopy.content.SlimeDef;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.storage.VacpackLimits;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public final class SlimeFeedingService {
    private final ContentRegistry contentRegistry;
    private final long fedDurationMillis;

    public SlimeFeedingService(ContentRegistry contentRegistry, long fedDurationMillis) {
        this.contentRegistry = contentRegistry;
        this.fedDurationMillis = Math.max(1L, fedDurationMillis);
    }

    public FeedResult feed(PlayerProfile profile, PenSlime penSlime, long now) {
        SlimeDef slime = this.contentRegistry.getSlime(penSlime.getSlimeId());
        if (slime == null) {
            return FeedResult.unknownSlime(penSlime.getSlimeId());
        }

        String foodId = foodIdFor(slime.getFavoriteFood());
        if (foodId == null) {
            return FeedResult.noFoodMapping(slime.getFavoriteFood().name());
        }
        String plortId = ContentIds.resolvePlortForSlime(this.contentRegistry, slime.getId());
        if (VacpackLimits.used(profile, plortId) >= VacpackLimits.capacity(profile, plortId)) {
            return FeedResult.vacpackFull();
        }
        if (!profile.getStorage().remove(foodId, 1)) {
            return FeedResult.missingFood(foodId);
        }

        penSlime.feed(now, this.fedDurationMillis);
        if (VacpackLimits.add(profile, plortId, 1) <= 0) {
            profile.getStorage().add(foodId, 1);
            return FeedResult.vacpackFull();
        }
        return FeedResult.success(slime.getId(), foodId);
    }

    private static String foodIdFor(Material material) {
        return FoodMappings.BY_MATERIAL.get(material);
    }

    public static final class FeedResult {
        private final boolean success;
        private final String slimeId;
        private final String foodId;
        private final String message;

        private FeedResult(boolean success, String slimeId, String foodId, String message) {
            this.success = success;
            this.slimeId = slimeId;
            this.foodId = foodId;
            this.message = message;
        }

        public static FeedResult success(String slimeId, String foodId) {
            return new FeedResult(true, slimeId, foodId, null);
        }

        public static FeedResult unknownSlime(String slimeId) {
            return new FeedResult(false, slimeId, null, "Unknown slime: " + slimeId);
        }

        public static FeedResult noFoodMapping(String material) {
            return new FeedResult(false, null, null, "No food mapping for material: " + material);
        }

        public static FeedResult missingFood(String foodId) {
            return new FeedResult(false, null, foodId, "Need food in storage: " + foodId);
        }

        public static FeedResult vacpackFull() {
            return new FeedResult(false, null, null, "Vacpack is full.");
        }

        public boolean isSuccess() {
            return this.success;
        }

        public String getSlimeId() {
            return this.slimeId;
        }

        public String getFoodId() {
            return this.foodId;
        }

        public String getMessage() {
            return this.message;
        }
    }

    private static final class FoodMappings {
        private static final Map<Material, String> BY_MATERIAL = new HashMap<>();

        static {
            BY_MATERIAL.put(Material.APPLE, "plant_sweetroot");
            BY_MATERIAL.put(Material.CARROT_ITEM, "plant_meadow_carrot");
            BY_MATERIAL.put(Material.WHEAT, "plant_sweetroot");
            BY_MATERIAL.put(Material.RAW_FISH, "plant_blue_algae");
            BY_MATERIAL.put(Material.CACTUS, "plant_spicy_pumpkin");
            BY_MATERIAL.put(Material.SUGAR, "plant_honeyflower");
            BY_MATERIAL.put(Material.BROWN_MUSHROOM, "plant_bright_mushroom");
            BY_MATERIAL.put(Material.BREAD, "plant_sweetroot");
            BY_MATERIAL.put(Material.POTATO_ITEM, "plant_meadow_carrot");
            BY_MATERIAL.put(Material.COOKIE, "plant_night_cap");
        }
    }
}
