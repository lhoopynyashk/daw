package dev.lhoopy.content;

public final class RewardDef {
    private final String id;
    private final String itemId;
    private final int amount;

    public RewardDef(String id, String itemId, int amount) {
        this.id = id;
        this.itemId = itemId;
        this.amount = amount;
    }

    public String getId() {
        return this.id;
    }

    public String getItemId() {
        return this.itemId;
    }

    public int getAmount() {
        return this.amount;
    }
}
