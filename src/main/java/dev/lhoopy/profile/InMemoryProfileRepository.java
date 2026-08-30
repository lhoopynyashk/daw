package dev.lhoopy.profile;

import dev.lhoopy.profile.PlayerProfile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryProfileRepository implements ProfileRepository {
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<PlayerProfile> load(UUID playerId) {
        PlayerProfile profile = this.profiles.computeIfAbsent(playerId, id -> new PlayerProfile(id, 0L));
        return CompletableFuture.completedFuture(profile);
    }

    @Override
    public CompletableFuture<Void> save(PlayerProfile profile) {
        this.profiles.put(profile.getPlayerId(), profile);
        return CompletableFuture.completedFuture(null);
    }
}
