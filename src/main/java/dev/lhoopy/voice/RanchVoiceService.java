package dev.lhoopy.voice;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.world.RanchWorldService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import ru.cristalix.core.voice.IVoiceService;
import ru.cristalix.core.voice.VoiceChatSettings;
import ru.cristalix.core.voice.room.VoiceFlag;
import ru.cristalix.core.voice.room.VoiceRoom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RanchVoiceService implements PluginService, Listener {
    private static final int VOICE_DISTANCE_BLOCKS = 48;

    private final SlimesPlugin plugin;
    private final RanchWorldService ranchWorldService;
    private final Map<String, VoiceRoom> roomsByWorld = new HashMap<>();
    private final Map<UUID, String> playerRoomNames = new HashMap<>();
    private IVoiceService voiceService;
    private BukkitTask syncTask;

    public RanchVoiceService(SlimesPlugin plugin, RanchWorldService ranchWorldService) {
        this.plugin = plugin;
        this.ranchWorldService = ranchWorldService;
    }

    @Override
    public void enable() {
        try {
            IVoiceService.register(this.plugin);
        } catch (Throwable ignored) {
            // Core may already have a registered voice service on shared test realms.
        }
        try {
            this.voiceService = IVoiceService.get();
            this.voiceService.updateSettings(VoiceChatSettings.builder()
                    .maxDinstanceInBlocks(VOICE_DISTANCE_BLOCKS)
                    .spatial(true)
                    .hidePlayers(false)
                    .build());
        } catch (Throwable throwable) {
            this.voiceService = null;
            this.plugin.getLogger().warning("Ranch voice chat is unavailable: " + throwable.getMessage());
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        this.syncTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                syncPlayer(player);
            }
            cleanupEmptyRooms();
        }, 20L, 40L);
        this.plugin.getLogger().info("Ranch voice chat enabled");
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        if (this.syncTask != null) {
            this.syncTask.cancel();
            this.syncTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            removePlayer(player);
        }
        this.roomsByWorld.clear();
        this.playerRoomNames.clear();
        this.voiceService = null;
        this.plugin.getLogger().info("Ranch voice chat disabled");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> syncPlayer(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        syncPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTask(this.plugin, () -> syncPlayer(event.getPlayer()));
    }

    private void syncPlayer(Player player) {
        if (this.voiceService == null || !player.isOnline()) {
            return;
        }

        World world = player.getWorld();
        String desiredRoomName = this.ranchWorldService.isRanchWorld(world) ? world.getName() : null;
        String currentRoomName = this.playerRoomNames.get(player.getUniqueId());
        if (desiredRoomName != null && desiredRoomName.equals(currentRoomName)) {
            return;
        }

        removePlayer(player);
        if (desiredRoomName == null) {
            return;
        }

        VoiceRoom room = roomForWorld(desiredRoomName);
        if (room.addPlayer(player)) {
            this.playerRoomNames.put(player.getUniqueId(), desiredRoomName);
        }
    }

    private VoiceRoom roomForWorld(String worldName) {
        VoiceRoom existing = this.roomsByWorld.get(worldName);
        if (existing != null) {
            return existing;
        }

        VoiceRoom room = this.voiceService.createRoom();
        room.setDefaultFlag(VoiceFlag.SPEAK, true, true);
        room.setDefaultFlag(VoiceFlag.LISTEN, true, true);
        this.roomsByWorld.put(worldName, room);
        return room;
    }

    private void removePlayer(Player player) {
        String roomName = this.playerRoomNames.remove(player.getUniqueId());
        if (roomName == null) {
            return;
        }
        VoiceRoom room = this.roomsByWorld.get(roomName);
        if (room != null) {
            room.removePlayer(player);
        }
    }

    private void cleanupEmptyRooms() {
        Iterator<Map.Entry<String, VoiceRoom>> iterator = this.roomsByWorld.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, VoiceRoom> entry = iterator.next();
            if (entry.getValue().getPlayerMap().isEmpty()) {
                iterator.remove();
            }
        }
    }
}
