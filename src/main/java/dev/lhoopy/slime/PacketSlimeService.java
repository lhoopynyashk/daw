package dev.lhoopy.slime;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.SlimeDef;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PacketSlimeService {
    private static final double VACUUM_RANGE = 6.5D;
    private static final double VACUUM_RANGE_SQUARED = VACUUM_RANGE * VACUUM_RANGE;

    private final ContentRegistry contentRegistry;
    private final List<PacketSlime> packetSlimes = new ArrayList<>();
    private final Map<UUID, BukkitTask> interestTasks = new HashMap<>();
    private final PacketSlimeView view = new PacketSlimeView();
    private final PacketSlimeMovementService movementService = new PacketSlimeMovementService(this.view, this::cancelInterest);
    private BukkitTask movementTask;

    PacketSlimeService(ContentRegistry contentRegistry) {
        this.contentRegistry = contentRegistry;
    }

    void enable(Plugin plugin) {
        if (this.movementTask != null) {
            this.movementTask.cancel();
        }
        this.movementTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> this.movementService.tick(this.packetSlimes),
                PacketSlimeMovementService.MOVEMENT_PERIOD_TICKS, PacketSlimeMovementService.MOVEMENT_PERIOD_TICKS);
    }

    public void handleCommand(Player player, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            clear(player);
            player.sendMessage(ChatColor.GREEN + "Визуальные слаймы очищены.");
            return;
        }

        spawn(player, args);
    }

    public void shutdown() {
        if (this.movementTask != null) {
            this.movementTask.cancel();
            this.movementTask = null;
        }
        for (PacketSlime packetSlime : new ArrayList<>(this.packetSlimes)) {
            Player viewer = Bukkit.getPlayer(packetSlime.getViewerId());
            if (viewer != null && viewer.isOnline()) {
                this.view.destroy(viewer, packetSlime);
            }
        }
        this.packetSlimes.clear();
        for (BukkitTask task : this.interestTasks.values()) {
            task.cancel();
        }
        this.interestTasks.clear();
    }

    void spawn(Player player, String[] args) {
        String id = args.length >= 1 ? args[0] : null;
        SlimeDef definition = id == null ? this.contentRegistry.getDefaultSlime() : this.contentRegistry.getSlime(id);
        if (definition == null) {
            player.sendMessage(ChatColor.RED + "Неизвестный тип слайма. Загружено типов: " + this.contentRegistry.slimes().size());
            return;
        }

        SlimeState state = args.length >= 2 && args[1].equalsIgnoreCase("interested")
                ? SlimeState.INTERESTED
                : SlimeState.NORMAL;
        Location location = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(3.0D));
        PacketSlime packetSlime = this.view.spawn(player, definition, state, location);
        this.packetSlimes.add(packetSlime);

        player.sendMessage(ChatColor.GREEN + "Спавн: " + definition.getDisplayName()
                + ChatColor.GRAY + " id=" + packetSlime.getEntityId()
                + " состояние=" + state.name().toLowerCase());
    }

    void spawnHuntSlime(Player player, SlimeDef definition, Location location) {
        this.packetSlimes.add(this.view.spawn(player, definition, SlimeState.NORMAL, location));
    }

    int countHuntSlimes(Player player) {
        int count = 0;
        for (PacketSlime packetSlime : this.packetSlimes) {
            if (packetSlime.getPurpose() == PacketSlimePurpose.HUNT
                    && packetSlime.getViewerId().equals(player.getUniqueId())
                    && packetSlime.getLocation().getWorld().equals(player.getWorld())) {
                count++;
            }
        }
        return count;
    }

    void clearHuntSlimes(Player player) {
        Iterator<PacketSlime> iterator = this.packetSlimes.iterator();
        while (iterator.hasNext()) {
            PacketSlime packetSlime = iterator.next();
            if (!packetSlime.getViewerId().equals(player.getUniqueId())
                    || packetSlime.getPurpose() != PacketSlimePurpose.HUNT) {
                continue;
            }
            this.view.destroy(player, packetSlime);
            cancelInterest(packetSlime.getUniqueId());
            iterator.remove();
        }
    }

    void spawnPenSlime(Player player, SlimeDef definition, Location location, int penIndex) {
        this.packetSlimes.add(this.view.spawn(player, definition, SlimeState.NORMAL, location, PacketSlimePurpose.PEN, penIndex));
    }

    void clear(Player player) {
        Iterator<PacketSlime> iterator = this.packetSlimes.iterator();
        while (iterator.hasNext()) {
            PacketSlime packetSlime = iterator.next();
            if (!packetSlime.getViewerId().equals(player.getUniqueId())) {
                continue;
            }
            this.view.destroy(player, packetSlime);
            cancelInterest(packetSlime.getUniqueId());
            iterator.remove();
        }
    }

    void clearPenSlimes(Player player) {
        Iterator<PacketSlime> iterator = this.packetSlimes.iterator();
        while (iterator.hasNext()) {
            PacketSlime packetSlime = iterator.next();
            if (!packetSlime.getViewerId().equals(player.getUniqueId()) || packetSlime.getPurpose() != PacketSlimePurpose.PEN) {
                continue;
            }
            this.view.destroy(player, packetSlime);
            iterator.remove();
        }
    }

    void clearForViewer(Player player) {
        clear(player);
    }

    PacketSlime findNearest(Player player, boolean interestedOnly) {
        PacketSlime nearest = null;
        double nearestDistance = VACUUM_RANGE_SQUARED;
        for (PacketSlime packetSlime : this.packetSlimes) {
            if (packetSlime.getPurpose() != PacketSlimePurpose.HUNT) {
                continue;
            }
            if (!packetSlime.getViewerId().equals(player.getUniqueId())) {
                continue;
            }
            if (interestedOnly && packetSlime.getState() != SlimeState.INTERESTED) {
                continue;
            }
            if (!packetSlime.getLocation().getWorld().equals(player.getWorld())) {
                continue;
            }
            double distance = packetSlime.getLocation().distanceSquared(player.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = packetSlime;
            }
        }
        return nearest;
    }

    PacketSlime findNearestPenSlime(Player player, double range) {
        PacketSlime nearest = null;
        double nearestDistance = range * range;
        for (PacketSlime packetSlime : this.packetSlimes) {
            if (packetSlime.getPurpose() != PacketSlimePurpose.PEN || !packetSlime.getViewerId().equals(player.getUniqueId())) {
                continue;
            }
            if (!packetSlime.getLocation().getWorld().equals(player.getWorld())) {
                continue;
            }
            double distance = packetSlime.getLocation().distanceSquared(player.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = packetSlime;
            }
        }
        return nearest;
    }

    boolean remove(Player viewer, UUID uniqueId) {
        Iterator<PacketSlime> iterator = this.packetSlimes.iterator();
        while (iterator.hasNext()) {
            PacketSlime packetSlime = iterator.next();
            if (!packetSlime.getViewerId().equals(viewer.getUniqueId()) || !packetSlime.getUniqueId().equals(uniqueId)) {
                continue;
            }
            this.view.destroy(viewer, packetSlime);
            iterator.remove();
            BukkitTask task = this.interestTasks.remove(uniqueId);
            if (task != null) {
                task.cancel();
            }
            return true;
        }
        return false;
    }

    boolean handleFoodUse(Plugin plugin, Player player, ItemStack itemInHand) {
        if (itemInHand == null || itemInHand.getType() == Material.AIR || SlimeVacuumItem.isVacuum(itemInHand)) {
            return false;
        }

        PacketSlime nearest = findNearest(player, false);
        if (nearest == null) {
            return false;
        }
        if (nearest.getState() == SlimeState.INTERESTED) {
            player.sendMessage(ChatColor.YELLOW + "Слайм уже заинтересован. Используй сосалку.");
            return true;
        }
        if (itemInHand.getType() != nearest.getDefinition().getFavoriteFood()) {
            player.sendMessage(ChatColor.RED + "Эта еда не интересует слайма.");
            player.sendMessage(ChatColor.GRAY + "Нужная еда: " + nearest.getDefinition().getFavoriteFood().name());
            return true;
        }

        consumeOne(player, itemInHand);
        nearest.setState(SlimeState.INTERESTED);
        this.view.refreshMetadata(player, nearest);
        scheduleInterestReset(plugin, player, nearest);
        player.sendMessage(ChatColor.GREEN + "Слайм заинтересовался! "
                + ChatColor.GRAY + "Время: " + nearest.getDefinition().getInterestSeconds() + "с.");
        return true;
    }

    private void scheduleInterestReset(Plugin plugin, Player player, PacketSlime slime) {
        BukkitTask oldTask = this.interestTasks.remove(slime.getUniqueId());
        if (oldTask != null) {
            oldTask.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!removeMissing(slime) && player.isOnline()) {
                slime.setState(SlimeState.NORMAL);
                this.view.refreshMetadata(player, slime);
            }
            this.interestTasks.remove(slime.getUniqueId());
        }, slime.getDefinition().getInterestSeconds() * 20L);
        this.interestTasks.put(slime.getUniqueId(), task);
    }

    private boolean removeMissing(PacketSlime slime) {
        return !this.packetSlimes.contains(slime);
    }

    private void cancelInterest(UUID slimeId) {
        BukkitTask task = this.interestTasks.remove(slimeId);
        if (task != null) {
            task.cancel();
        }
    }

    private static void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() <= 1) {
            player.setItemInHand(null);
            return;
        }
        item.setAmount(item.getAmount() - 1);
        player.setItemInHand(item);
    }

}
