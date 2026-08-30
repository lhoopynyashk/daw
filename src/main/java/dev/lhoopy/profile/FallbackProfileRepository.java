package dev.lhoopy.profile;

import dev.lhoopy.profile.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class FallbackProfileRepository implements ProfileRepository {
    private final Plugin plugin;
    private final CoreProfileRepository primary;
    private final ProfileRepository fallback;

    public FallbackProfileRepository(Plugin plugin, CoreProfileRepository primary, ProfileRepository fallback) {
        this.plugin = plugin;
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public CompletableFuture<PlayerProfile> load(Player player) {
        if (!this.primary.isAvailable()) {
            warnFallback("load", player.getUniqueId(), "Core PlayerData service is not available");
            return this.fallback.load(player);
        }

        return this.primary.load(player);
    }

    @Override
    public CompletableFuture<PlayerProfile> load(UUID playerId) {
        if (!this.primary.isAvailable()) {
            warnFallback("load", playerId, "Core PlayerData service is not available");
            return this.fallback.load(playerId);
        }

        return this.primary.load(playerId);
    }

    @Override
    public CompletableFuture<Void> save(PlayerProfile profile) {
        if (!this.primary.isAvailable()) {
            warnFallback("save", profile.getPlayerId(), "Core PlayerData service is not available");
            return this.fallback.save(profile);
        }

        return this.primary.save(profile);
    }

    @Override
    public CompletableFuture<Void> saveAndRelease(PlayerProfile profile) {
        if (!this.primary.isAvailable()) {
            warnFallback("save and release", profile.getPlayerId(), "Core PlayerData service is not available");
            return this.fallback.saveAndRelease(profile);
        }

        return this.primary.saveAndRelease(profile);
    }

    private void warnFallback(String action, UUID playerId, String reason) {
        this.plugin.getLogger().warning("Using local YAML profile fallback for " + action + " " + playerId + ": " + reason);
    }
}
