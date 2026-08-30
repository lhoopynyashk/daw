package dev.lhoopy.crafting;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.world.RanchWorldService;
import org.bukkit.Bukkit;
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

public final class FarmerTableNpcService implements PluginService, Listener {
    private static final String NPC_TAG = "slimes:farmer_table";
    private static final String OWNER_TAG_PREFIX = "slimes:farmer_owner:";
    private static final String DEFAULT_NAME = "§aСтол фермера";

    private final SlimesPlugin plugin;
    private final RanchWorldService ranchWorldService;
    private final FarmerTableMenuService menuService;

    public FarmerTableNpcService(SlimesPlugin plugin, RanchWorldService ranchWorldService,
                                 FarmerTableMenuService menuService) {
        this.plugin = plugin;
        this.ranchWorldService = ranchWorldService;
        this.menuService = menuService;
    }

    @Override
    public void enable() {
        if (!this.plugin.getConfig().getBoolean("farmer-table.npc.enabled", true)) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleEnsure(player, 20L);
        }
        this.plugin.getLogger().info("Farmer table NPC enabled");
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
        this.menuService.open(event.getPlayer());
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
        kept.setCustomName(this.plugin.getConfig().getString("farmer-table.npc.name", DEFAULT_NAME));
        kept.setCustomNameVisible(true);
        kept.addScoreboardTag(NPC_TAG);
        kept.addScoreboardTag(ownerTag);
        kept.setAI(false);
        kept.setInvulnerable(true);
        kept.setSilent(true);
        kept.setRemoveWhenFarAway(false);
        kept.setProfession(Villager.Profession.FARMER);
    }

    private Location npcLocation(World world) {
        return new Location(
                world,
                this.plugin.getConfig().getDouble("farmer-table.npc.x", -8.5D),
                this.plugin.getConfig().getDouble("farmer-table.npc.y", 91.0D),
                this.plugin.getConfig().getDouble("farmer-table.npc.z", 0.5D),
                (float) this.plugin.getConfig().getDouble("farmer-table.npc.yaw", 90.0D),
                0.0F
        );
    }
}
