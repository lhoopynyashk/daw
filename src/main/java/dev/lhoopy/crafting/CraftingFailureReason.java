package dev.lhoopy.crafting;

public enum CraftingFailureReason {
    NONE,
    UNKNOWN_RECIPE,
    WRONG_STATION,
    INVALID_AMOUNT,
    AMOUNT_LIMIT_EXCEEDED,
    MISSING_INGREDIENTS,
    MISSING_COINS,
    LOCKED,
    CHANCE_FAILED,
    STORAGE_FULL
}
