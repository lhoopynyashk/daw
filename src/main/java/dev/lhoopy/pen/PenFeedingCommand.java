package dev.lhoopy.pen;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PenFeedingCommand {
    private final ProfileService profileService;
    private final SlimeFeedingService feedingService;

    public PenFeedingCommand(ContentRegistry contentRegistry, ProfileService profileService, long fedDurationMillis) {
        this.profileService = profileService;
        this.feedingService = new SlimeFeedingService(contentRegistry, fedDurationMillis);
    }

    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }
        Player player = (Player) sender;
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль ещё загружается, попробуй через пару секунд.");
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.YELLOW + "Профиль всё ещё загружается.");
            return;
        }
        if (args.length < 1) {
            sendUsage(player);
            return;
        }
        if (profile.getPenSlimes().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "В загоне нет слаймов.");
            return;
        }

        String target = args[0];
        if (target.equalsIgnoreCase("all")) {
            feedAll(player, profile);
            return;
        }

        for (PenSlime slime : profile.getPenSlimes()) {
            if (slime.getSlimeId().equalsIgnoreCase(target)) {
                feedOne(player, profile, slime);
                return;
            }
        }
        player.sendMessage(ChatColor.RED + "No slime in pen: " + target);
    }

    private void feedAll(Player player, PlayerProfile profile) {
        int fed = 0;
        String lastError = null;
        long now = System.currentTimeMillis();
        for (PenSlime slime : profile.getPenSlimes()) {
            SlimeFeedingService.FeedResult result = this.feedingService.feed(profile, slime, now);
            if (result.isSuccess()) {
                fed++;
            } else {
                lastError = result.getMessage();
            }
        }
        if (fed > 0) {
            this.profileService.saveLoaded(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Покормлено слаймов: " + ChatColor.WHITE + fed);
            return;
        }
        player.sendMessage(ChatColor.RED + (lastError == null ? "No slimes fed." : lastError));
    }

    private void feedOne(Player player, PlayerProfile profile, PenSlime slime) {
        SlimeFeedingService.FeedResult result = this.feedingService.feed(profile, slime, System.currentTimeMillis());
        if (!result.isSuccess()) {
            player.sendMessage(ChatColor.RED + result.getMessage());
            return;
        }
        this.profileService.saveLoaded(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Слайм накормлен: " + ChatColor.WHITE + result.getSlimeId()
                + ChatColor.GRAY + " (" + result.getFoodId() + ")");
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/penfeed all");
        sender.sendMessage(ChatColor.YELLOW + "/penfeed <slimeId>");
    }
}
