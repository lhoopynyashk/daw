package dev.lhoopy.farm;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.PlantDef;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.storage.PlayerStorage;
import gg.cristalix.wada.transfer.ModTransfer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

final class FarmPlotMenuService {
    static final String OPEN_CHANNEL = "slimehunt:plot";
    static final String ACTION_CHANNEL = "slimehunt:plotact";

    private static final PlotType[] PLOT_TYPES = {
            new PlotType("basic", "Обычная"),
            new PlotType("wet", "Влажная"),
            new PlotType("mycelium", "Грибница"),
            new PlotType("hot", "Горячая"),
            new PlotType("crystal", "Кристальная"),
            new PlotType("sky", "Небесная")
    };

    private final ContentRegistry contentRegistry;
    private final CropGrowthService cropGrowthService;
    private final EnginexHuntBridge clientBridge;

    FarmPlotMenuService(ContentRegistry contentRegistry, CropGrowthService cropGrowthService,
                        EnginexHuntBridge clientBridge) {
        this.contentRegistry = contentRegistry;
        this.cropGrowthService = cropGrowthService;
        this.clientBridge = clientBridge;
    }

    void open(Player player, PlayerProfile profile, String plotId) {
        FarmPlot plot = profile.getFarmData().getPlot(plotId);
        if (plot == null || !this.clientBridge.isClientModLoaded(player)) {
            return;
        }

        long now = System.currentTimeMillis();
        PlantDef planted = plot.isEmpty() ? null : this.contentRegistry.getPlant(plot.getPlantId());
        if (planted != null) {
            this.cropGrowthService.updateGrowth(plot, planted, now);
        }

        int growth = planted == null ? 0 : this.cropGrowthService.growthPercent(plot, planted);
        long remaining = planted == null ? 0L : this.cropGrowthService.remainingSeconds(plot, planted, now);
        long waterRemaining = Math.max(0L, (plot.getWateredUntilMillis() - now + 999L) / 1000L);
        List<SeedEntry> seeds = availableSeeds(profile);

        ModTransfer transfer = new ModTransfer()
                .writeString(plot.getId())
                .writeString(displayPlotName(plot.getId()))
                .writeString(plot.getPlotTypeId())
                .writeBoolean(planted != null)
                .writeString(planted == null ? "" : planted.getDisplayName())
                .writeString(planted == null ? "" : planted.getId())
                .writeInt(growth)
                .writeInt((int) Math.min(Integer.MAX_VALUE, remaining))
                .writeBoolean(planted != null && remaining <= 0L)
                .writeInt(planted == null ? 0 : planted.getHarvestAmount())
                .writeInt((int) Math.min(Integer.MAX_VALUE, waterRemaining))
                .writeInt(seeds.size());

        for (SeedEntry seed : seeds) {
            transfer.writeString(seed.plant.getSeedId())
                    .writeString(seed.plant.getDisplayName())
                    .writeString(seed.plant.getPlotTypeId())
                    .writeInt(seed.amount)
                    .writeInt(seed.plant.getGrowthSeconds());
        }
        transfer.writeInt(PLOT_TYPES.length);
        for (PlotType type : PLOT_TYPES) {
            transfer.writeString(type.id).writeString(type.title);
        }
        transfer.send(OPEN_CHANNEL, player);
    }

    private List<SeedEntry> availableSeeds(PlayerProfile profile) {
        List<SeedEntry> result = new ArrayList<>();
        for (PlantDef plant : this.contentRegistry.plants()) {
            int amount = amount(profile.getStorage(), plant.getSeedId())
                    + amount(profile.getVacpackStorage(), plant.getSeedId());
            if (amount > 0) {
                result.add(new SeedEntry(plant, amount));
            }
        }
        return result;
    }

    private static int amount(PlayerStorage storage, String itemId) {
        return storage == null ? 0 : storage.getAmount(itemId);
    }

    private static String displayPlotName(String plotId) {
        String suffix = plotId.replace("plot_", "").replace('_', ' ');
        return "Грядка " + suffix;
    }

    private static final class SeedEntry {
        private final PlantDef plant;
        private final int amount;

        private SeedEntry(PlantDef plant, int amount) {
            this.plant = plant;
            this.amount = amount;
        }
    }

    private static final class PlotType {
        private final String id;
        private final String title;

        private PlotType(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }
}
