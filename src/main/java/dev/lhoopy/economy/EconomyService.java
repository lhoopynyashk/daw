package dev.lhoopy.economy;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.PlortDef;
import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.pen.PlortProductionService;
import dev.lhoopy.pen.PenStyleCatalog;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.hunt.EnginexHuntBridge;
import gg.cristalix.wada.transfer.ModTransfer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EconomyService implements PluginService {
    public static final String TERMINAL_OPEN_CHANNEL = "slimehunt:sellmenu";
    public static final String TERMINAL_ACTION_CHANNEL = "slimehunt:sellact";

    private final SlimesPlugin plugin;
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final EnginexHuntBridge clientBridge;
    private final SellTerminalService sellTerminalService;
    private final PlortProductionService plortProductionService;
    private final PlortsCommand plortsCommand;
    private final Map<UUID, SellResult> lastSales = new ConcurrentHashMap<>();

    public EconomyService(SlimesPlugin plugin, ContentRegistry contentRegistry, ProfileService profileService,
                          PenStyleCatalog styleCatalog, EnginexHuntBridge clientBridge) {
        this.plugin = plugin;
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.clientBridge = clientBridge;
        PriceService priceService = new PriceService();
        this.sellTerminalService = new SellTerminalService(contentRegistry, profileService, priceService, styleCatalog);
        this.plortProductionService = new PlortProductionService(plugin, contentRegistry, profileService, styleCatalog);
        this.plortsCommand = new PlortsCommand(contentRegistry, profileService, priceService, this.sellTerminalService, this.plortProductionService);
    }

    @Override
    public void enable() {
        this.plortProductionService.enable();
        ModTransfer.registerChannel(TERMINAL_ACTION_CHANNEL, (player, transfer) -> {
            String plortId = transfer.readString();
            int amount = transfer.readInt();
            Bukkit.getScheduler().runTask(this.plugin, () -> sellFromMenu(player, plortId, amount));
        });
    }

    @Override
    public void shutdown() {
        this.plortProductionService.shutdown();
    }

    public void handlePlortsCommand(CommandSender sender, String[] args) {
        this.plortsCommand.handle(sender, args);
    }

    public void handleSellTerminalCommand(CommandSender sender, String[] args) {
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
        if (args.length > 0 && args[0].equalsIgnoreCase("sell")) {
            SellResult result = this.sellTerminalService.sellAllPlorts(profile);
            finishSale(player, profile, result);
            return;
        }

        openTerminal(player, profile);
    }

    public void handleVacuumUse(Player player) {
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
        if (player.isSneaking()) {
            this.sellTerminalService.sellAllPlorts(player, profile);
            return;
        }
        this.plortsCommand.sendPlortStatus(player, profile);
    }

    private void openTerminal(Player player, PlayerProfile profile) {
        if (!this.clientBridge.isClientModLoaded(player)) {
            if (!this.clientBridge.sendClientModTo(player)) {
                player.sendMessage(ChatColor.RED + "Не удалось загрузить мод терминала.");
                return;
            }
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline()) {
                    handleSellTerminalCommand(player, new String[0]);
                }
            }, 60L);
            return;
        }

        List<PlortDef> available = new ArrayList<>();
        for (PlortDef plort : this.contentRegistry.plorts()) {
            if (this.sellTerminalService.getAvailableAmount(profile, plort.getId()) > 0) {
                available.add(plort);
            }
        }
        available.sort(Comparator.comparing(PlortDef::getDisplayName));

        SellResult lastSale = this.lastSales.get(player.getUniqueId());
        ModTransfer transfer = new ModTransfer()
                .writeString(Long.toString(profile.getCoins()))
                .writeString(Long.toString(this.sellTerminalService.estimateAllPlorts(profile)))
                .writeInt(lastSale == null ? 0 : lastSale.getTotalAmount())
                .writeString(Long.toString(lastSale == null ? 0L : lastSale.getCoins()))
                .writeInt(available.size());
        for (PlortDef plort : available) {
            transfer.writeString(plort.getId())
                    .writeString(plort.getDisplayName())
                    .writeInt(this.sellTerminalService.getEffectivePrice(profile, plort))
                    .writeInt(profile.getVacpackStorage().getAmount(plort.getId()))
                    .writeInt(unprotectedStorageAmount(profile, plort.getId()));
        }
        transfer.send(TERMINAL_OPEN_CHANNEL, player);
    }

    private void sellFromMenu(Player player, String plortId, int amount) {
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Профиль не загружен.");
            return;
        }
        this.plortProductionService.updateProduction(player, profile, true);
        SellResult result = "__all__".equals(plortId)
                ? this.sellTerminalService.sellAllPlorts(profile)
                : this.sellTerminalService.sellPlort(profile, plortId, amount);
        finishSale(player, profile, result);
        openTerminal(player, profile);
    }

    private void finishSale(Player player, PlayerProfile profile, SellResult result) {
        if (result.isEmpty()) {
            player.playSound(player.getLocation(), "note.bass", 0.7F, 0.8F);
            player.sendMessage(ChatColor.YELLOW + "Продавать пока нечего.");
            return;
        }
        this.lastSales.put(player.getUniqueId(), result);
        this.profileService.saveLoaded(player.getUniqueId());
        player.playSound(player.getLocation(), "random.orb", 0.8F, 1.1F);
        player.sendMessage(ChatColor.GREEN + "Продано: " + ChatColor.WHITE + result.getTotalAmount()
                + ChatColor.GREEN + " плортов. Получено: "
                + ChatColor.GOLD + result.getCoins() + ChatColor.GREEN + " монет.");
    }

    private static int unprotectedStorageAmount(PlayerProfile profile, String plortId) {
        dev.lhoopy.storage.StoredItem item = profile.getStorage().get(plortId);
        return item == null || item.isProtectedItem() ? 0 : item.getAmount();
    }
}
