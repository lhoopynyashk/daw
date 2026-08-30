package dev.lhoopy.content;

public final class UpgradeDef {
    private final String id;
    private final String displayName;
    private final int price;

    public UpgradeDef(String id, String displayName, int price) {
        this.id = id;
        this.displayName = displayName;
        this.price = price;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getPrice() {
        return this.price;
    }
}
