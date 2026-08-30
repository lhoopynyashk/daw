package dev.lhoopy.crafting;

public final class PlotSeedCategory {
    private final String id;
    private final String plotTypeId;
    private final String title;

    public PlotSeedCategory(String id, String plotTypeId, String title) {
        this.id = id;
        this.plotTypeId = plotTypeId;
        this.title = title;
    }

    public String getId() {
        return this.id;
    }

    public String getPlotTypeId() {
        return this.plotTypeId;
    }

    public String getTitle() {
        return this.title;
    }
}
