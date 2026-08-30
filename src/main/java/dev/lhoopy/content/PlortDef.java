package dev.lhoopy.content;

public final class PlortDef {
    private final String id;
    private final String displayName;
    private final int basePrice;

    public PlortDef(String id, String displayName, int basePrice) {
        this.id = id;
        this.displayName = displayName;
        this.basePrice = basePrice;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getBasePrice() {
        return this.basePrice;
    }
}
