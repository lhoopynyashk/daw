package dev.lhoopy.farm;

import dev.lhoopy.content.PlantDef;

public final class CropGrowthService {
    static final double WRONG_PLOT_MULTIPLIER = 0.4D;
    private static final double DRY_GROWTH_MULTIPLIER = 0.2D;
    private static final double WATERED_GROWTH_MULTIPLIER = 1.0D;

    public long remainingSeconds(FarmPlot plot, PlantDef plant, long nowMillis) {
        long requiredMillis = plant.getGrowthSeconds() * 1000L;
        long remainingGrowthMillis = Math.max(0L, requiredMillis - plot.getGrowthProgressMillis());
        double multiplier = growthMultiplier(plot, plant, nowMillis);
        long realMillis = (long) Math.ceil(remainingGrowthMillis / multiplier);
        return (realMillis + 999L) / 1000L;
    }

    public int growthPercent(FarmPlot plot, PlantDef plant) {
        long requiredMillis = plant.getGrowthSeconds() * 1000L;
        if (requiredMillis <= 0L) {
            return 100;
        }
        return (int) Math.min(100L, plot.getGrowthProgressMillis() * 100L / requiredMillis);
    }

    public void updateGrowth(FarmPlot plot, PlantDef plant, long nowMillis) {
        if (plot.isEmpty() || plant == null) {
            return;
        }
        long lastUpdate = plot.getLastGrowthUpdateMillis() <= 0L ? plot.getPlantedAtMillis() : plot.getLastGrowthUpdateMillis();
        if (lastUpdate <= 0L || nowMillis <= lastUpdate) {
            plot.setLastGrowthUpdateMillis(nowMillis);
            return;
        }

        long wateredUntil = plot.getWateredUntilMillis();
        long wateredMillis;
        long dryMillis;

        if (lastUpdate < wateredUntil) {
            long wateredEnd = Math.min(nowMillis, wateredUntil);
            wateredMillis = Math.max(0L, wateredEnd - lastUpdate);
            dryMillis = Math.max(0L, nowMillis - wateredEnd);
        } else {
            wateredMillis = 0L;
            dryMillis = nowMillis - lastUpdate;
        }

        double plotMultiplier = plotTypeMultiplier(plot, plant);
        long growthGain = Math.round((wateredMillis * WATERED_GROWTH_MULTIPLIER + dryMillis * DRY_GROWTH_MULTIPLIER) * plotMultiplier);
        long requiredMillis = Math.max(0L, plant.getGrowthSeconds() * 1000L);
        plot.setGrowthProgressMillis(Math.min(requiredMillis, plot.getGrowthProgressMillis() + growthGain));
        plot.setLastGrowthUpdateMillis(nowMillis);
    }

    public boolean isCorrectPlotType(FarmPlot plot, PlantDef plant) {
        return plot.getPlotTypeId().equalsIgnoreCase(plant.getPlotTypeId());
    }

    private double growthMultiplier(FarmPlot plot, PlantDef plant, long nowMillis) {
        double waterMultiplier = plot.getWateredUntilMillis() > nowMillis ? WATERED_GROWTH_MULTIPLIER : DRY_GROWTH_MULTIPLIER;
        return waterMultiplier * plotTypeMultiplier(plot, plant);
    }

    private double plotTypeMultiplier(FarmPlot plot, PlantDef plant) {
        return isCorrectPlotType(plot, plant) ? 1.0D : WRONG_PLOT_MULTIPLIER;
    }
}
