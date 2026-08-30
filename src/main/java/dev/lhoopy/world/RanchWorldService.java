package dev.lhoopy.world;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RanchWorldService implements PluginService, Listener {
    private static final String CONFIG_ENABLED = "ranch-worlds.enabled";
    private static final String CONFIG_TEMPLATE_PATH = "ranch-worlds.template-path";
    private static final String CONFIG_RUNTIME_PREFIX = "ranch-worlds.runtime-prefix";
    private static final String CONFIG_UNLOAD_DELAY_SECONDS = "ranch-worlds.unload-delay-seconds";
    private static final String CONFIG_DELETE_ON_UNLOAD = "ranch-worlds.delete-runtime-on-unload";
    private static final String CONFIG_BUILD_FALLBACK_PLATFORM = "ranch-worlds.build-fallback-platform";
    private static final int FALLBACK_Y = 90;
    private static final int FALLBACK_RADIUS = 15;

    private final SlimesPlugin plugin;
    private final Map<UUID, String> playerWorldNames = new HashMap<>();
    private final Map<String, UUID> worldOwners = new HashMap<>();
    private final Set<String> managedWorldNames = new HashSet<>();
    private WorldOptimizationService optimizationService;

    public RanchWorldService(SlimesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        this.optimizationService = new WorldOptimizationService(this.plugin, this);
        this.optimizationService.enable();
        if (isEnabled()) {
            this.plugin.getLogger().info("Ranch runtime worlds enabled");
        } else {
            this.plugin.getLogger().info("Ranch runtime worlds disabled");
        }
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        if (this.optimizationService != null) {
            this.optimizationService.shutdown();
            this.optimizationService = null;
        }
        for (String worldName : new HashSet<>(this.managedWorldNames)) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Bukkit.unloadWorld(world, false);
            }
        }
        this.managedWorldNames.clear();
        this.playerWorldNames.clear();
        this.worldOwners.clear();
        this.plugin.getLogger().info("Ranch runtime worlds disabled");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        scheduleUnloadIfRanchWorld(event.getPlayer().getWorld());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        scheduleUnloadIfRanchWorld(event.getFrom());
        World current = event.getPlayer().getWorld();
        if (isRanchWorld(current)) {
            tuneWorld(current);
        }
    }

    public boolean isEnabled() {
        return this.plugin.getConfig().getBoolean(CONFIG_ENABLED, true);
    }

    public Location openRanch(Player player) {
        World world = loadWorld(player);
        Location spawn = ranchSpawn(world);
        player.teleport(spawn);
        return spawn;
    }

    public Location openRanch(Player visitor, Player owner) {
        World world = loadWorld(owner.getUniqueId());
        Location spawn = ranchSpawn(world);
        visitor.teleport(spawn);
        return spawn;
    }

    public Location prepareRanchSpawn(Player player) {
        return ranchSpawn(loadWorld(player));
    }

    public boolean isRanchWorld(World world) {
        return world != null && this.managedWorldNames.contains(world.getName());
    }

    public UUID getOwnerId(World world) {
        return world == null ? null : this.worldOwners.get(world.getName());
    }

    public Location penNpcLocation(World world) {
        return new Location(world, 8.5D, FALLBACK_Y + 1.0D, 0.5D, 270.0F, 0.0F);
    }

    private Location ranchSpawn(World world) {
        return new Location(world, 0.5D, FALLBACK_Y + 1.0D, 0.5D, 180.0F, 0.0F);
    }

    public void scheduleUnloadIfRanchWorld(World world) {
        if (!isRanchWorld(world)) {
            return;
        }
        String worldName = world.getName();
        long delayTicks = Math.max(1L, this.plugin.getConfig().getLong(CONFIG_UNLOAD_DELAY_SECONDS, 60L)) * 20L;
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> unloadIfEmpty(worldName), delayTicks);
    }

    private World loadWorld(Player player) {
        return loadWorld(player.getUniqueId());
    }

    private World loadWorld(UUID ownerId) {
        String worldName = worldName(ownerId);
        this.playerWorldNames.put(ownerId, worldName);
        this.worldOwners.put(worldName, ownerId);
        this.managedWorldNames.add(worldName);

        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            return loaded;
        }

        prepareWorldFolder(worldName);
        WorldCreator creator = new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .type(WorldType.FLAT)
                .generateStructures(false)
                .generator(new VoidChunkGenerator());
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            throw new IllegalStateException("Could not create ranch world " + worldName);
        }

        tuneWorld(world);
        if (this.plugin.getConfig().getBoolean(CONFIG_BUILD_FALLBACK_PLATFORM, true) && !hasTemplate()) {
            buildFallbackPlatform(world);
        }
        return world;
    }

    private String worldName(UUID playerId) {
        String prefix = this.plugin.getConfig().getString(CONFIG_RUNTIME_PREFIX, "ranch_");
        return prefix + playerId.toString().replace("-", "");
    }

    private void prepareWorldFolder(String worldName) {
        File target = new File(Bukkit.getWorldContainer(), worldName);
        if (target.exists()) {
            return;
        }

        File template = templateFolder();
        if (!template.isDirectory()) {
            return;
        }

        try {
            copyTemplate(template.toPath(), target.toPath());
            this.plugin.getLogger().info("Copied ranch template for " + worldName);
        } catch (IOException exception) {
            this.plugin.getLogger().warning("Could not copy ranch template for " + worldName + ": " + exception.getMessage());
        }
    }

    private boolean hasTemplate() {
        return templateFolder().isDirectory();
    }

    private File templateFolder() {
        String relativePath = this.plugin.getConfig().getString(CONFIG_TEMPLATE_PATH, "templates/ranch_template");
        return new File(this.plugin.getDataFolder(), relativePath);
    }

    private void copyTemplate(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (fileName.equalsIgnoreCase("uid.dat") || fileName.equalsIgnoreCase("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void tuneWorld(World world) {
        if (this.optimizationService != null) {
            this.optimizationService.tune(world);
            return;
        }
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");
        world.setGameRuleValue("randomTickSpeed", "0");
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
    }

    private void buildFallbackPlatform(World world) {
        for (int x = -FALLBACK_RADIUS; x <= FALLBACK_RADIUS; x++) {
            for (int z = -FALLBACK_RADIUS; z <= FALLBACK_RADIUS; z++) {
                world.getBlockAt(x, FALLBACK_Y - 1, z).setType(Material.GRASS);
                world.getBlockAt(x, FALLBACK_Y, z).setType(Material.AIR);
                world.getBlockAt(x, FALLBACK_Y + 1, z).setType(Material.AIR);
                if (Math.abs(x) == FALLBACK_RADIUS || Math.abs(z) == FALLBACK_RADIUS) {
                    world.getBlockAt(x, FALLBACK_Y, z).setType(Material.FENCE);
                }
            }
        }
        world.getBlockAt(0, FALLBACK_Y, 0).setType(Material.TORCH);
        world.getBlockAt(5, FALLBACK_Y, 5).setType(Material.CHEST);
        world.getBlockAt(-5, FALLBACK_Y, -5).setType(Material.HAY_BLOCK);
    }

    private void unloadIfEmpty(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null || !world.getPlayers().isEmpty()) {
            return;
        }
        Bukkit.unloadWorld(world, false);
        if (this.plugin.getConfig().getBoolean(CONFIG_DELETE_ON_UNLOAD, false)) {
            deleteWorldFolder(new File(Bukkit.getWorldContainer(), worldName));
        }
        this.plugin.getLogger().info(ChatColor.stripColor("Unloaded empty ranch world " + worldName));
    }

    private void deleteWorldFolder(File folder) {
        if (!folder.isDirectory()) {
            return;
        }
        try {
            Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            this.plugin.getLogger().warning("Could not delete ranch world folder " + folder.getName() + ": " + exception.getMessage());
        }
    }
}
