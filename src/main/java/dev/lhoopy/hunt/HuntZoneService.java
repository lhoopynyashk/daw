package dev.lhoopy.hunt;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.profile.ProfileService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.network.ISocketClient;
import ru.cristalix.core.realm.IRealmService;
import ru.cristalix.core.realm.RealmId;
import ru.cristalix.core.realm.RealmInfo;
import ru.cristalix.core.transfer.ITransferService;
import ru.cristalix.core.transfer.TransferService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HuntZoneService implements PluginService {
    private static final String CONFIG_REALM_TYPE = "hunt-zone.realm-type";
    private static final String CONFIG_REALM_ID = "hunt-zone.realm-id";
    private static final String CONFIG_MODE = "hunt-zone.mode";
    private static final String CONFIG_RANCH_REALM_TYPE = "ranch-home.realm-type";
    private static final String CONFIG_RANCH_REALM_ID = "ranch-home.realm-id";
    private static final String DEFAULT_REALM_TYPE = "HUNT";
    private static final int DEFAULT_REALM_ID = 1;
    private static final String DEFAULT_RANCH_REALM_TYPE = "TEST";
    private static final int DEFAULT_RANCH_REALM_ID = 89;

    private final SlimesPlugin plugin;
    private final ProfileService profileService;
    private final Set<UUID> transferringPlayers = new HashSet<>();

    public HuntZoneService(SlimesPlugin plugin, ProfileService profileService) {
        this.plugin = plugin;
        this.profileService = profileService;
    }

    @Override
    public void enable() {
        RealmId target = configuredRealm();
        this.plugin.getLogger().info("Hunt zone mode: " + configuredMode() + ", target realm: " + target);
    }

    @Override
    public void shutdown() {
        this.transferringPlayers.clear();
    }

    public void transferToHuntZone(Player player, String[] args) {
        if (isLocalMode() && args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Охота сейчас в режиме local: отдельный реалм не нужен.");
            return;
        }
        RealmId target = parseTarget(args);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Используй: /huntzone [realmType] [realmId]");
            return;
        }

        try {
            ITransferService transferService = ensureTransferService();
            if (transferService == null) {
                player.sendMessage(ChatColor.RED + "Сервис переноса в реалм не доступен.");
                player.sendMessage(ChatColor.GRAY + "Проверь Tower-данные в start.sh и подключение CoreAPI.");
                return;
            }

            Map<String, String> metadata = metadataFor(player, target);
            releaseAndTransfer(
                    player,
                    transferService,
                    target,
                    metadata,
                    "Отправляю в охотничью зону: "
            );
        } catch (NoClassDefFoundError | Exception error) {
            this.plugin.getLogger().warning("Could not transfer " + player.getName() + " to hunt zone " + target + ": " + error.getMessage());
            player.sendMessage(ChatColor.RED + "Перенос в реалм сейчас недоступен.");
            player.sendMessage(ChatColor.GRAY + "Проверь, что целевой реалм запущен и CoreAPI даёт TransferService.");
        }
    }

    public boolean returnToRanchIfInHuntZone(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return true;
        }

        Player player = (Player) sender;
        RealmId current = currentRealmId();
        if (current == null || !sameRealm(current, configuredRealm())) {
            return false;
        }

        RealmId target = configuredRanchRealm();
        try {
            ITransferService transferService = ensureTransferService();
            if (transferService == null) {
                player.sendMessage(ChatColor.RED + "Сервис переноса в реалм не доступен.");
                return true;
            }

            Map<String, String> metadata = new HashMap<>();
            metadata.put("slimes_destination", "ranch");
            metadata.put("return_from", current.toString());
            metadata.put("player_name", player.getName());

            releaseAndTransfer(
                    player,
                    transferService,
                    target,
                    metadata,
                    "Возвращаю на ферму: "
            );
        } catch (NoClassDefFoundError | Exception error) {
            this.plugin.getLogger().warning("Could not return " + player.getName() + " to ranch realm " + target + ": " + error.getMessage());
            player.sendMessage(ChatColor.RED + "Возврат на ферму сейчас недоступен.");
        }
        return true;
    }

    private void releaseAndTransfer(
            Player player,
            ITransferService transferService,
            RealmId target,
            Map<String, String> metadata,
            String successMessage
    ) {
        UUID playerId = player.getUniqueId();
        if (!this.transferringPlayers.add(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "Перенос уже выполняется.");
            return;
        }

        this.profileService.releaseLoaded(playerId).whenComplete((ignored, saveError) ->
                org.bukkit.Bukkit.getScheduler().runTask(this.plugin, () -> {
                    if (!player.isOnline()) {
                        this.transferringPlayers.remove(playerId);
                        return;
                    }
                    if (saveError != null) {
                        this.transferringPlayers.remove(playerId);
                        this.plugin.getLogger().warning("Could not save and release " + player.getName()
                                + " before transfer: " + saveError.getMessage());
                        player.sendMessage(ChatColor.RED + "Не удалось сохранить ресурсы. Перенос отменён.");
                        return;
                    }
                    try {
                        transferService.transfer(playerId, target, metadata);
                        player.sendMessage(ChatColor.YELLOW + successMessage + ChatColor.WHITE + target);
                        scheduleTransferRecovery(player);
                    } catch (RuntimeException transferError) {
                        this.transferringPlayers.remove(playerId);
                        this.plugin.getLogger().warning("Could not transfer " + player.getName()
                                + " to " + target + ": " + transferError.getMessage());
                        player.sendMessage(ChatColor.RED + "Перенос не удался.");
                        this.profileService.load(player);
                    }
                })
        );
    }

    private void scheduleTransferRecovery(Player player) {
        UUID playerId = player.getUniqueId();
        org.bukkit.Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            this.transferringPlayers.remove(playerId);
            if (!this.profileService.isLoaded(playerId)) {
                this.profileService.load(player);
                player.sendMessage(ChatColor.RED + "Перенос не завершился. Повтори позже.");
            }
        }, 100L);
    }

    public boolean isCurrentHuntZone() {
        RealmId current = currentRealmId();
        return current != null && sameRealm(current, configuredRealm());
    }

    private ITransferService ensureTransferService() {
        ITransferService service = ITransferService.get();
        if (service != null) {
            return service;
        }

        ISocketClient socketClient = ISocketClient.get();
        if (socketClient == null) {
            this.plugin.getLogger().warning("TransferService is missing and Core socket client is not available.");
            return null;
        }

        try {
            service = new TransferService(socketClient);
            CoreApi.get().registerService(ITransferService.class, service);
            this.plugin.getLogger().info("Registered missing Core TransferService.");
            return service;
        } catch (RuntimeException error) {
            this.plugin.getLogger().warning("Could not register Core TransferService: " + error.getMessage());
            return ITransferService.get();
        }
    }

    private RealmId parseTarget(String[] args) {
        if (args.length >= 2) {
            try {
                return RealmId.of(args[0], Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (args.length == 1) {
            return RealmId.of(args[0]);
        }
        return configuredRealm();
    }

    private RealmId configuredRealm() {
        String type = this.plugin.getConfig().getString(CONFIG_REALM_TYPE, DEFAULT_REALM_TYPE);
        int id = this.plugin.getConfig().getInt(CONFIG_REALM_ID, DEFAULT_REALM_ID);
        return RealmId.of(type, id);
    }

    private boolean isLocalMode() {
        return "local".equalsIgnoreCase(configuredMode());
    }

    private String configuredMode() {
        return this.plugin.getConfig().getString(CONFIG_MODE, "local");
    }

    private RealmId configuredRanchRealm() {
        String type = this.plugin.getConfig().getString(CONFIG_RANCH_REALM_TYPE, DEFAULT_RANCH_REALM_TYPE);
        int id = this.plugin.getConfig().getInt(CONFIG_RANCH_REALM_ID, DEFAULT_RANCH_REALM_ID);
        return RealmId.of(type, id);
    }

    private Map<String, String> metadataFor(Player player, RealmId target) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("slimes_destination", "hunt_zone");
        metadata.put("target_realm", target.toString());
        metadata.put("player_name", player.getName());
        metadata.put("return_reason", "ranch");

        RealmId current = currentRealmId();
        if (current != null) {
            metadata.put("return_realm", current.toString());
        }
        return metadata;
    }

    private RealmId currentRealmId() {
        try {
            IRealmService realmService = IRealmService.get();
            if (realmService == null) {
                return null;
            }
            RealmInfo info = realmService.getCurrentRealmInfo();
            return info == null ? null : info.getRealmId();
        } catch (NoClassDefFoundError coreApiMissing) {
            return null;
        } catch (RuntimeException error) {
            this.plugin.getLogger().warning(
                    "Cristalix Core call failed: " + error);
            return null;
        }
    }

    private static boolean sameRealm(RealmId first, RealmId second) {
        return first != null && second != null
                && first.getTypeName().equalsIgnoreCase(second.getTypeName())
                && first.getId() == second.getId();
    }
}
