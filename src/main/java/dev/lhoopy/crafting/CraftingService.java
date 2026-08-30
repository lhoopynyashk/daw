package dev.lhoopy.crafting;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.RecipeDef;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.storage.PlayerStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class CraftingService {
    private final ContentRegistry contentRegistry;

    public CraftingService(ContentRegistry contentRegistry) {
        this.contentRegistry = contentRegistry;
    }

    public boolean canCraft(PlayerProfile profile, String recipeId) {
        RecipeDef recipe = this.contentRegistry.getRecipe(recipeId);
        return recipe != null && validate(profile, recipe, 1).isSuccess();
    }

    public boolean craft(PlayerProfile profile, String recipeId) {
        return craft(profile, recipeId, null).isSuccess();
    }

    public CraftingResult craft(PlayerProfile profile, String recipeId, String stationId) {
        return craft(profile, recipeId, stationId, 1);
    }

    public CraftingResult craft(PlayerProfile profile, String recipeId, String stationId, int amount) {
        RecipeDef recipe = this.contentRegistry.getRecipe(recipeId);
        if (recipe == null) {
            return CraftingResult.fail(CraftingFailureReason.UNKNOWN_RECIPE, null, amount);
        }
        if (stationId != null && !recipe.getStationId().equalsIgnoreCase(stationId)) {
            return CraftingResult.fail(CraftingFailureReason.WRONG_STATION, recipe, amount);
        }

        CraftingResult validation = validate(profile, recipe, amount);
        if (!validation.isSuccess()) {
            return validation;
        }

        int craftedAmount = rollCraftedAmount(recipe, amount);
        long totalCoinCost = multiply(recipe.getCoinCost(), amount);
        for (Map.Entry<String, Integer> ingredient : recipe.getIngredients().entrySet()) {
            profile.getStorage().remove(ingredient.getKey(), multiply(ingredient.getValue(), amount));
        }
        if (totalCoinCost > 0L) {
            profile.setCoins(profile.getCoins() - totalCoinCost);
        }
        if (craftedAmount > 0) {
            profile.getStorage().add(recipe.getResultId(), multiply(recipe.getResultAmount(), craftedAmount));
            return CraftingResult.success(recipe, amount, craftedAmount);
        }
        return CraftingResult.fail(CraftingFailureReason.CHANCE_FAILED, recipe, amount);
    }

    public CraftingResult validate(PlayerProfile profile, String recipeId, String stationId, int amount) {
        RecipeDef recipe = this.contentRegistry.getRecipe(recipeId);
        if (recipe == null) {
            return CraftingResult.fail(CraftingFailureReason.UNKNOWN_RECIPE, null, amount);
        }
        if (stationId != null && !recipe.getStationId().equalsIgnoreCase(stationId)) {
            return CraftingResult.fail(CraftingFailureReason.WRONG_STATION, recipe, amount);
        }
        return validate(profile, recipe, amount);
    }

    private static CraftingResult validate(PlayerProfile profile, RecipeDef recipe, int amount) {
        if (amount <= 0) {
            return CraftingResult.fail(CraftingFailureReason.INVALID_AMOUNT, recipe, amount);
        }
        if (amount > recipe.getMaxCraftsPerAction()) {
            return CraftingResult.fail(CraftingFailureReason.AMOUNT_LIMIT_EXCEEDED, recipe, amount);
        }

        List<String> missingUnlocks = getMissingUnlocks(profile, recipe);
        List<String> missingFlags = getMissingFlags(profile, recipe);
        if (!missingUnlocks.isEmpty() || !missingFlags.isEmpty()) {
            return CraftingResult.locked(recipe, amount, missingUnlocks, missingFlags);
        }

        long totalCoinCost = multiply(recipe.getCoinCost(), amount);
        if (profile.getCoins() < totalCoinCost) {
            return CraftingResult.missingCoins(recipe, amount, totalCoinCost - profile.getCoins());
        }

        Map<String, Integer> missingIngredients = getMissingIngredients(profile, recipe, amount);
        if (!missingIngredients.isEmpty()) {
            return CraftingResult.missing(recipe, amount, missingIngredients);
        }

        int currentResultAmount = profile.getStorage().getAmount(recipe.getResultId());
        int resultAmount = multiply(recipe.getResultAmount(), amount);
        if (resultAmount > PlayerStorage.MAX_AMOUNT_PER_ITEM - currentResultAmount) {
            return CraftingResult.fail(CraftingFailureReason.STORAGE_FULL, recipe, amount);
        }

        return CraftingResult.success(recipe, amount, 0);
    }

    private static Map<String, Integer> getMissingIngredients(PlayerProfile profile, RecipeDef recipe, int amount) {
        Map<String, Integer> missing = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> ingredient : recipe.getIngredients().entrySet()) {
            int available = profile.getStorage().getAmount(ingredient.getKey());
            int required = multiply(ingredient.getValue(), amount);
            if (available < required) {
                missing.put(ingredient.getKey(), required - available);
            }
        }
        return missing;
    }

    private static List<String> getMissingUnlocks(PlayerProfile profile, RecipeDef recipe) {
        List<String> missing = new ArrayList<>();
        for (String unlockId : recipe.getUnlockRequirements()) {
            if (!profile.getProgressData().isUnlocked(unlockId)) {
                missing.add(unlockId);
            }
        }
        return missing;
    }

    private static List<String> getMissingFlags(PlayerProfile profile, RecipeDef recipe) {
        List<String> missing = new ArrayList<>();
        for (String flag : recipe.getFlagRequirements()) {
            if (!profile.getProgressData().hasFlag(flag)) {
                missing.add(flag);
            }
        }
        return missing;
    }

    private static int rollCraftedAmount(RecipeDef recipe, int amount) {
        double chance = recipe.getSuccessChance();
        if (chance >= 1.0D) {
            return amount;
        }
        if (chance <= 0.0D) {
            return 0;
        }
        int crafted = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < amount; i++) {
            if (random.nextDouble() <= chance) {
                crafted++;
            }
        }
        return crafted;
    }

    private static int multiply(int value, int amount) {
        long result = (long) value * Math.max(0, amount);
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private static long multiply(long value, int amount) {
        if (value <= 0L || amount <= 0) {
            return 0L;
        }
        long result = value * (long) amount;
        return result < 0L ? Long.MAX_VALUE : result;
    }
}
