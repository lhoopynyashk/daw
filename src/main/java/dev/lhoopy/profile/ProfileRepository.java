package dev.lhoopy.profile;

import dev.lhoopy.profile.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProfileRepository {
    default CompletableFuture<PlayerProfile> load(Player player) {
        return load(player.getUniqueId());
    }

    CompletableFuture<PlayerProfile> load(UUID playerId);

    CompletableFuture<Void> save(PlayerProfile profile);

    default CompletableFuture<Void> saveAndRelease(PlayerProfile profile) {
        return save(profile);
    }
}
