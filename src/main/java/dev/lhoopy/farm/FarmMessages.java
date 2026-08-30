package dev.lhoopy.farm;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.PlantDef;
import dev.lhoopy.profile.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class FarmMessages {
    private final ContentRegistry contentRegistry;
    private final CropGrowthService cropGrowthService;

    FarmMessages(ContentRegistry contentRegistry, CropGrowthService cropGrowthService) {
        this.contentRegistry = contentRegistry;
        this.cropGrowthService = cropGrowthService;
    }

    void sendPlots(Player player, PlayerProfile profile) {
        long now = System.currentTimeMillis();
        player.sendMessage(ChatColor.GREEN + "Грядки фермы:");
        for (FarmPlot plot : profile.getFarmData().getPlots()) {
            sendPlotLine(player, plot, now);
        }
    }

    void sendCrops(Player player, PlayerProfile profile) {
        player.sendMessage(ChatColor.GREEN + "Культуры фермы:");
        player.sendMessage(ChatColor.GRAY + "Монеты: " + ChatColor.GOLD + profile.getCoins());
        for (PlantDef plant : this.contentRegistry.plants()) {
            player.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + plant.getId()
                    + ChatColor.GRAY + " seed=" + ChatColor.YELLOW + plant.getSeedId()
                    + ChatColor.GRAY + " price=" + ChatColor.GOLD + plant.getSeedPrice()
                    + ChatColor.GRAY + " harvest=" + ChatColor.GREEN + plant.getHarvestAmount() + "x " + plant.getOutputFoodId()
                    + ChatColor.GRAY + " storage=" + ChatColor.AQUA + profile.getStorage().getAmount(plant.getSeedId()) + " seeds"
                    + ChatColor.GRAY + "/" + ChatColor.AQUA + profile.getStorage().getAmount(plant.getOutputFoodId()) + " food");
        }
    }

    void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/farm plots");
        sender.sendMessage(ChatColor.YELLOW + "/farm crops");
        sender.sendMessage(ChatColor.YELLOW + "/farm buy <plantId|seedId> [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/farm plant <plotId> <seedId>");
        sender.sendMessage(ChatColor.YELLOW + "/farm water <plotId>");
        sender.sendMessage(ChatColor.YELLOW + "/farm settype <plotId> <type>");
        sender.sendMessage(ChatColor.YELLOW + "/farm harvest <plotId>");
    }

    private void sendPlotLine(Player player, FarmPlot plot, long now) {
        if (plot.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "- " + plot.getId() + ": " + ChatColor.WHITE + "пусто");
            return;
        }

        PlantDef plant = this.contentRegistry.getPlant(plot.getPlantId());
        if (plant == null) {
            player.sendMessage(ChatColor.GRAY + "- " + plot.getId() + ": "
                    + ChatColor.DARK_AQUA + "[" + plot.getPlotTypeId() + "] "
                    + ChatColor.RED + "неизвестное растение " + plot.getPlantId());
            return;
        }

        this.cropGrowthService.updateGrowth(plot, plant, now);
        long remaining = this.cropGrowthService.remainingSeconds(plot, plant, now);
        int percent = this.cropGrowthService.growthPercent(plot, plant);
        String plotFit = this.cropGrowthService.isCorrectPlotType(plot, plant)
                ? ChatColor.GREEN + "подходящая грядка"
                : ChatColor.GOLD + "неподходящая грядка x" + CropGrowthService.WRONG_PLOT_MULTIPLIER;
        String readyText = remaining <= 0L ? ChatColor.GREEN + "готово" : ChatColor.YELLOW + String.valueOf(remaining) + "s";
        String watered = plot.getWateredUntilMillis() > now ? ChatColor.AQUA + " полито" : ChatColor.DARK_GRAY + " сухо";

        player.sendMessage(ChatColor.GRAY + "- " + plot.getId() + ": "
                + ChatColor.DARK_AQUA + "[" + plot.getPlotTypeId() + "] "
                + ChatColor.WHITE + plot.getPlantId()
                + ChatColor.GRAY + " seed=" + ChatColor.YELLOW + plant.getSeedId()
                + ChatColor.GRAY + " grows=" + ChatColor.AQUA + plant.getGrowthSeconds() + "s"
                + ChatColor.GRAY + " (" + ChatColor.WHITE + percent + "%" + ChatColor.GRAY + ", "
                + readyText + ChatColor.GRAY + ", " + watered + ChatColor.GRAY + ", " + plotFit + ChatColor.GRAY + ")");
    }
}
