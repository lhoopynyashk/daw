package dev.lhoopy.pen;

public enum PenFeedQuality {
    HUNGRY(0.50D),
    FED(1.00D),
    FAVORITE(1.25D);

    private final double productionMultiplier;

    PenFeedQuality(double productionMultiplier) {
        this.productionMultiplier = productionMultiplier;
    }

    public double getProductionMultiplier() {
        return this.productionMultiplier;
    }

    public static PenFeedQuality read(String value, PenFeedQuality fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
