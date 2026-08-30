package dev.lhoopy.farm;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.pen.PenService;
import dev.lhoopy.world.RanchWorldService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FarmWorldService {
    private static final int FARM_SPACING = 96;
    private static final int FARM_GRID_WIDTH = 100;
    private static final int FARM_ORIGIN_X = 1000;
    private static final int FARM_ORIGIN_Z = 1000;
    private static final int FARM_Y = 90;
    private static final int FARM_RADIUS = 15;
    private static final String FARM_NPC_TAG_PREFIX = "slimes:farm_pen:";

    private final SlimesPlugin plugin;
    private final RanchWorldService ranchWorldService;
    private final Set<UUID> preparedFarms = new HashSet<>();

    public FarmWorldService(SlimesPlugin plugin, RanchWorldService ranchWorldService) {
        this.plugin = plugin;
        this.ranchWorldService = ranchWorldService;
    }

    public void clearRuntimeState() {
        this.preparedFarms.clear();
    }

    public Location ensureSharedFarm(Player player) {
        World world = getMainWorld();
        FarmLayout layout = layoutFor(player.getUniqueId(), world);
        if (this.preparedFarms.add(player.getUniqueId())) {
            preparePlatform(layout);
        }
        ensurePenNpc(player, layout);
        return layout.spawn;
    }

    public void ensureRanchPenNpc(Player player, World world) {
        ensurePenNpc(player, layoutForRanchWorld(world));
    }

    public void cleanupLoadedPenNpcs() {
        int removedLegacy = 0;
        int removedDuplicate = 0;
        Map<String, Villager> keptByOwnerTag = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Villager) || !isPenNpcCandidate(entity)) {
                    continue;
                }
                String ownerTag = ownerTag(entity);
                if (ownerTag == null) {
                    entity.remove();
                    removedLegacy++;
                    continue;
                }
                Villager existing = keptByOwnerTag.putIfAbsent(ownerTag, (Villager) entity);
                if (existing != null) {
                    entity.remove();
                    removedDuplicate++;
                }
            }
        }
        if (removedLegacy > 0) {
            this.plugin.getLogger().info("Removed old shared pen NPCs: " + removedLegacy);
        }
        if (removedDuplicate > 0) {
            this.plugin.getLogger().info("Removed duplicate owned pen NPCs: " + removedDuplicate);
        }
    }

    public void cleanupChunkPenNpcs(Entity[] entities) {
        int removed = 0;
        for (Entity entity : entities) {
            if (!isLegacyPenNpc(entity)) {
                continue;
            }
            entity.remove();
            removed++;
        }
        if (removed > 0) {
            this.plugin.getLogger().info("Removed old shared pen NPCs from loaded chunk: " + removed);
        }
    }

    private FarmLayout layoutForRanchWorld(World world) {
        Location spawn = new Location(world, 0.5D, FARM_Y + 1.0D, 0.5D, 180.0F, 0.0F);
        Location penNpc = this.ranchWorldService.penNpcLocation(world);
        return new FarmLayout(0, 0, spawn, penNpc);
    }

    private World getMainWorld() {
        if (Bukkit.getWorlds().isEmpty()) {
            throw new IllegalStateException("No loaded worlds for personal farms");
        }
        return Bukkit.getWorlds().get(0);
    }

    private FarmLayout layoutFor(UUID playerId, World world) {
        int slot = Math.floorMod(playerId.hashCode(), FARM_GRID_WIDTH * FARM_GRID_WIDTH);
        int gridX = slot % FARM_GRID_WIDTH;
        int gridZ = slot / FARM_GRID_WIDTH;
        int baseX = FARM_ORIGIN_X + gridX * FARM_SPACING;
        int baseZ = FARM_ORIGIN_Z + gridZ * FARM_SPACING;
        Location spawn = new Location(world, baseX + 0.5D, FARM_Y + 1.0D, baseZ + 0.5D, 180.0F, 0.0F);
        Location penNpc = new Location(world, baseX + 8.5D, FARM_Y + 1.0D, baseZ + 0.5D, 270.0F, 0.0F);
        return new FarmLayout(baseX, baseZ, spawn, penNpc);
    }

    private void preparePlatform(FarmLayout layout) {
        World world = layout.spawn.getWorld();
        for (int x = -FARM_RADIUS; x <= FARM_RADIUS; x++) {
            for (int z = -FARM_RADIUS; z <= FARM_RADIUS; z++) {
                int blockX = layout.baseX + x;
                int blockZ = layout.baseZ + z;
                world.getBlockAt(blockX, FARM_Y - 1, blockZ).setType(Material.GRASS);
                world.getBlockAt(blockX, FARM_Y, blockZ).setType(Material.AIR);
                world.getBlockAt(blockX, FARM_Y + 1, blockZ).setType(Material.AIR);
                if (Math.abs(x) == FARM_RADIUS || Math.abs(z) == FARM_RADIUS) {
                    world.getBlockAt(blockX, FARM_Y, blockZ).setType(Material.FENCE);
                }
            }
        }

        world.getBlockAt(layout.baseX, FARM_Y, layout.baseZ).setType(Material.TORCH);
        world.getBlockAt(layout.baseX + 5, FARM_Y, layout.baseZ + 5).setType(Material.CHEST);
        world.getBlockAt(layout.baseX - 5, FARM_Y, layout.baseZ - 5).setType(Material.HAY_BLOCK);
    }

    private void ensurePenNpc(Player owner, FarmLayout layout) {
        Villager npc = findAndCleanupFarmPenNpc(owner.getUniqueId(), layout.penNpc);
        if (npc == null) {
            npc = (Villager) layout.penNpc.getWorld().spawnEntity(layout.penNpc, EntityType.VILLAGER);
        } else {
            npc.teleport(layout.penNpc);
        }

        npc.setCustomName(PenService.NPC_NAME);
        npc.setCustomNameVisible(true);
        npc.addScoreboardTag(PenService.NPC_TAG);
        npc.addScoreboardTag(FARM_NPC_TAG_PREFIX + owner.getUniqueId());
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setRemoveWhenFarAway(false);
        npc.setProfession(Villager.Profession.FARMER);
    }

    private Villager findAndCleanupFarmPenNpc(UUID ownerId, Location location) {
        Villager nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        String ownerTag = FARM_NPC_TAG_PREFIX + ownerId;
        int removed = 0;
        for (Entity entity : location.getWorld().getEntities()) {
            if (!(entity instanceof Villager) || !entity.getScoreboardTags().contains(ownerTag)) {
                continue;
            }
            double distance = entity.getLocation().distanceSquared(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = (Villager) entity;
            }
        }
        for (Entity entity : location.getWorld().getEntities()) {
            if (entity == nearest || !(entity instanceof Villager) || !entity.getScoreboardTags().contains(ownerTag)) {
                continue;
            }
            entity.remove();
            removed++;
        }
        if (removed > 0) {
            this.plugin.getLogger().info("Removed duplicate pen NPCs for " + ownerId + ": " + removed);
        }
        return nearest;
    }

    private boolean isLegacyPenNpc(Entity entity) {
        if (!(entity instanceof Villager)) {
            return false;
        }
        boolean penNpc = entity.getScoreboardTags().contains(PenService.NPC_TAG);
        String customName = entity.getCustomName();
        if (!penNpc && (customName == null || !PenService.NPC_PLAIN_NAME.equals(ChatColor.stripColor(customName)))) {
            return false;
        }
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith(FARM_NPC_TAG_PREFIX)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPenNpcCandidate(Entity entity) {
        if (!(entity instanceof Villager)) {
            return false;
        }
        if (entity.getScoreboardTags().contains(PenService.NPC_TAG)) {
            return true;
        }
        String customName = entity.getCustomName();
        return customName != null && PenService.NPC_PLAIN_NAME.equals(ChatColor.stripColor(customName));
    }

    private String ownerTag(Entity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith(FARM_NPC_TAG_PREFIX)) {
                return tag;
            }
        }
        return null;
    }

    private static final class FarmLayout {
        private final int baseX;
        private final int baseZ;
        private final Location spawn;
        private final Location penNpc;

        private FarmLayout(int baseX, int baseZ, Location spawn, Location penNpc) {
            this.baseX = baseX;
            this.baseZ = baseZ;
            this.spawn = spawn;
            this.penNpc = penNpc;
        }
    }
}
