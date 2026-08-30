package dev.lhoopy.economy;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.PlortDef;
import dev.lhoopy.pen.PlortProductionService;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.storage.StoredItem;
import dev.lhoopy.storage.VacpackLimits;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PlortsCommand {
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final PriceService priceService;
    private final SellTerminalService sellTerminalService;
    private final PlortProductionService plortProductionService;

    public PlortsCommand(ContentRegistry contentRegistry, ProfileService profileService, PriceService priceService, SellTerminalService sellTerminalService, PlortProductionService plortProductionService) {
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.priceService = priceService;
        this.sellTerminalService = sellTerminalService;
        this.plortProductionService = plortProductionService;
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

        this.plortProductionService.updateProduction(player, profile, true);

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("collect")) {
                this.plortProductionService.collectPenPlorts(player, profile, true);
                sendPlortStatus(player, profile);
                return;
            }
            if (args[0].equalsIgnoreCase("sell")) {
                this.sellTerminalService.sellAllPlorts(player, profile);
                return;
            }
            if (!args[0].equalsIgnoreCase("status")) {
                player.sendMessage(ChatColor.YELLOW + "/plorts");
                player.sendMessage(ChatColor.YELLOW + "/plorts collect");
                player.sendMessage(ChatColor.YELLOW + "/plorts sell");
                return;
            }
        }

        sendPlortStatus(player, profile);
    }

    void sendPlortStatus(Player player, PlayerProfile profile) {
        player.sendMessage(ChatColor.GOLD + "Плорты и монеты:");
        player.sendMessage(ChatColor.YELLOW + "Монеты: " + ChatColor.WHITE + profile.getCoins());
        int penPlorts = countPlorts(profile.getPenPlortStorage().getItems());
        player.sendMessage(ChatColor.YELLOW + "Загон: " + ChatColor.WHITE + penPlorts + ChatColor.GRAY + " плортов");
        player.sendMessage(ChatColor.YELLOW + "Вакпак: " + ChatColor.WHITE
                + VacpackLimits.used(profile.getVacpackStorage(), "plorts") + "/" + profile.getVacpackPlortCapacity()
                + ChatColor.GRAY + " плортов");
        int storagePlorts = countPlorts(profile.getStorage().getItems());
        player.sendMessage(ChatColor.YELLOW + "Склад: " + ChatColor.WHITE + storagePlorts + ChatColor.GRAY + " плортов");
        boolean hasPlorts = false;
        for (StoredItem item : profile.getVacpackStorage().getItems()) {
            PlortDef plort = this.contentRegistry.getPlort(item.getItemId());
            if (plort == null || item.getAmount() <= 0) {
                continue;
            }
            hasPlorts = true;
            int price = this.priceService.getSellPrice(plort);
            int value = item.getAmount() * price;
            player.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + plort.getDisplayName()
                    + ChatColor.GRAY + ": " + ChatColor.YELLOW + item.getAmount()
                    + ChatColor.GRAY + " x " + price
                    + ChatColor.GRAY + " = " + ChatColor.GOLD + value);
        }
        if (!hasPlorts && penPlorts <= 0 && storagePlorts <= 0) {
            player.sendMessage(ChatColor.GRAY + "Плортов пока нет. Посели слаймов в загон и подожди немного.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "/plorts collect " + ChatColor.GRAY + "- собрать плорты из загона в вакпак");
            player.sendMessage(ChatColor.YELLOW + "/plorts sell " + ChatColor.GRAY + "- продать вакпак и склад");
            player.sendMessage(ChatColor.YELLOW + "/sellterminal " + ChatColor.GRAY + "- посмотреть оценку продажи");
        }
    }

    private int countPlorts(Iterable<StoredItem> items) {
        int total = 0;
        for (StoredItem item : items) {
            PlortDef plort = this.contentRegistry.getPlort(item.getItemId());
            if (plort != null && item.getAmount() > 0) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
