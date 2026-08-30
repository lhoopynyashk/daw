package dev.lhoopy.farm;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class FarmCommand {
    private final ProfileService profileService;
    private final FarmMessages messages;
    private final FarmPlotActions actions;

    public FarmCommand(ContentRegistry contentRegistry, ProfileService profileService, CropGrowthService cropGrowthService, WateringService wateringService) {
        this.profileService = profileService;
        this.messages = new FarmMessages(contentRegistry, cropGrowthService);
        this.actions = new FarmPlotActions(contentRegistry, profileService, cropGrowthService, wateringService);
    }

    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только игроку.");
            return;
        }

        Player player = (Player) sender;
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            this.messages.sendUsage(player);
            return;
        }

        PlayerProfile profile = loadedProfile(player);
        if (profile == null) {
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("plots")) {
            this.messages.sendPlots(player, profile);
            this.profileService.saveLoaded(player.getUniqueId());
            return;
        }
        if (action.equals("crops") || action.equals("shop")) {
            this.messages.sendCrops(player, profile);
            return;
        }
        if (action.equals("buy")) {
            if (args.length < 2) {
                this.messages.sendUsage(player);
                return;
            }
            this.actions.buySeeds(player, profile, args[1], parsePositive(args, 2, 1));
            return;
        }
        if (action.equals("plant")) {
            if (args.length < 3) {
                this.messages.sendUsage(player);
                return;
            }
            this.actions.plant(player, profile, args[1], args[2]);
            return;
        }
        if (action.equals("water")) {
            if (args.length < 2) {
                this.messages.sendUsage(player);
                return;
            }
            this.actions.water(player, profile, args[1]);
            return;
        }
        if (action.equals("settype")) {
            if (args.length < 3) {
                this.messages.sendUsage(player);
                return;
            }
            this.actions.setPlotType(player, profile, args[1], args[2]);
            return;
        }
        if (action.equals("harvest")) {
            if (args.length < 2) {
                this.messages.sendUsage(player);
                return;
            }
            this.actions.harvest(player, profile, args[1]);
            return;
        }

        this.messages.sendUsage(player);
    }

    private PlayerProfile loadedProfile(Player player) {
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль ещё загружается, попробуй через пару секунд.");
            return null;
        }

        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.YELLOW + "Профиль всё ещё загружается.");
        }
        return profile;
    }

    private static int parsePositive(String[] args, int index, int fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(args[index]));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
