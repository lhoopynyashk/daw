package dev.lhoopy.farm;

import dev.lhoopy.content.PlantDef;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlantVisualsTest {
    @Test
    void growthPercentMapsToFourStableStages() {
        assertEquals(0, PlantVisuals.stage(0));
        assertEquals(0, PlantVisuals.stage(24));
        assertEquals(1, PlantVisuals.stage(25));
        assertEquals(2, PlantVisuals.stage(50));
        assertEquals(2, PlantVisuals.stage(99));
        assertEquals(3, PlantVisuals.stage(100));
    }

    @Test
    void plantSpreadsAcrossPlotAsItGrows() {
        assertTrue(PlantVisuals.occupiesCell(0, 0, 0));
        assertFalse(PlantVisuals.occupiesCell(0, 1, 1));
        assertTrue(PlantVisuals.occupiesCell(1, 1, 1));
        assertTrue(PlantVisuals.occupiesCell(2, 1, 0));
        assertTrue(PlantVisuals.occupiesCell(3, 0, 1));
    }

    @Test
    void familiarPlantsUseRecognizableVanillaModels() {
        assertEquals(Material.CARROT, PlantVisuals.material(plant("plant_meadow_carrot", "basic")));
        assertEquals(Material.PUMPKIN_STEM, PlantVisuals.material(plant("plant_spicy_pumpkin", "basic")));
        assertEquals(Material.NETHER_WARTS, PlantVisuals.material(plant("plant_hot_pepper", "hot")));
        Material mushroom = PlantVisuals.material(plant("plant_bright_mushroom", "mycelium"));
        assertTrue(mushroom == Material.BROWN_MUSHROOM || mushroom == Material.RED_MUSHROOM);
    }

    private static PlantDef plant(String id, String plotType) {
        return new PlantDef(id, id, "seed_" + id, plotType, id, 60, 1, 1);
    }
}
