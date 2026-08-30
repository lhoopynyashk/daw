package dev.lhoopy.pen;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.slime.SlimeService;
import dev.lhoopy.slime.SlimeVacuumItem;
import gg.cristalix.wada.transfer.ModTransfer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class PenService implements PluginService, Listener {
    public static final String NPC_TAG = "slimes:pen_npc";
    public static final String NPC_NAME = ChatColor.GREEN + "Загон слаймов";
    public static final String NPC_PLAIN_NAME = "Загон слаймов";
    public static final String PEN_OPEN_CHANNEL = "slimehunt:pen_open";
    public static final String PEN_MOVE_CHANNEL = "slimehunt:pen_move";
    public static final String PEN_REMOVE_CHANNEL = "slimehunt:pen_remove";
    public static final String PEN_VISUAL_CHANNEL = "slimehunt:pen_visual";

    private final SlimesPlugin plugin;
    private final PenMenuBridge menuBridge;
    private final PenFeedingCommand feedingCommand;
    private final PenVisualService visualService;
    private final PenFeedingService feedingService;

    public PenService(SlimesPlugin plugin, EnginexHuntBridge enginexHuntBridge, ContentRegistry contentRegistry,
                      SlimeService slimeWorldService, ProfileService profileService, PenStyleCatalog styleCatalog) {
        this.plugin = plugin;
        this.menuBridge = new PenMenuBridge(plugin, enginexHuntBridge, contentRegistry, slimeWorldService, profileService, styleCatalog);
        this.visualService = new PenVisualService(plugin, contentRegistry, profileService, styleCatalog);
        long fedDurationMillis = Math.max(30L, plugin.getConfig().getLong("pen.feeding.fed-duration-seconds", 300L)) * 1000L;
        this.feedingCommand = new PenFeedingCommand(contentRegistry, profileService, fedDurationMillis);
        this.feedingService = new PenFeedingService(plugin, contentRegistry, profileService, this.visualService,
                styleCatalog, fedDurationMillis);
    }

    @Override
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        ModTransfer.registerChannel(PEN_MOVE_CHANNEL, (player, transfer) -> {
            String slimeId = transfer.readString();
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                this.menuBridge.moveCapturedSlime(player, slimeId);
                refreshPenVisuals(player);
            });
        });
        ModTransfer.registerChannel(PEN_REMOVE_CHANNEL, (player, transfer) -> {
            int index = transfer.readInt();
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                this.menuBridge.removePenSlime(player, index);
                refreshPenVisuals(player);
            });
        });
        this.plugin.getLogger().info("Pen service enabled");
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        this.plugin.getLogger().info("Pen service disabled");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.visualService.scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.visualService.clear(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        this.visualService.clear(event.getPlayer());
        this.visualService.scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!isPenNpc(event.getRightClicked())) {
            return;
        }

        event.setCancelled(true);
        openPenMenu(event.getPlayer());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (isPenNpc(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (isPenNpc(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR || SlimeVacuumItem.isVacuum(item)) {
            return;
        }

        if (this.feedingService.feedNearest(player, item)) {
            event.setCancelled(true);
        }
    }

    public void openPenMenu(Player player) {
        refreshPenVisuals(player);
        this.menuBridge.open(player);
    }

    public void handleFeedCommand(org.bukkit.command.CommandSender sender, String[] args) {
        this.feedingCommand.handle(sender, args);
    }

    public void refreshPenVisuals(Player player) {
        this.visualService.refresh(player);
    }

    private boolean isPenNpc(Entity entity) {
        return PenNpcMatcher.isPenNpc(entity);
    }
}
