package dev.lhoopy.profile;

import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileRepository;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public final class ProfileService implements PluginService {
    private final ProfileRepository repository;
    private final Map<UUID, PlayerProfile> loadedProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<PlayerProfile>> loadingProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> releasingProfiles = new ConcurrentHashMap<>();
    private final ProfilePersistenceQueue persistenceQueue = new ProfilePersistenceQueue();

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public void enable() {
    }

    @Override
    public void shutdown() {
        List<CompletableFuture<Void>> saves = new ArrayList<>();
        for (UUID playerId : new ArrayList<>(this.loadedProfiles.keySet())) {
            saves.add(releaseLoaded(playerId).exceptionally(error -> {
                org.bukkit.Bukkit.getLogger().warning("[SlimeRancher] Could not save and release profile on shutdown " + playerId + ": " + error.getMessage());
                return null;
            }));
        }
        CompletableFuture.allOf(saves.toArray(new CompletableFuture[0])).join();
        this.loadedProfiles.clear();
        this.loadingProfiles.clear();
        this.releasingProfiles.clear();
    }

    public CompletableFuture<PlayerProfile> load(Player player) {
        if (isLoaded(player.getUniqueId())) {
            return CompletableFuture.completedFuture(this.loadedProfiles.get(player.getUniqueId()));
        }
        CompletableFuture<PlayerProfile> existing = this.loadingProfiles.get(player.getUniqueId());
        if (existing != null) {
            return existing;
        }

        org.bukkit.Bukkit.getLogger().info("[SlimeRancher] Loading profile " + player.getName() + " " + player.getUniqueId());
        player.sendMessage(ChatColor.GRAY + "Профиль SlimeRancher загружается...");
        CompletableFuture<PlayerProfile> future = this.repository.load(player);
        this.loadingProfiles.put(player.getUniqueId(), future);
        future.thenAccept(profile -> {
                    profile.setLastPlortProductionMillis(System.currentTimeMillis());
                    this.loadedProfiles.put(player.getUniqueId(), profile);
                    this.loadingProfiles.remove(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "Профиль SlimeRancher загружен.");
                    org.bukkit.Bukkit.getLogger().info("[SlimeRancher] Loaded profile " + player.getName()
                            + " captured=" + profile.getCapturedSlimeIds().size()
                            + " pen=" + profile.getPenSlimeIds().size()
                            + " capacity=" + profile.getPenCapacity());
                })
                .exceptionally(error -> {
                    this.loadingProfiles.remove(player.getUniqueId());
                    org.bukkit.Bukkit.getLogger().warning("[SlimeRancher] Could not load profile " + player.getName() + ": " + error.getMessage());
                    error.printStackTrace();
                    player.sendMessage(ChatColor.RED + "Профиль SlimeRancher пока не загружен. Перезайди через минуту.");
                    return null;
                });
        return future;
    }

    public CompletableFuture<Void> unload(Player player) {
        this.loadingProfiles.remove(player.getUniqueId());
        if (!this.loadedProfiles.containsKey(player.getUniqueId())
                && !this.releasingProfiles.containsKey(player.getUniqueId())) {
            return CompletableFuture.completedFuture(null);
        }
        org.bukkit.Bukkit.getLogger().info("[SlimeRancher] Saving and releasing profile "
                + player.getName() + " " + player.getUniqueId());
        return releaseLoaded(player.getUniqueId())
                .thenRun(() -> org.bukkit.Bukkit.getLogger().info("[SlimeRancher] Saved and released profile "
                        + player.getName() + " " + player.getUniqueId()))
                .exceptionally(error -> {
                    org.bukkit.Bukkit.getLogger().warning("[SlimeRancher] Could not save profile "
                            + player.getName() + ": " + error.getMessage());
                    return null;
                });
    }

    public PlayerProfile getLoaded(UUID playerId) {
        return this.loadedProfiles.get(playerId);
    }

    public boolean isLoaded(UUID playerId) {
        return this.loadedProfiles.containsKey(playerId);
    }

    public boolean ensureLoaded(Player player) {
        if (isLoaded(player.getUniqueId())) {
            return true;
        }
        load(player);
        return false;
    }

    public CompletableFuture<Void> saveLoaded(UUID playerId) {
        PlayerProfile profile = this.loadedProfiles.get(playerId);
        if (profile != null) {
            return this.persistenceQueue.enqueue(playerId, () -> this.repository.save(profile));
        }
        return CompletableFuture.completedFuture(null);
    }

    public synchronized CompletableFuture<Void> releaseLoaded(UUID playerId) {
        CompletableFuture<Void> existing = this.releasingProfiles.get(playerId);
        if (existing != null) {
            return existing;
        }

        PlayerProfile profile = this.loadedProfiles.remove(playerId);
        if (profile == null) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException(
                    this.loadingProfiles.containsKey(playerId)
                            ? "Profile is still loading: " + playerId
                            : "Profile is not loaded: " + playerId
            ));
            return failed;
        }
        profile.setLastPlortProductionMillis(System.currentTimeMillis());

        CompletableFuture<Void> release = this.persistenceQueue.enqueue(
                playerId,
                () -> this.repository.saveAndRelease(profile)
        );
        this.releasingProfiles.put(playerId, release);
        release.whenComplete((ignored, error) -> {
            this.releasingProfiles.remove(playerId, release);
            if (error != null) {
                this.loadedProfiles.putIfAbsent(playerId, profile);
            }
        });
        return release;
    }
}
