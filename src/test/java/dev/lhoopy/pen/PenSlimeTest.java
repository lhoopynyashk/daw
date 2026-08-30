package dev.lhoopy.pen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PenSlimeTest {
    private static final long NOW = 1_000_000L;

    @Test
    void hungrySlimeProducesEverySecondBatch() {
        PenSlime slime = new PenSlime("slime_pink");

        assertEquals(0, slime.produceForBatches(1, NOW));
        assertEquals(1, slime.produceForBatches(1, NOW));
        assertEquals(0.0D, slime.getProductionRemainder(), 0.0001D);
    }

    @Test
    void regularFoodProducesAtFullRate() {
        PenSlime slime = new PenSlime("slime_pink");
        slime.feed(NOW, 10_000L, PenFeedQuality.FED);

        assertEquals(3, slime.produceForBatches(3, NOW + 1L));
    }

    @Test
    void favoriteFoodProducesFivePlortsInFourBatches() {
        PenSlime slime = new PenSlime("slime_pink");
        slime.feed(NOW, 10_000L, PenFeedQuality.FAVORITE);

        assertEquals(5, slime.produceForBatches(4, NOW + 1L));
        assertEquals(0.0D, slime.getProductionRemainder(), 0.0001D);
    }

    @Test
    void expiredFoodReturnsToHungryRate() {
        PenSlime slime = new PenSlime("slime_pink");
        slime.feed(NOW, 100L, PenFeedQuality.FAVORITE);

        assertEquals(PenFeedQuality.HUNGRY, slime.getFeedQuality(NOW + 101L));
        assertEquals(1, slime.produceForBatches(2, NOW + 101L));
    }
}
