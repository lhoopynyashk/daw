package dev.lhoopy.farm;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.world.RanchWorldService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

final class RanchJoinFlow {
    private static final long LOADING_CLOSE_DELAY_TICKS = 60L;

    private final SlimesPlugin plugin;
    private final RanchWorldService ranchWorldService;
    private final EnginexHuntBridge enginexHuntBridge;
    private final ProfileService profileService;
    private final FarmWorldService farmWorldService;

    RanchJoinFlow(
            SlimesPlugin plugin,
            RanchWorldService ranchWorldService,
            EnginexHuntBridge enginexHuntBridge,
            ProfileService profileService,
            FarmWorldService farmWorldService
    ) {
        this.plugin = plugin;
        this.ranchWorldService = ranchWorldService;
        this.enginexHuntBridge = enginexHuntBridge;
        this.profileService = profileService;
        this.farmWorldService = farmWorldService;
    }

    void start(Player player) {
        this.enginexHuntBridge.beginOrUpdateLoading(player, "Загрузка профиля", 25);
        this.profileService.load(player).thenAccept(profile -> {
            if (!player.isOnline()) {
                return;
            }
            Bukkit.getScheduler().runTask(this.plugin, () -> openRanch(player, profile));
        }).exceptionally(error -> {
            if (player.isOnline()) {
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    this.enginexHuntBridge.closeLoading(player);
                    player.sendMessage(ChatColor.RED + "Не удалось загрузить ранчо. Перезайди через минуту.");
                });
            }
            return null;
        });
    }

    private void openRanch(Player player, PlayerProfile profile) {
        if (!player.isOnline()) {
            return;
        }

        this.enginexHuntBridge.sendLoadingStatus(player, "Загрузка ранчо", 55);
        try {
            Location spawn = this.ranchWorldService.isRanchWorld(player.getWorld())
                    ? player.getLocation()
                    : this.ranchWorldService.openRanch(player);

            this.enginexHuntBridge.sendLoadingStatus(player, "Подготовка загона", 82);
            this.farmWorldService.ensureRanchPenNpc(player, spawn.getWorld());
            this.enginexHuntBridge.sendLoadingStatus(player, "Готово", 100);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline()) {
                    this.enginexHuntBridge.closeLoading(player);
                }
            }, LOADING_CLOSE_DELAY_TICKS);
        } catch (RuntimeException exception) {
            this.enginexHuntBridge.closeLoading(player);
            this.plugin.getLogger().warning("Could not open ranch for " + player.getName() + ": " + exception.getMessage());
            player.sendMessage(ChatColor.RED + "Ранчо пока не открылось. Попробуй /ranch.");
        }
    }
}
