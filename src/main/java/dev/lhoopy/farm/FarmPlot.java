package dev.lhoopy.farm;

public final class FarmPlot {
    private final String id;
    private String plotTypeId = "basic";
    private String plantId;
    private long plantedAtMillis;
    private long wateredUntilMillis;
    private long growthProgressMillis;
    private long lastGrowthUpdateMillis;

    public FarmPlot(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public String getPlotTypeId() {
        return this.plotTypeId;
    }

    public void setPlotTypeId(String plotTypeId) {
        if (plotTypeId == null || plotTypeId.trim().isEmpty()) {
            this.plotTypeId = "basic";
            return;
        }
        this.plotTypeId = plotTypeId.toLowerCase(java.util.Locale.ROOT).replace('-', '_');
    }

    public String getPlantId() {
        return this.plantId;
    }

    public void setPlantId(String plantId) {
        this.plantId = plantId;
    }

    public long getPlantedAtMillis() {
        return this.plantedAtMillis;
    }

    public void setPlantedAtMillis(long plantedAtMillis) {
        this.plantedAtMillis = Math.max(0L, plantedAtMillis);
    }

    public long getWateredUntilMillis() {
        return this.wateredUntilMillis;
    }

    public void setWateredUntilMillis(long wateredUntilMillis) {
        this.wateredUntilMillis = Math.max(0L, wateredUntilMillis);
    }

    public long getGrowthProgressMillis() {
        return this.growthProgressMillis;
    }

    public void setGrowthProgressMillis(long growthProgressMillis) {
        this.growthProgressMillis = Math.max(0L, growthProgressMillis);
    }

    public long getLastGrowthUpdateMillis() {
        return this.lastGrowthUpdateMillis;
    }

    public void setLastGrowthUpdateMillis(long lastGrowthUpdateMillis) {
        this.lastGrowthUpdateMillis = Math.max(0L, lastGrowthUpdateMillis);
    }

    public boolean isEmpty() {
        return this.plantId == null || this.plantId.trim().isEmpty();
    }

    public void plant(String plantId, long nowMillis) {
        this.plantId = plantId;
        this.plantedAtMillis = nowMillis;
        this.wateredUntilMillis = 0L;
        this.growthProgressMillis = 0L;
        this.lastGrowthUpdateMillis = nowMillis;
    }

    public void clear() {
        this.plantId = null;
        this.plantedAtMillis = 0L;
        this.wateredUntilMillis = 0L;
        this.growthProgressMillis = 0L;
        this.lastGrowthUpdateMillis = 0L;
    }
}
