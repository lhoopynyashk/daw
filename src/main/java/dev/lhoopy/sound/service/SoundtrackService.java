package dev.lhoopy.sound.service;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import gg.cristalix.wada.Wada;
import gg.cristalix.wada.component.soundplayer.ISoundPlayer;
import gg.cristalix.wada.component.soundplayer.data.Sound;
import gg.cristalix.wada.component.soundplayer.data.SoundAttenuationType;
import gg.cristalix.wada.component.soundplayer.data.SoundCategory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SoundtrackService implements PluginService, Listener {
    private static final String CONFIG_ROOT = "soundtrack";

    private final SlimesPlugin plugin;
    private final List<Track> tracks = new ArrayList<>();
    private final Map<UUID, Playback> playback = new HashMap<>();
    private final Set<UUID> mutedPlayers = new HashSet<>();

    private ISoundPlayer soundPlayer;
    private BukkitTask playbackTask;
    private boolean enabled;
    private boolean autoplay;
    private float volume;
    private int startDelaySeconds;
    private int gapSeconds;

    public SoundtrackService(SlimesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        migrateLegacyConfig();
        loadConfig();
        if (!this.enabled || this.tracks.isEmpty()) {
            this.plugin.getLogger().info("Soundtrack service disabled");
            return;
        }

        this.soundPlayer = Wada.get().getSoundPlayer();
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleAutoplay(player, now);
        }
        this.playbackTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
        this.plugin.getLogger().info("Soundtrack service enabled with " + this.tracks.size() + " track(s)");
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        if (this.playbackTask != null) {
            this.playbackTask.cancel();
            this.playbackTask = null;
        }
        if (this.soundPlayer != null && !Bukkit.getOnlinePlayers().isEmpty()) {
            this.soundPlayer.stopAllSounds(Bukkit.getOnlinePlayers());
        }
        this.playback.clear();
        this.mutedPlayers.clear();
        this.soundPlayer = null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleAutoplay(event.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.playback.remove(event.getPlayer().getUniqueId());
    }

    public void playDefaultSoundtrack(Player player) {
        if (!this.enabled || this.tracks.isEmpty() || this.soundPlayer == null) {
            player.sendMessage("\u00a7eМузыка сейчас недоступна.");
            return;
        }
        this.mutedPlayers.remove(player.getUniqueId());
        this.playback.put(player.getUniqueId(), new Playback(0, 0L));
        playNext(player, System.currentTimeMillis());
        player.sendMessage("\u00a7aМузыка включена.");
    }

    public void stopFor(Player player) {
        this.mutedPlayers.add(player.getUniqueId());
        this.playback.remove(player.getUniqueId());
        if (this.soundPlayer != null) {
            this.soundPlayer.stopAllSounds(player);
        }
        player.sendMessage("\u00a7eМузыка выключена.");
    }

    private void loadConfig() {
        this.enabled = this.plugin.getConfig().getBoolean(CONFIG_ROOT + ".enabled", false);
        this.autoplay = this.plugin.getConfig().getBoolean(CONFIG_ROOT + ".autoplay", true);
        this.volume = clampVolume((float) this.plugin.getConfig().getDouble(CONFIG_ROOT + ".volume", 0.35D));
        this.startDelaySeconds = Math.max(0, this.plugin.getConfig().getInt(CONFIG_ROOT + ".start-delay-seconds", 4));
        this.gapSeconds = Math.max(0, this.plugin.getConfig().getInt(CONFIG_ROOT + ".gap-seconds", 5));
        this.tracks.clear();

        ConfigurationSection tracksSection = this.plugin.getConfig().getConfigurationSection(CONFIG_ROOT + ".tracks");
        if (tracksSection == null) {
            return;
        }
        for (String id : tracksSection.getKeys(false)) {
            String path = CONFIG_ROOT + ".tracks." + id + ".";
            String url = this.plugin.getConfig().getString(path + "url", "").trim();
            int durationSeconds = this.plugin.getConfig().getInt(path + "duration-seconds", 0);
            if (!url.startsWith("https://") || durationSeconds <= 0) {
                this.plugin.getLogger().warning("Skipping invalid soundtrack track '" + id + "'");
                continue;
            }
            this.tracks.add(new Track(id, url, durationSeconds));
        }
    }

    private void migrateLegacyConfig() {
        if (this.plugin.getConfig().isConfigurationSection(CONFIG_ROOT + ".tracks")) {
            return;
        }
        this.plugin.getConfig().set(CONFIG_ROOT + ".enabled", true);
        this.plugin.getConfig().set(CONFIG_ROOT + ".autoplay", true);
        this.plugin.getConfig().set(CONFIG_ROOT + ".volume", 0.35D);
        this.plugin.getConfig().set(CONFIG_ROOT + ".start-delay-seconds", 4);
        this.plugin.getConfig().set(CONFIG_ROOT + ".gap-seconds", 5);
        this.plugin.getConfig().set(CONFIG_ROOT + ".tracks.ranch_theme.url",
                "https://cdn.jsdelivr.net/gh/lhoopynyashk/music@main/music.ogg");
        this.plugin.getConfig().set(CONFIG_ROOT + ".tracks.ranch_theme.duration-seconds", 149);
        this.plugin.getConfig().set(CONFIG_ROOT + ".url", null);
        this.plugin.getConfig().set(CONFIG_ROOT + ".category", null);
        this.plugin.getConfig().set(CONFIG_ROOT + ".replay-interval-seconds", null);
        this.plugin.saveConfig();
        this.plugin.getLogger().info("Migrated legacy soundtrack configuration");
    }

    private void scheduleAutoplay(Player player, long now) {
        if (!this.enabled || !this.autoplay || this.tracks.isEmpty() || this.mutedPlayers.contains(player.getUniqueId())) {
            return;
        }
        long staggerMillis = Math.floorMod(player.getUniqueId().hashCode(), 2_000);
        this.playback.put(player.getUniqueId(), new Playback(
                Math.floorMod(player.getUniqueId().hashCode(), this.tracks.size()),
                now + this.startDelaySeconds * 1_000L + staggerMillis
        ));
    }

    private void tick() {
        if (this.soundPlayer == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Playback state = this.playback.get(player.getUniqueId());
            if (state != null && !this.mutedPlayers.contains(player.getUniqueId()) && now >= state.nextPlayAtMillis) {
                playNext(player, now);
            }
        }
    }

    private void playNext(Player player, long now) {
        Playback state = this.playback.computeIfAbsent(player.getUniqueId(), ignored -> new Playback(0, now));
        Track track = this.tracks.get(state.trackIndex);
        try {
            Sound sound = Sound.builder()
                    .url(track.url)
                    // WADA 4.13.2 serializes coordinates even for 2D sounds.
                    .location(player.getLocation())
                    .volume(this.volume)
                    .pitch(1.0F)
                    .category(SoundCategory.MUSIC)
                    .attenuation(SoundAttenuationType.NONE)
                    .build();
            this.soundPlayer.play(sound, player);
            state.trackIndex = (state.trackIndex + 1) % this.tracks.size();
            state.nextPlayAtMillis = now + (track.durationSeconds + this.gapSeconds) * 1_000L;
        } catch (RuntimeException exception) {
            state.nextPlayAtMillis = now + 15_000L;
            this.plugin.getLogger().warning("Could not start soundtrack '" + track.id + "' for "
                    + player.getName() + ": " + exception.getMessage());
        }
    }

    private static float clampVolume(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static final class Track {
        private final String id;
        private final String url;
        private final int durationSeconds;

        private Track(String id, String url, int durationSeconds) {
            this.id = id;
            this.url = url;
            this.durationSeconds = durationSeconds;
        }
    }

    private static final class Playback {
        private int trackIndex;
        private long nextPlayAtMillis;

        private Playback(int trackIndex, long nextPlayAtMillis) {
            this.trackIndex = trackIndex;
            this.nextPlayAtMillis = nextPlayAtMillis;
        }
    }
}
