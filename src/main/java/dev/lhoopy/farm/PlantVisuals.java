package dev.lhoopy.farm;

import dev.lhoopy.content.PlantDef;
import org.bukkit.Material;

import java.util.Locale;

final class PlantVisuals {
    static final int STAGE_COUNT = 4;

    private PlantVisuals() {
    }

    static int stage(int growthPercent) {
        if (growthPercent >= 100) {
            return 3;
        }
        if (growthPercent >= 50) {
            return 2;
        }
        if (growthPercent >= 25) {
            return 1;
        }
        return 0;
    }

    static boolean occupiesCell(int stage, int dx, int dz) {
        int cell = dz * 2 + dx;
        int visibleCells;
        switch (stage) {
            case 0:
                visibleCells = 1;
                break;
            case 1:
                visibleCells = 2;
                break;
            default:
                visibleCells = 4;
                break;
        }
        // Diagonal-first order makes the 2x2 cluster read as one spreading plant.
        int[] order = {0, 3, 1, 2};
        for (int index = 0; index < visibleCells; index++) {
            if (order[index] == cell) {
                return true;
            }
        }
        return false;
    }

    static Material material(PlantDef plant) {
        String id = normalize(plant.getId());
        String plotType = normalize(plant.getPlotTypeId());
        if ("mycelium".equals(plotType)) {
            return id.hashCode() % 2 == 0 ? Material.BROWN_MUSHROOM : Material.RED_MUSHROOM;
        }
        if ("hot".equals(plotType)) {
            return Material.NETHER_WARTS;
        }
        if (id.contains("carrot")) {
            return Material.CARROT;
        }
        if (id.contains("pumpkin")) {
            return Material.PUMPKIN_STEM;
        }
        if (id.contains("berry") || "sky".equals(plotType)) {
            return Material.MELON_STEM;
        }
        if (id.contains("root") || id.contains("tuber") || id.contains("turnip")) {
            return Material.POTATO;
        }
        return Material.CROPS;
    }

    static byte data(Material material, int stage) {
        if (material == Material.NETHER_WARTS) {
            return (byte) stage;
        }
        if (material == Material.BROWN_MUSHROOM || material == Material.RED_MUSHROOM) {
            return 0;
        }
        int[] cropData = {0, 2, 5, 7};
        return (byte) cropData[Math.max(0, Math.min(cropData.length - 1, stage))];
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
