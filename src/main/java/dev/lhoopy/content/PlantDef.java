package dev.lhoopy.content;

public final class PlantDef {
    private final String id;
    private final String displayName;
    private final String seedId;
    private final String plotTypeId;
    private final String outputFoodId;
    private final int growthSeconds;
    private final int seedPrice;
    private final int harvestAmount;

    public PlantDef(String id, String displayName, String seedId, String plotTypeId, String outputFoodId, int growthSeconds, int seedPrice, int harvestAmount) {
        this.id = id;
        this.displayName = displayName;
        this.seedId = seedId;
        this.plotTypeId = plotTypeId;
        this.outputFoodId = outputFoodId;
        this.growthSeconds = growthSeconds;
        this.seedPrice = seedPrice;
        this.harvestAmount = harvestAmount;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getSeedId() {
        return this.seedId;
    }

    public String getPlotTypeId() {
        return this.plotTypeId;
    }

    public String getOutputFoodId() {
        return this.outputFoodId;
    }

    public int getGrowthSeconds() {
        return this.growthSeconds;
    }

    public int getSeedPrice() {
        return this.seedPrice;
    }

    public int getHarvestAmount() {
        return this.harvestAmount;
    }
}
