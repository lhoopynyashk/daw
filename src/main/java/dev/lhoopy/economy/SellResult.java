package dev.lhoopy.economy;

public final class SellResult {
    private final int vacpackAmount;
    private final int storageAmount;
    private final long coins;

    public SellResult(int vacpackAmount, int storageAmount, long coins) {
        this.vacpackAmount = Math.max(0, vacpackAmount);
        this.storageAmount = Math.max(0, storageAmount);
        this.coins = Math.max(0L, coins);
    }

    public int getVacpackAmount() {
        return this.vacpackAmount;
    }

    public int getStorageAmount() {
        return this.storageAmount;
    }

    public int getTotalAmount() {
        return this.vacpackAmount + this.storageAmount;
    }

    public long getCoins() {
        return this.coins;
    }

    public boolean isEmpty() {
        return getTotalAmount() <= 0 || this.coins <= 0L;
    }
}
