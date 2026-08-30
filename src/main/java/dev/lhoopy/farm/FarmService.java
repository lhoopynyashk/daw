package dev.lhoopy.farm;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.location.LocationRealmService;
import dev.lhoopy.world.RanchWorldService;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import gg.cristalix.wada.transfer.ModTransfer;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import ru.cristalix.core.realm.IRealmService;
import ru.cristalix.core.realm.RealmId;
import ru.cristalix.core.realm.RealmInfo;

import java.util.UUID;


public final class FarmService implements PluginService, Listener {
    private static final String CONFIG_HUNT_REALM_TYPE = "hunt-zone.realm-type";
    private static final String CONFIG_HUNT_REALM_ID = "hunt-zone.realm-id";
    private final SlimesPlugin plugin;
    private final ProfileService profileService;
    private final RanchWorldService ranchWorldService;
    private final EnginexHuntBridge enginexHuntBridge;
    private final FarmWorldService farmWorldService;
    private final CropGrowthService cropGrowthService = new CropGrowthService();
    private final WateringService wateringService;
    private final FarmCommand farmCommand;
    private final FarmPlotWorldService farmPlotWorldService;
    private final FarmPlotActions farmPlotActions;
    private final FarmPlotMenuService farmPlotMenuService;
    private final FarmTravelService farmTravelService;
    private final RanchJoinFlow ranchJoinFlow;
    private final LocationRealmService locationRealmService;
    private BukkitTask plotRefreshTask;

    public FarmService(SlimesPlugin plugin, RanchWorldService ranchWorldService, EnginexHuntBridge enginexHuntBridge, ContentRegistry contentRegistry, ProfileService profileService, LocationRealmService locationRealmService) {
        this.plugin = plugin;
        this.profileService = profileService;
        this.ranchWorldService = ranchWorldService;
        this.enginexHuntBridge = enginexHuntBridge;
        this.locationRealmService = locationRealmService;
        this.farmWorldService = new FarmWorldService(plugin, ranchWorldService);
        this.wateringService = new WateringService(contentRegistry, this.cropGrowthService);
        this.farmCommand = new FarmCommand(contentRegistry, profileService, this.cropGrowthService, this.wateringService);
        this.farmPlotActions = new FarmPlotActions(contentRegistry, profileService, this.cropGrowthService, this.wateringService);
        this.farmPlotMenuService = new FarmPlotMenuService(contentRegistry, this.cropGrowthService, enginexHuntBridge);
        this.farmPlotWorldService = new FarmPlotWorldService(
                plugin,
                contentRegistry,
                profileService,
                this.cropGrowthService,
                this.farmPlotMenuService
        );
        this.farmTravelService = new FarmTravelService(ranchWorldService, this.farmWorldService, this::isPassiveRealm);
        this.ranchJoinFlow = new RanchJoinFlow(plugin, ranchWorldService, enginexHuntBridge, profileService, this.farmWorldService);
    }

