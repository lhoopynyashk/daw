package dev.lhoopy.farm;

import dev.lhoopy.content.PlantDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CropGrowthServiceTest {
    private static final long START = 100_000L;

    private final CropGrowthService service = new CropGrowthService();
    private final PlantDef plant = new PlantDef(
            "plant_carrot",
            "Carrot",
            "seed_carrot",
            "basic",
            "food_carrot",
            10,
            1,
            2
    );

    @Test
    void dryPlotGrowsAtReducedSpeed() {
        FarmPlot plot = plantedPlot("basic");

        this.service.updateGrowth(plot, this.plant, START + 10_000L);

        assertEquals(2_000L, plot.getGrowthProgressMillis());
        assertEquals(20, this.service.growthPercent(plot, this.plant));
    }

    @Test
    void updateSplitsWateredAndDryTime() {
        FarmPlot plot = plantedPlot("basic");
        plot.setWateredUntilMillis(START + 5_000L);

        this.service.updateGrowth(plot, this.plant, START + 10_000L);

        assertEquals(6_000L, plot.getGrowthProgressMillis());
    }

    @Test
    void wrongPlotTypeAppliesAdditionalPenalty() {
        FarmPlot plot = plantedPlot("hot");

        this.service.updateGrowth(plot, this.plant, START + 10_000L);

        assertEquals(800L, plot.getGrowthProgressMillis());
    }

    @Test
    void growthProgressStopsAtRequiredDuration() {
        FarmPlot plot = plantedPlot("basic");

        this.service.updateGrowth(plot, this.plant, START + 100_000L);

        assertEquals(10_000L, plot.getGrowthProgressMillis());
        assertEquals(0L, this.service.remainingSeconds(plot, this.plant, START + 100_000L));
    }

    private static FarmPlot plantedPlot(String plotType) {
        FarmPlot plot = new FarmPlot("plot_1");
        plot.setPlotTypeId(plotType);
        plot.plant("plant_carrot", START);
        return plot;
    }
}
