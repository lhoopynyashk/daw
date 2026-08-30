package dev.lhoopy.pen;

public final class PenSlime {
    private final String slimeId;
    private long lastFedMillis;
    private long fedUntilMillis;
    private PenFeedQuality feedQuality;
    private double productionRemainder;

    public PenSlime(String slimeId) {
        this(slimeId, 0L, 0L, PenFeedQuality.HUNGRY, 0.0D);
    }

    public PenSlime(String slimeId, long lastFedMillis, long fedUntilMillis) {
        this(slimeId, lastFedMillis, fedUntilMillis,
                fedUntilMillis > 0L ? PenFeedQuality.FAVORITE : PenFeedQuality.HUNGRY, 0.0D);
    }

    public PenSlime(String slimeId, long lastFedMillis, long fedUntilMillis,
                    PenFeedQuality feedQuality, double productionRemainder) {
        this.slimeId = slimeId;
        this.lastFedMillis = Math.max(0L, lastFedMillis);
        this.fedUntilMillis = Math.max(0L, fedUntilMillis);
        this.feedQuality = feedQuality == null ? PenFeedQuality.HUNGRY : feedQuality;
        this.productionRemainder = sanitizeRemainder(productionRemainder);
    }

    public String getSlimeId() {
        return this.slimeId;
    }

    public long getLastFedMillis() {
        return this.lastFedMillis;
    }

    public long getFedUntilMillis() {
        return this.fedUntilMillis;
    }

    public void feed(long now, long fedDurationMillis) {
        feed(now, fedDurationMillis, PenFeedQuality.FAVORITE);
    }

    public void feed(long now, long fedDurationMillis, PenFeedQuality quality) {
        this.lastFedMillis = Math.max(0L, now);
        this.fedUntilMillis = Math.max(this.fedUntilMillis, now + Math.max(0L, fedDurationMillis));
        this.feedQuality = quality == null ? PenFeedQuality.FED : quality;
    }

    public boolean isFed(long now) {
        return this.fedUntilMillis > now;
    }

    public PenFeedQuality getFeedQuality(long now) {
        return isFed(now) ? this.feedQuality : PenFeedQuality.HUNGRY;
    }

    public PenFeedQuality getStoredFeedQuality() {
        return this.feedQuality;
    }

    public double getProductionRemainder() {
        return this.productionRemainder;
    }

    public int produceForBatches(int batches, long now) {
        if (batches <= 0) {
            return 0;
        }
        double total = this.productionRemainder
                + batches * getFeedQuality(now).getProductionMultiplier();
        int produced = (int) Math.floor(total + 1.0E-9D);
        this.productionRemainder = sanitizeRemainder(total - produced);
        return produced;
    }

    private static double sanitizeRemainder(double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            return 0.0D;
        }
        return value >= 1.0D ? value % 1.0D : value;
    }
}
