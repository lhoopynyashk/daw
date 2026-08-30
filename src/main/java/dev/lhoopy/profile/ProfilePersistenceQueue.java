package dev.lhoopy.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class ProfilePersistenceQueue {
    private final Map<UUID, CompletableFuture<Void>> tails = new HashMap<>();

    synchronized CompletableFuture<Void> enqueue(UUID playerId, Supplier<CompletableFuture<Void>> operation) {
        CompletableFuture<Void> previous = this.tails.get(playerId);
        CompletableFuture<Void> ready = previous == null
                ? CompletableFuture.completedFuture(null)
                : previous.handle((ignored, error) -> null);
        CompletableFuture<Void> next = ready.thenCompose(ignored -> invoke(operation));
        this.tails.put(playerId, next);
        next.whenComplete((ignored, error) -> removeTail(playerId, next));
        return next;
    }

    private static CompletableFuture<Void> invoke(Supplier<CompletableFuture<Void>> operation) {
        try {
            CompletableFuture<Void> future = operation.get();
            return future == null ? CompletableFuture.completedFuture(null) : future;
        } catch (Throwable error) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(error);
            return failed;
        }
    }

    private synchronized void removeTail(UUID playerId, CompletableFuture<Void> completed) {
        if (this.tails.get(playerId) == completed) {
            this.tails.remove(playerId);
        }
    }
}
