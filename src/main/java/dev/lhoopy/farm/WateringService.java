package dev.lhoopy.farm;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.PlantDef;

public final class WateringService {
    private static final long WATER_DURATION_MILLIS = 10L * 60L * 1000L;

    private final ContentRegistry contentRegistry;
    private final CropGrowthService cropGrowthService;

    public WateringService(ContentRegistry contentRegistry, CropGrowthService cropGrowthService) {
        this.contentRegistry = contentRegistry;
        this.cropGrowthService = cropGrowthService;
    }

    public void water(FarmPlot plot, long nowMillis) {
        if (!plot.isEmpty()) {
            PlantDef plant = this.contentRegistry.getPlant(plot.getPlantId());
            this.cropGrowthService.updateGrowth(plot, plant, nowMillis);
        }
        plot.setWateredUntilMillis(nowMillis + WATER_DURATION_MILLIS);
    }
}
