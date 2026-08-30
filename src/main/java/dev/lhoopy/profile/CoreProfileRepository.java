package dev.lhoopy.profile;

import dev.lhoopy.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.data.IPlayerDataService;
import ru.cristalix.core.database.document.IDocument;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CoreProfileRepository implements ProfileRepository {
    private static final int CACHE_LOAD_ATTEMPTS = 300;
    private static final long CACHE_LOAD_INTERVAL_TICKS = 2L;

    private final Plugin plugin;

    public CoreProfileRepository(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<PlayerProfile> load(Player player) {
        IPlayerDataService service = service();
        if (service == null) {
            return unavailable("Core PlayerData service is not available");
        }

        return waitForCachedData(service, player.getUniqueId(), 0)
                .thenApply(document -> ProfileDocuments.read(player.getUniqueId(), document));
    }

    @Override
    public CompletableFuture<PlayerProfile> load(UUID playerId) {
        IPlayerDataService service = service();
        if (service == null) {
            return unavailable("Core PlayerData service is not available");
        }

        return service.requestReadonlyData(playerId)
                .thenApply(document -> ProfileDocuments.read(playerId, document));
    }

    @Override
    public CompletableFuture<Void> save(PlayerProfile profile) {
        return save(profile, true);
    }

    @Override
    public CompletableFuture<Void> saveAndRelease(PlayerProfile profile) {
        return save(profile, false);
    }

    private CompletableFuture<Void> save(PlayerProfile profile, boolean keepLocked) {
        IPlayerDataService service = service();
        if (service == null) {
            return failed("Core PlayerData service is not available");
        }

        IDocument document = service.requestCachedData(profile.getPlayerId());
        if (document == null) {
            return failed("Core profile was not loaded before save: " + profile.getPlayerId());
        }

        service.updateCachedData(profile.getPlayerId(), ProfileDocuments.writeInto(document, profile));
        return service.updateData(profile.getPlayerId(), keepLocked)
                .exceptionally(error -> {
                    this.plugin.getLogger().warning("Could not save Core profile " + profile.getPlayerId() + ": " + error.getMessage());
                    throw new IllegalStateException(error);
                });
    }

    public boolean isAvailable() {
        return service() != null;
    }

    private IPlayerDataService service() {
        try {
            return CoreApi.get().getService(IPlayerDataService.class);
        } catch (NoClassDefFoundError coreApiMissing) {
            return null;
        } catch (RuntimeException error) {
            this.plugin.getLogger().warning(
                    "Cristalix Core call failed: " + error);
            return null;
        }
    }

    private CompletableFuture<IDocument> waitForCachedData(IPlayerDataService service, UUID playerId, int attempt) {
        IDocument document = service.requestCachedData(playerId);
        if (document != null) {
            return CompletableFuture.completedFuture(document);
        }
        if (attempt >= CACHE_LOAD_ATTEMPTS) {
            return failed("Core profile cache was not populated: " + playerId);
        }

        CompletableFuture<IDocument> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskLater(this.plugin, () ->
                waitForCachedData(service, playerId, attempt + 1)
                        .whenComplete((loaded, error) -> {
                            if (error != null) {
                                future.completeExceptionally(error);
                                return;
                            }
                            future.complete(loaded);
                        }), CACHE_LOAD_INTERVAL_TICKS);
        return future;
    }

    private static <T> CompletableFuture<T> unavailable(String message) {
        return failed(message);
    }

    private static <T> CompletableFuture<T> failed(String message) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException(message));
        return future;
    }
}
