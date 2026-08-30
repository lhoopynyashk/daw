package dev.lhoopy.pen;

public final class PenStyleDef {
    private final String id;
    private final String displayName;
    private final String rarity;
    private final String description;
    private final double chance;
    private final double productionMultiplier;
    private final double sellMultiplier;
    private final double extraPlortChance;
    private final double foodUseMultiplier;
    private final int capacityBonus;
    private final boolean automaticCollection;

    public PenStyleDef(String id, String displayName, String rarity, String description, double chance,
                       double productionMultiplier, double sellMultiplier, double extraPlortChance,
                       double foodUseMultiplier, int capacityBonus, boolean automaticCollection) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.description = description;
        this.chance = chance;
        this.productionMultiplier = productionMultiplier;
        this.sellMultiplier = sellMultiplier;
        this.extraPlortChance = extraPlortChance;
        this.foodUseMultiplier = foodUseMultiplier;
        this.capacityBonus = capacityBonus;
        this.automaticCollection = automaticCollection;
    }

    public String getId() { return this.id; }
    public String getDisplayName() { return this.displayName; }
    public String getRarity() { return this.rarity; }
    public String getDescription() { return this.description; }
    public double getChance() { return this.chance; }
    public double getProductionMultiplier() { return this.productionMultiplier; }
    public double getSellMultiplier() { return this.sellMultiplier; }
    public double getExtraPlortChance() { return this.extraPlortChance; }
    public double getFoodUseMultiplier() { return this.foodUseMultiplier; }
    public int getCapacityBonus() { return this.capacityBonus; }
    public boolean hasAutomaticCollection() { return this.automaticCollection; }
}
