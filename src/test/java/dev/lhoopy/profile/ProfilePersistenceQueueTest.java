package dev.lhoopy.profile;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class ProfilePersistenceQueueTest {
    @Test
    void runsOperationsForOnePlayerInOrder() {
        ProfilePersistenceQueue queue = new ProfilePersistenceQueue();
        UUID playerId = UUID.randomUUID();
        CompletableFuture<Void> firstGate = new CompletableFuture<>();
        List<String> calls = new ArrayList<>();

        CompletableFuture<Void> first = queue.enqueue(playerId, () -> {
            calls.add("save");
            return firstGate;
        });
        CompletableFuture<Void> release = queue.enqueue(playerId, () -> {
            calls.add("release");
            return CompletableFuture.completedFuture(null);
        });

        assertEquals(Arrays.asList("save"), calls);
        assertFalse(release.isDone());

        firstGate.complete(null);
        CompletableFuture.allOf(first, release).join();
        assertEquals(Arrays.asList("save", "release"), calls);
    }

    @Test
    void failedSaveDoesNotPreventFinalRelease() {
        ProfilePersistenceQueue queue = new ProfilePersistenceQueue();
        UUID playerId = UUID.randomUUID();
        List<String> calls = new ArrayList<>();
        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("save failed"));

        queue.enqueue(playerId, () -> {
            calls.add("save");
            return failed;
        });
        CompletableFuture<Void> release = queue.enqueue(playerId, () -> {
            calls.add("release");
            return CompletableFuture.completedFuture(null);
        });

        release.join();
        assertEquals(Arrays.asList("save", "release"), calls);
    }
}
