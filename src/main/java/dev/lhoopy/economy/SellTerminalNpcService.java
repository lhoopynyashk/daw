package dev.lhoopy.economy;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.world.RanchWorldService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public final class SellTerminalNpcService implements PluginService, Listener {
    private static final String NPC_TAG = "slimes:sell_terminal";
    private static final String OWNER_TAG_PREFIX = "slimes:sell_owner:";
    private static final String DEFAULT_NAME = "&6Терминал продажи";

    private final SlimesPlugin plugin;
    private final RanchWorldService ranchWorldService;
    private final EconomyService economyService;

    public SellTerminalNpcService(SlimesPlugin plugin, RanchWorldService ranchWorldService,
                                  EconomyService economyService) {
        this.plugin = plugin;
        this.ranchWorldService = ranchWorldService;
        this.economyService = economyService;
    }

    @Override
    public void enable() {
        if (!this.plugin.getConfig().getBoolean("sell-terminal.npc.enabled", true)) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleEnsure(player, 20L);
        }
        this.plugin.getLogger().info("Sell terminal NPC enabled");
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleEnsure(event.getPlayer(), 60L);
    }

    @EventHandler
    public void onWorldChanged(PlayerChangedWorldEvent event) {
        scheduleEnsure(event.getPlayer(), 20L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getScoreboardTags().contains(NPC_TAG)) {
            return;
        }
        event.setCancelled(true);
        this.economyService.handleSellTerminalCommand(event.getPlayer(), new String[0]);
    }

    private void scheduleEnsure(Player player, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
                ensureNpc(player);
            }
        }, delayTicks);
    }

    private void ensureNpc(Player player) {
        World world = player.getWorld();
        if (!this.ranchWorldService.isRanchWorld(world)) {
            return;
        }
        UUID ownerId = this.ranchWorldService.getOwnerId(world);
        if (ownerId == null) {
            return;
        }

        Location target = npcLocation(world);
        String ownerTag = OWNER_TAG_PREFIX + ownerId;
        Villager kept = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Villager) || !entity.getScoreboardTags().contains(NPC_TAG)) {
                continue;
            }
            if (!entity.getScoreboardTags().contains(ownerTag)) {
                entity.remove();
                continue;
            }
            double distance = entity.getLocation().distanceSquared(target);
            if (distance < nearestDistance) {
                if (kept != null) {
                    kept.remove();
                }
                kept = (Villager) entity;
                nearestDistance = distance;
            } else {
                entity.remove();
            }
        }

        if (kept == null) {
            kept = (Villager) world.spawnEntity(target, EntityType.VILLAGER);
        } else {
            kept.teleport(target);
        }
        String configuredName = this.plugin.getConfig().getString("sell-terminal.npc.name", DEFAULT_NAME);
        kept.setCustomName(ChatColor.translateAlternateColorCodes('&', configuredName));
        kept.setCustomNameVisible(true);
        kept.addScoreboardTag(NPC_TAG);
        kept.addScoreboardTag(ownerTag);
        kept.setAI(false);
        kept.setInvulnerable(true);
        kept.setSilent(true);
        kept.setRemoveWhenFarAway(false);
        kept.setProfession(Villager.Profession.LIBRARIAN);
    }

    private Location npcLocation(World world) {
        return new Location(
                world,
                this.plugin.getConfig().getDouble("sell-terminal.npc.x", 0.5D),
                this.plugin.getConfig().getDouble("sell-terminal.npc.y", 91.0D),
                this.plugin.getConfig().getDouble("sell-terminal.npc.z", 8.5D),
                (float) this.plugin.getConfig().getDouble("sell-terminal.npc.yaw", 180.0D),
                0.0F
        );
    }
}
