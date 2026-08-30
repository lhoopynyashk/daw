package dev.lhoopy.storage;

public final class StoredItem {
    private final String itemId;
    private int amount;
    private boolean protectedItem;

    public StoredItem(String itemId, int amount, boolean protectedItem) {
        this.itemId = itemId;
        this.amount = Math.max(0, amount);
        this.protectedItem = protectedItem;
    }

    public String getItemId() {
        return this.itemId;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    public void addAmount(int amount) {
        if (amount <= 0) {
            return;
        }
        this.amount += amount;
    }

    public boolean removeAmount(int amount) {
        if (amount <= 0 || this.amount < amount) {
            return false;
        }
        this.amount -= amount;
        return true;
    }

    public boolean isProtectedItem() {
        return this.protectedItem;
    }

    public void setProtectedItem(boolean protectedItem) {
        this.protectedItem = protectedItem;
    }
}
