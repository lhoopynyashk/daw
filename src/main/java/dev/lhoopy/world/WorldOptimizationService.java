package dev.lhoopy.world;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public final class WorldOptimizationService implements PluginService, Listener {
    private static final String CONFIG_ENABLED = "ranch-worlds.optimization.enabled";
    private static final String CONFIG_CANCEL_NATURAL_SPAWNS = "ranch-worlds.optimization.cancel-natural-spawns";
    private static final String CONFIG_CLEAR_DROPPED_ITEMS = "ranch-worlds.optimization.clear-dropped-items";
    private static final String CONFIG_CLEAR_UNUSED_ENTITIES = "ranch-worlds.optimization.clear-unused-entities";
    private static final String CONFIG_CLEANUP_INTERVAL_SECONDS = "ranch-worlds.optimization.cleanup-interval-seconds";

    private final SlimesPlugin plugin;
    private final RanchWorldService ranchWorldService;
    private int cleanupTaskId = -1;

    public WorldOptimizationService(SlimesPlugin plugin, RanchWorldService ranchWorldService) {
        this.plugin = plugin;
        this.ranchWorldService = ranchWorldService;
    }

    @Override
    public void enable() {
        if (!isEnabled()) {
            this.plugin.getLogger().info("Ranch world optimizer disabled");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        long intervalTicks = Math.max(5L, this.plugin.getConfig().getLong(CONFIG_CLEANUP_INTERVAL_SECONDS, 30L)) * 20L;
        this.cleanupTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, this::cleanupRanchWorlds, intervalTicks, intervalTicks).getTaskId();
        this.plugin.getLogger().info("Ranch world optimizer enabled");
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        if (this.cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.cleanupTaskId);
            this.cleanupTaskId = -1;
        }
        this.plugin.getLogger().info("Ranch world optimizer disabled");
    }

    public void tune(World world) {
        if (world == null || !isEnabled()) {
            return;
        }
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setPVP(false);
        world.setSpawnFlags(false, false);
        world.setMonsterSpawnLimit(0);
        world.setAnimalSpawnLimit(0);
        world.setWaterAnimalSpawnLimit(0);
        world.setAmbientSpawnLimit(0);
        world.setTicksPerMonsterSpawns(Integer.MAX_VALUE);
        world.setTicksPerAnimalSpawns(Integer.MAX_VALUE);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");
        world.setGameRuleValue("doFireTick", "false");
        world.setGameRuleValue("mobGriefing", "false");
        world.setGameRuleValue("doEntityDrops", "false");
        world.setGameRuleValue("randomTickSpeed", "0");
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!this.plugin.getConfig().getBoolean(CONFIG_CANCEL_NATURAL_SPAWNS, true)) {
            return;
        }
        if (!this.ranchWorldService.isRanchWorld(event.getLocation().getWorld())) {
            return;
        }
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!this.plugin.getConfig().getBoolean(CONFIG_CLEAR_DROPPED_ITEMS, true)) {
            return;
        }
        if (this.ranchWorldService.isRanchWorld(event.getLocation().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (this.ranchWorldService.isRanchWorld(event.getWorld()) && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        if (this.ranchWorldService.isRanchWorld(event.getWorld()) && event.toThunderState()) {
            event.setCancelled(true);
        }
    }

    private void cleanupRanchWorlds() {
        if (!this.plugin.getConfig().getBoolean(CONFIG_CLEAR_UNUSED_ENTITIES, true)) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            if (!this.ranchWorldService.isRanchWorld(world)) {
                continue;
            }
            tune(world);
            int removed = 0;
            for (Entity entity : world.getEntities()) {
                if (!shouldRemove(entity)) {
                    continue;
                }
                entity.remove();
                removed++;
            }
            if (removed > 0) {
                this.plugin.getLogger().fine("Removed unused ranch entities in " + world.getName() + ": " + removed);
            }
        }
    }

    private boolean shouldRemove(Entity entity) {
        EntityType type = entity.getType();
        return type == EntityType.DROPPED_ITEM
                || type == EntityType.EXPERIENCE_ORB
                || type == EntityType.ARROW
                || type == EntityType.SPLASH_POTION
                || type == EntityType.FIREBALL
                || type == EntityType.SMALL_FIREBALL
                || type == EntityType.PRIMED_TNT
                || type == EntityType.FALLING_BLOCK
                || type == EntityType.BOAT
                || type == EntityType.MINECART;
    }

    private boolean isEnabled() {
        return this.plugin.getConfig().getBoolean(CONFIG_ENABLED, true);
    }
}
