package dev.lhoopy.slime;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.SlimeDef;
import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.economy.EconomyService;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.profile.ProfileService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class SlimeService implements Listener {
    private final SlimesPlugin plugin;
    private final SlimeRuntimeRegistry registry = new SlimeRuntimeRegistry();
    private final SlimeSpawnService spawnService;
    private final PacketSlimeService packetSlimeService;
    private final SlimeFeedingService feedingService;
    private final SlimeCaptureService captureService;
    private EconomyService economyService;

    public SlimeService(SlimesPlugin plugin, ContentRegistry contentRegistry, EnginexHuntBridge huntBridge, ProfileService profileService) {
        this.plugin = plugin;
        this.spawnService = new SlimeSpawnService(contentRegistry, this.registry);
        this.packetSlimeService = new PacketSlimeService(contentRegistry);
        this.feedingService = new SlimeFeedingService(plugin, this.registry);
        this.captureService = new SlimeCaptureService(contentRegistry, huntBridge, profileService, this.registry, this.packetSlimeService);
    }

    public void setEconomyService(EconomyService economyService) {
        this.economyService = economyService;
    }

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        this.packetSlimeService.enable(this.plugin);
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        this.packetSlimeService.shutdown();
        this.registry.shutdown();
    }

    public void giveVacuum(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }

        Player player = (Player) sender;
        player.getInventory().addItem(SlimeVacuumItem.create());
        player.sendMessage(ChatColor.GREEN + "Ты получил сосалку.");
    }

    public boolean ensureVacuum(Player player) {
        return SlimeVacuumItem.ensureInInventory(player);
    }

    public void giveFavoriteFood(Player sender, String[] args) {
        this.spawnService.giveFavoriteFood(sender, args);
    }

    public void spawn(Player sender, String[] args) {
        this.spawnPacket(sender, args);
    }

    public void spawnBukkit(Player sender, String[] args) {
        this.spawnService.spawn(sender, args);
    }

    public void spawnPacket(Player sender, String[] args) {
        this.packetSlimeService.handleCommand(sender, args);
    }

    public void clearPacketSlimes(Player player) {
        this.packetSlimeService.clearForViewer(player);
    }

    public void clearPenVisualSlimes(Player player) {
        this.packetSlimeService.clearPenSlimes(player);
    }

    public void spawnPenVisualSlime(Player player, SlimeDef definition, Location location, int penIndex) {
        this.packetSlimeService.spawnPenSlime(player, definition, location, penIndex);
    }

    public void spawnLocationSlime(Player player, SlimeDef definition, Location location) {
        this.packetSlimeService.spawnHuntSlime(player, definition, location);
    }

    public int countLocationSlimes(Player player) {
        return this.packetSlimeService.countHuntSlimes(player);
    }

    public void clearLocationSlimes(Player player) {
        this.packetSlimeService.clearHuntSlimes(player);
    }

    public PenPacketSlimeTarget findNearestPenVisualSlime(Player player, double range) {
        PacketSlime packetSlime = this.packetSlimeService.findNearestPenSlime(player, range);
        if (packetSlime == null) {
            return null;
        }
        return new PenPacketSlimeTarget(packetSlime.getDefinition(), packetSlime.getPenIndex());
    }

    public void completeCapture(Player player, SlimeCaptureTarget target, int hits, int total) {
        this.captureService.completeCapture(player, target, hits, total);
    }

    public List<SlimeDef> findCapturedSlimes(Player player) {
        return this.captureService.findCapturedSlimes(player);
    }

    public boolean removeCapturedSlime(Player player, String slimeId) {
        return this.captureService.removeCapturedSlime(player, slimeId);
    }

    public void clearCapturedSlimes(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }

        Player player = (Player) sender;
        int removed = this.captureService.clearCapturedSlimes(player);
        clearPenVisualSlimes(player);
        player.sendMessage(ChatColor.GREEN + "Пойманные слаймы очищены: " + ChatColor.WHITE + removed);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPacketSlimes(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        clearPacketSlimes(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        clearPacketSlimes(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Slime)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getItemInHand();
        Slime slime = (Slime) event.getRightClicked();
        RuntimeSlime runtime = this.registry.get(slime.getUniqueId());
        if (runtime == null) {
            if (SlimeVacuumItem.isVacuum(itemInHand)) {
                player.sendMessage(ChatColor.YELLOW + "Этот слайм не из системы.");
            }
            return;
        }

        if (SlimeVacuumItem.isVacuum(itemInHand)) {
            this.captureService.startCapture(player, slime, runtime);
            return;
        }

        this.feedingService.handleFoodClick(player, slime, runtime, itemInHand);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!SlimeVacuumItem.isVacuum(player.getItemInHand())) {
            if (this.packetSlimeService.handleFoodUse(this.plugin, player, player.getItemInHand())) {
                event.setCancelled(true);
            }
            return;
        }

        if (!SlimeVacuumItem.isVacuum(player.getItemInHand())) {
            return;
        }

        event.setCancelled(true);
        PacketSlime packetTarget = this.packetSlimeService.findNearest(player, true);
        if (packetTarget != null) {
            this.captureService.startCapture(player, packetTarget);
            return;
        }

        RuntimeSlimeTarget target = this.registry.findNearest(player, true);
        if (target != null) {
            this.captureService.startCapture(player, target.slime, target.runtime);
            return;
        }

        PacketSlime nearestPacket = this.packetSlimeService.findNearest(player, false);
        if (nearestPacket != null) {
            player.sendMessage(ChatColor.RED + "Ближайший слайм не заинтересован.");
            player.sendMessage(ChatColor.GRAY + "Дай ему любимую еду: " + nearestPacket.getDefinition().getFavoriteFood().name());
            return;
        }

        RuntimeSlimeTarget nearest = this.registry.findNearest(player, false);
        if (nearest == null) {
            if (this.economyService != null) {
                this.economyService.handleVacuumUse(player);
                return;
            }
            player.sendMessage(ChatColor.GRAY + "Рядом нет слаймов для поимки.");
            return;
        }

        player.sendMessage(ChatColor.RED + "Ближайший слайм не заинтересован.");
        player.sendMessage(ChatColor.GRAY + "Дай ему любимую еду: " + nearest.runtime.definition.getFavoriteFood().name());
    }
}
