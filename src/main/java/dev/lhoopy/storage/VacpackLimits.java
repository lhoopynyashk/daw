package dev.lhoopy.storage;

import dev.lhoopy.profile.PlayerProfile;

import java.util.Locale;

public final class VacpackLimits {
    private VacpackLimits() {
    }

    public static int capacity(PlayerProfile profile, String itemId) {
        String category = categoryOf(itemId);
        if (category.equals("plorts")) {
            return profile.getVacpackPlortCapacity();
        }
        if (category.equals("food")) {
            return profile.getVacpackFoodCapacity();
        }
        if (category.equals("seeds")) {
            return profile.getVacpackSeedCapacity();
        }
        if (category.equals("resources")) {
            return profile.getVacpackResourceCapacity();
        }
        return profile.getVacpackOtherCapacity();
    }

    public static int used(PlayerProfile profile, String itemId) {
        return used(profile.getVacpackStorage(), categoryOf(itemId));
    }

    public static int used(PlayerStorage storage, String category) {
        int total = 0;
        for (StoredItem item : storage.getItems()) {
            if (categoryOf(item.getItemId()).equals(category)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static int add(PlayerProfile profile, String itemId, int amount) {
        return profile.getVacpackStorage().addLimited(itemId, amount, used(profile, itemId), capacity(profile, itemId));
    }

    public static String categoryOf(String itemId) {
        String normalized = itemId == null ? "" : itemId.toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.startsWith("plort_")) {
            return "plorts";
        }
        if (normalized.startsWith("food_") || normalized.startsWith("plant_")) {
            return "food";
        }
        if (normalized.startsWith("seed_")) {
            return "seeds";
        }
        if (normalized.startsWith("res_")) {
            return "resources";
        }
        return "other";
    }
}
