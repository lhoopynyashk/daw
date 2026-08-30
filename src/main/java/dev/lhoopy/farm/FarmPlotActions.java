package dev.lhoopy.farm;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.PlantDef;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.storage.StoredItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

final class FarmPlotActions {
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final CropGrowthService cropGrowthService;
    private final WateringService wateringService;

    FarmPlotActions(ContentRegistry contentRegistry, ProfileService profileService, CropGrowthService cropGrowthService, WateringService wateringService) {
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.cropGrowthService = cropGrowthService;
        this.wateringService = wateringService;
    }

    void buySeeds(Player player, PlayerProfile profile, String plantOrSeedId, int amount) {
        PlantDef plant = this.contentRegistry.getPlantOrSeed(plantOrSeedId);
        if (plant == null) {
            player.sendMessage(ChatColor.RED + "Неизвестная культура или семя: " + plantOrSeedId);
            return;
        }

        long price = (long) plant.getSeedPrice() * amount;
        if (profile.getCoins() < price) {
            player.sendMessage(ChatColor.RED + "Нужно монет: " + price + ", у тебя: " + profile.getCoins() + ".");
            return;
        }

        profile.setCoins(profile.getCoins() - price);
        profile.getStorage().add(plant.getSeedId(), amount);
        save(player);
        player.sendMessage(ChatColor.GREEN + "Куплено: " + ChatColor.WHITE + amount + "x " + plant.getSeedId()
                + ChatColor.GREEN + " за " + ChatColor.GOLD + price + " монет");
    }

    void plant(Player player, PlayerProfile profile, String plotId, String plantOrSeedId) {
        FarmPlot plot = findPlot(player, profile, plotId);
        if (plot == null || !ensureEmpty(player, plot)) {
            return;
        }

        PlantDef plant = this.contentRegistry.getPlantOrSeed(plantOrSeedId);
        if (plant == null) {
            player.sendMessage(ChatColor.RED + "Неизвестное растение или семя: " + plantOrSeedId);
            return;
        }
        if (!removeSeed(profile, plant.getSeedId())) {
            player.sendMessage(ChatColor.RED + "В складе нет семени: " + plant.getSeedId());
            return;
        }

        plot.plant(plant.getId(), System.currentTimeMillis());
        save(player);
        player.sendMessage(ChatColor.GREEN + "Посажено: " + ChatColor.WHITE + plant.getDisplayName()
                + ChatColor.GREEN + " на " + ChatColor.WHITE + plot.getId());
    }

    void plantFirstAvailableSeed(Player player, PlayerProfile profile, String plotId) {
        FarmPlot plot = findPlot(player, profile, plotId);
        if (plot == null || !ensureEmpty(player, plot)) {
            return;
        }

        for (StoredItem item : profile.getVacpackStorage().getItems()) {
            PlantDef plant = this.contentRegistry.getPlantBySeed(item.getItemId());
            if (plant == null || item.getAmount() <= 0) {
                continue;
            }
            plant(player, profile, plotId, plant.getSeedId());
            return;
        }
        for (StoredItem item : profile.getStorage().getItems()) {
            PlantDef plant = this.contentRegistry.getPlantBySeed(item.getItemId());
            if (plant == null || item.getAmount() <= 0) {
                continue;
            }
            plant(player, profile, plotId, plant.getSeedId());
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "В складе нет семян. Получи старт через /starter или скрафти семена.");
    }

    void water(Player player, PlayerProfile profile, String plotId) {
        FarmPlot plot = findPlot(player, profile, plotId);
        if (plot == null) {
            return;
        }

        this.wateringService.water(plot, System.currentTimeMillis());
        save(player);
        player.sendMessage(ChatColor.AQUA + "Грядка полита: " + ChatColor.WHITE + plot.getId());
    }

    void setPlotType(Player player, PlayerProfile profile, String plotId, String plotTypeId) {
        if (!player.hasPermission("slimes.farm.debug")) {
            player.sendMessage(ChatColor.RED + "Нет доступа.");
            return;
        }

        setPlotTypeInternal(player, profile, plotId, plotTypeId);
    }

    void cyclePlotType(Player player, PlayerProfile profile, String plotId) {
        FarmPlot plot = findPlot(player, profile, plotId);
        if (plot == null) {
            return;
        }
        setPlotTypeInternal(player, profile, plotId, nextPlotType(plot.getPlotTypeId()));
    }

