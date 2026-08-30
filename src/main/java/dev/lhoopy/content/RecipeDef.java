package dev.lhoopy.content;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeDef {
    private final String id;
    private final String stationId;
    private final String categoryId;
    private final String resultId;
    private final int resultAmount;
    private final Map<String, Integer> ingredients;
    private final List<String> unlockRequirements;
    private final List<String> flagRequirements;
    private final long coinCost;
    private final double successChance;
    private final int maxCraftsPerAction;

    public RecipeDef(
            String id,
            String stationId,
            String categoryId,
            String resultId,
            int resultAmount,
            Map<String, Integer> ingredients,
            List<String> unlockRequirements,
            List<String> flagRequirements,
            long coinCost,
            double successChance,
            int maxCraftsPerAction
    ) {
        this.id = id;
        this.stationId = stationId;
        this.categoryId = categoryId;
        this.resultId = resultId;
        this.resultAmount = resultAmount;
        this.ingredients = Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
        this.unlockRequirements = Collections.unmodifiableList(new ArrayList<>(unlockRequirements));
        this.flagRequirements = Collections.unmodifiableList(new ArrayList<>(flagRequirements));
        this.coinCost = coinCost;
        this.successChance = successChance;
        this.maxCraftsPerAction = maxCraftsPerAction;
    }

    public String getId() {
        return this.id;
    }

    public String getStationId() {
        return this.stationId;
    }

    public String getCategoryId() {
        return this.categoryId;
    }

    public String getResultId() {
        return this.resultId;
    }

    public int getResultAmount() {
        return this.resultAmount;
    }

    public Map<String, Integer> getIngredients() {
        return this.ingredients;
    }

    public List<String> getUnlockRequirements() {
        return this.unlockRequirements;
    }

    public List<String> getFlagRequirements() {
        return this.flagRequirements;
    }

    public long getCoinCost() {
        return this.coinCost;
    }

    public double getSuccessChance() {
        return this.successChance;
    }

    public int getMaxCraftsPerAction() {
        return this.maxCraftsPerAction;
    }
}