    @Override
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        this.farmPlotWorldService.loadConfig();
        ModTransfer.registerChannel(FarmPlotMenuService.ACTION_CHANNEL, (player, transfer) -> {
            String action = transfer.readString();
            String plotId = transfer.readString();
            String argument = transfer.readString();
            Bukkit.getScheduler().runTask(this.plugin, () -> handlePlotMenuAction(player, action, plotId, argument));
        });
        if (isPassiveRealm()) {
            this.plugin.getLogger().info("Farm service is passive on hunting location realm");
            return;
        }
        this.farmWorldService.cleanupLoadedPenNpcs();
        if (!this.ranchWorldService.isEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ensureFarm(player);
            }
        } else {
            this.plotRefreshTask = Bukkit.getScheduler().runTaskTimer(
                    this.plugin,
                    this::refreshOnlineRanchPlots,
                    20L,
                    20L
            );
        }
        this.plugin.getLogger().info("Farm service enabled");
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        if (this.plotRefreshTask != null) {
            this.plotRefreshTask.cancel();
            this.plotRefreshTask = null;
        }
        this.farmPlotWorldService.clearRuntimeState();
        this.farmWorldService.clearRuntimeState();
        this.plugin.getLogger().info("Farm service disabled");
    }

    @EventHandler
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        if (isPassiveRealm() || !this.ranchWorldService.isEnabled()) {
            return;
        }
        try {
            event.setSpawnLocation(this.ranchWorldService.prepareRanchSpawn(event.getPlayer()));
            Player player = event.getPlayer();
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (player.isOnline()) {
                    this.enginexHuntBridge.showLoading(player, "Загрузка ранчо", 10);
                }
            });
        } catch (RuntimeException exception) {
            this.plugin.getLogger().warning("Could not prepare initial ranch spawn for "
                    + event.getPlayer().getName() + ": " + exception.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isPassiveRealm()) {
            return;
        }
        if (this.ranchWorldService.isEnabled()) {
            startRanchJoinFlow(event.getPlayer());
            schedulePlotRender(event.getPlayer(), 60L);
            return;
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Location ignored = ensureFarm(event.getPlayer());
            renderFarmPlots(event.getPlayer());
        }, 20L);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (isPassiveRealm()) {
            return;
        }
        this.farmWorldService.cleanupChunkPenNpcs(event.getChunk().getEntities());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        schedulePlotRender(event.getPlayer(), 10L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isPassiveRealm()) {
            return;
        }
        if (!this.ranchWorldService.isEnabled()) {
            this.farmPlotWorldService.handleInteract(event, true);
            return;
        }
        UUID ownerId = this.ranchWorldService.getOwnerId(event.getPlayer().getWorld());
        if (ownerId == null) {
            return;
        }
        this.farmPlotWorldService.handleInteract(event, ownerId.equals(event.getPlayer().getUniqueId()));
    }

    public void teleportHome(CommandSender sender) {
        this.farmTravelService.teleportHome(sender);
    }

    public void visit(CommandSender sender, String[] args) {
        this.farmTravelService.visit(sender, args);
    }

    public void handleCommand(CommandSender sender, String[] args) {
        this.farmCommand.handle(sender, args);
    }

    public Location ensureFarm(Player player) {
        Location location = this.farmTravelService.ensureSharedFarm(player);
        renderFarmPlots(player, true);
        return location;
    }

    private void startRanchJoinFlow(Player player) {
        this.ranchJoinFlow.start(player);
    }

    private void schedulePlotRender(Player player, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> renderFarmPlots(player, true), delayTicks);
    }

    private void renderFarmPlots(Player player) {
        renderFarmPlots(player, true);
    }

    private void renderFarmPlots(Player player, boolean force) {
        if (player == null || !player.isOnline() || isPassiveRealm()) {
            return;
        }
        if (!this.ranchWorldService.isRanchWorld(player.getWorld()) && this.ranchWorldService.isEnabled()) {
            return;
        }
        if (!this.ranchWorldService.isEnabled()) {
            this.farmWorldService.ensureSharedFarm(player);
        } else {
            UUID ownerId = this.ranchWorldService.getOwnerId(player.getWorld());
            if (ownerId == null || !ownerId.equals(player.getUniqueId())) {
                return;
            }
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            return;
        }
        this.farmPlotWorldService.render(player, profile, force);
    }

    private void refreshOnlineRanchPlots() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            renderFarmPlots(player, false);
        }
    }

    private void handlePlotMenuAction(Player player, String action, String plotId, String argument) {
        if (!canModifyPlots(player) || !this.farmPlotWorldService.hasPlot(plotId)) {
            player.sendMessage(ChatColor.RED + "Эта грядка вам недоступна.");
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            return;
        }
        profile.getFarmData().getOrCreatePlot(plotId);
        switch (action) {
            case "plant":
                this.farmPlotActions.plant(player, profile, plotId, argument);
                break;
            case "water":
                if (!player.getInventory().contains(Material.WATER_BUCKET)) {
                    player.sendMessage(ChatColor.YELLOW + "Для полива нужно ведро воды.");
                    break;
                }
                this.farmPlotActions.water(player, profile, plotId);
                player.playSound(player.getLocation(), "random.splash", 0.65F, 1.4F);
                break;
            case "harvest":
                this.farmPlotActions.harvest(player, profile, plotId);
                player.playSound(player.getLocation(), "random.pop", 0.8F, 1.25F);
                break;
            case "type":
                this.farmPlotActions.changePlotType(player, profile, plotId, argument);
                break;
            default:
                return;
        }
        renderFarmPlots(player, true);
        this.farmPlotMenuService.open(player, profile, plotId);
    }

    private boolean canModifyPlots(Player player) {
        if (!this.ranchWorldService.isEnabled()) {
            return true;
        }
        UUID ownerId = this.ranchWorldService.getOwnerId(player.getWorld());
        return ownerId != null && ownerId.equals(player.getUniqueId());
    }

    private boolean isCurrentHuntZone() {
        RealmId current = currentRealmId();
        if (current == null) {
            return false;
        }
        String huntType = this.plugin.getConfig().getString(CONFIG_HUNT_REALM_TYPE, "HUNT");
        int huntId = this.plugin.getConfig().getInt(CONFIG_HUNT_REALM_ID, 1);
        return current.getTypeName().equalsIgnoreCase(huntType) && current.getId() == huntId;
    }

    private boolean isPassiveRealm() {
        return isCurrentHuntZone() || this.locationRealmService.isCurrentLocationRealm();
    }

    private RealmId currentRealmId() {
        try {
            IRealmService realmService = IRealmService.get();
            if (realmService == null) {
                return null;
            }
            RealmInfo info = realmService.getCurrentRealmInfo();
            return info == null ? null : info.getRealmId();
        } catch (NoClassDefFoundError | Exception ignored) {
            return null;
        }
    }

}
