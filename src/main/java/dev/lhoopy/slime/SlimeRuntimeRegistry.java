package dev.lhoopy.slime;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class SlimeRuntimeRegistry {
    private static final double VACUUM_RANGE = 6.5D;
    private static final double VACUUM_RANGE_SQUARED = VACUUM_RANGE * VACUUM_RANGE;

    private final Map<UUID, RuntimeSlime> slimes = new HashMap<>();
    private final Map<UUID, BukkitTask> interestTasks = new HashMap<>();

    RuntimeSlime get(UUID entityId) {
        return this.slimes.get(entityId);
    }

    void put(UUID entityId, RuntimeSlime runtime) {
        this.slimes.put(entityId, runtime);
    }

    void remove(UUID entityId) {
        this.slimes.remove(entityId);
        BukkitTask task = this.interestTasks.remove(entityId);
        if (task != null) {
            task.cancel();
        }
    }

    void putInterestTask(UUID entityId, BukkitTask task) {
        BukkitTask oldTask = this.interestTasks.remove(entityId);
        if (oldTask != null) {
            oldTask.cancel();
        }
        this.interestTasks.put(entityId, task);
    }

    void removeInterestTask(UUID entityId) {
        this.interestTasks.remove(entityId);
    }

    void shutdown() {
        for (BukkitTask task : this.interestTasks.values()) {
            task.cancel();
        }
        this.interestTasks.clear();
        this.slimes.clear();
    }

    RuntimeSlimeTarget findNearest(Player player, boolean interestedOnly) {
        RuntimeSlimeTarget nearest = null;
        double nearestDistance = VACUUM_RANGE_SQUARED;
        for (Map.Entry<UUID, RuntimeSlime> entry : this.slimes.entrySet()) {
            RuntimeSlime runtime = entry.getValue();
            if (interestedOnly && runtime.state != SlimeState.INTERESTED) {
                continue;
            }
            Entity entity = findEntity(entry.getKey());
            if (!(entity instanceof Slime) || entity.isDead() || !entity.getWorld().equals(player.getWorld())) {
                continue;
            }

            double distance = entity.getLocation().distanceSquared(player.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = new RuntimeSlimeTarget((Slime) entity, runtime);
            }
        }
        return nearest;
    }

    static Entity findEntity(UUID uuid) {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid)) {
                    return entity;
                }
            }
        }
        return null;
    }

    static void applyName(Slime slime, RuntimeSlime runtime) {
        String stateText = runtime.state == SlimeState.INTERESTED
                ? ChatColor.YELLOW + "заинтересован"
                : ChatColor.GRAY + "обычный";
        slime.setCustomName(ChatColor.GREEN + runtime.definition.getDisplayName() + ChatColor.DARK_GRAY + " [" + stateText + ChatColor.DARK_GRAY + "]");
        slime.setCustomNameVisible(true);
    }
}
