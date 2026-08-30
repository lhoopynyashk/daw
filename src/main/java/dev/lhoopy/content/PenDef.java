package dev.lhoopy.content;

public final class PenDef {
    private final String id;
    private final String displayName;
    private final int baseCapacity;
    private final int upgradePrice;

    public PenDef(String id, String displayName, int baseCapacity, int upgradePrice) {
        this.id = id;
        this.displayName = displayName;
        this.baseCapacity = baseCapacity;
        this.upgradePrice = upgradePrice;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getBaseCapacity() {
        return this.baseCapacity;
    }

    public int getUpgradePrice() {
        return this.upgradePrice;
    }
}