    void changePlotType(Player player, PlayerProfile profile, String plotId, String plotTypeId) {
        if (!isKnownPlotType(plotTypeId)) {
            player.sendMessage(ChatColor.RED + "Неизвестный тип грядки: " + plotTypeId);
            return;
        }
        setPlotTypeInternal(player, profile, plotId, plotTypeId.toLowerCase());
    }

    void harvest(Player player, PlayerProfile profile, String plotId) {
        FarmPlot plot = findPlot(player, profile, plotId);
        if (plot == null || ensurePlanted(player, plot) == null) {
            return;
        }

        PlantDef plant = this.contentRegistry.getPlant(plot.getPlantId());
        if (plant == null) {
            player.sendMessage(ChatColor.RED + "Неизвестное растение на грядке: " + plot.getPlantId());
            return;
        }

        long now = System.currentTimeMillis();
        this.cropGrowthService.updateGrowth(plot, plant, now);
        long remaining = this.cropGrowthService.remainingSeconds(plot, plant, now);
        if (remaining > 0L) {
            player.sendMessage(ChatColor.YELLOW + "Ещё не выросло: " + remaining + "s");
            return;
        }

        profile.getStorage().add(plant.getOutputFoodId(), plant.getHarvestAmount());
        plot.clear();
        save(player);
        player.sendMessage(ChatColor.GREEN + "Собрано: " + ChatColor.WHITE + plant.getHarvestAmount()
                + "x " + plant.getOutputFoodId());
    }

    private void setPlotTypeInternal(Player player, PlayerProfile profile, String plotId, String plotTypeId) {
        FarmPlot plot = findPlot(player, profile, plotId);
        if (plot == null) {
            return;
        }

        PlantDef plant = this.contentRegistry.getPlant(plot.getPlantId());
        if (!plot.isEmpty() && plant != null) {
            this.cropGrowthService.updateGrowth(plot, plant, System.currentTimeMillis());
        }
        plot.setPlotTypeId(plotTypeId);
        save(player);
        player.sendMessage(ChatColor.GREEN + "Стиль грядки изменён: " + ChatColor.WHITE + plot.getId()
                + ChatColor.GREEN + " -> " + ChatColor.WHITE + plot.getPlotTypeId());
    }

    private FarmPlot findPlot(Player player, PlayerProfile profile, String plotId) {
        FarmPlot plot = profile.getFarmData().getPlot(plotId);
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "Неизвестная грядка: " + plotId);
        }
        return plot;
    }

    private boolean ensureEmpty(Player player, FarmPlot plot) {
        if (plot.isEmpty()) {
            return true;
        }
        player.sendMessage(ChatColor.RED + "Грядка уже занята: " + plot.getId());
        return false;
    }

    private FarmPlot ensurePlanted(Player player, FarmPlot plot) {
        if (!plot.isEmpty()) {
            return plot;
        }
        player.sendMessage(ChatColor.YELLOW + "Грядка пустая: " + plot.getId());
        return null;
    }

    private void save(Player player) {
        this.profileService.saveLoaded(player.getUniqueId());
    }

    private static boolean removeSeed(PlayerProfile profile, String seedId) {
        if (profile.getVacpackStorage().remove(seedId, 1)) {
            return true;
        }
        return profile.getStorage().remove(seedId, 1);
    }

    private static String nextPlotType(String current) {
        if ("basic".equalsIgnoreCase(current)) {
            return "wet";
        }
        if ("wet".equalsIgnoreCase(current)) {
            return "mycelium";
        }
        if ("mycelium".equalsIgnoreCase(current)) {
            return "hot";
        }
        if ("hot".equalsIgnoreCase(current)) {
            return "crystal";
        }
        if ("crystal".equalsIgnoreCase(current)) {
            return "sky";
        }
        return "basic";
    }

    private static boolean isKnownPlotType(String type) {
        return "basic".equalsIgnoreCase(type)
                || "wet".equalsIgnoreCase(type)
                || "mycelium".equalsIgnoreCase(type)
                || "hot".equalsIgnoreCase(type)
                || "crystal".equalsIgnoreCase(type)
                || "sky".equalsIgnoreCase(type);
    }
}
