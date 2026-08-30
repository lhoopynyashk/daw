package dev.lhoopy.crafting;

import dev.lhoopy.content.RecipeDef;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CraftingResult {
    private final boolean success;
    private final CraftingFailureReason failureReason;
    private final RecipeDef recipe;
    private final int requestedAmount;
    private final int craftedAmount;
    private final long missingCoins;
    private final Map<String, Integer> missingIngredients;
    private final List<String> missingUnlocks;
    private final List<String> missingFlags;

    private CraftingResult(
            boolean success,
            CraftingFailureReason failureReason,
            RecipeDef recipe,
            int requestedAmount,
            int craftedAmount,
            long missingCoins,
            Map<String, Integer> missingIngredients,
            List<String> missingUnlocks,
            List<String> missingFlags
    ) {
        this.success = success;
        this.failureReason = failureReason;
        this.recipe = recipe;
        this.requestedAmount = Math.max(0, requestedAmount);
        this.craftedAmount = Math.max(0, craftedAmount);
        this.missingCoins = Math.max(0L, missingCoins);
        this.missingIngredients = Collections.unmodifiableMap(new LinkedHashMap<>(missingIngredients));
        this.missingUnlocks = Collections.unmodifiableList(new ArrayList<>(missingUnlocks));
        this.missingFlags = Collections.unmodifiableList(new ArrayList<>(missingFlags));
    }

    public static CraftingResult success(RecipeDef recipe) {
        return success(recipe, 1, 1);
    }

    public static CraftingResult success(RecipeDef recipe, int requestedAmount, int craftedAmount) {
        return new CraftingResult(true, CraftingFailureReason.NONE, recipe, requestedAmount, craftedAmount, 0L,
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
    }

    public static CraftingResult fail(CraftingFailureReason failureReason, RecipeDef recipe) {
        return fail(failureReason, recipe, 1);
    }

    public static CraftingResult fail(CraftingFailureReason failureReason, RecipeDef recipe, int requestedAmount) {
        return new CraftingResult(false, failureReason, recipe, requestedAmount, 0, 0L,
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
    }

    public static CraftingResult missing(RecipeDef recipe, Map<String, Integer> missingIngredients) {
        return missing(recipe, 1, missingIngredients);
    }

    public static CraftingResult missing(RecipeDef recipe, int requestedAmount, Map<String, Integer> missingIngredients) {
        return new CraftingResult(false, CraftingFailureReason.MISSING_INGREDIENTS, recipe, requestedAmount, 0, 0L,
                missingIngredients, Collections.emptyList(), Collections.emptyList());
    }

    public static CraftingResult missingCoins(RecipeDef recipe, int requestedAmount, long missingCoins) {
        return new CraftingResult(false, CraftingFailureReason.MISSING_COINS, recipe, requestedAmount, 0, missingCoins,
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
    }

    public static CraftingResult locked(RecipeDef recipe, int requestedAmount, List<String> missingUnlocks, List<String> missingFlags) {
        return new CraftingResult(false, CraftingFailureReason.LOCKED, recipe, requestedAmount, 0, 0L,
                Collections.emptyMap(), missingUnlocks, missingFlags);
    }

    public boolean isSuccess() {
        return this.success;
    }

    public CraftingFailureReason getFailureReason() {
        return this.failureReason;
    }

    public RecipeDef getRecipe() {
        return this.recipe;
    }

    public int getRequestedAmount() {
        return this.requestedAmount;
    }

    public int getCraftedAmount() {
        return this.craftedAmount;
    }

    public long getMissingCoins() {
        return this.missingCoins;
    }

    public Map<String, Integer> getMissingIngredients() {
        return this.missingIngredients;
    }

    public List<String> getMissingUnlocks() {
        return this.missingUnlocks;
    }

    public List<String> getMissingFlags() {
        return this.missingFlags;
    }
}
