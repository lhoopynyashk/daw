package dev.lhoopy.content;

import org.bukkit.Material;

public final class SlimeDef {
    private final String id;
    private final String displayName;
    private final Material itemMaterial;
    private final Material favoriteFood;
    private final String rarity;
    private final int captureDifficulty;
    private final int sellPrice;
    private final int interestSeconds;
    private final int size;

    public SlimeDef(
            String id,
            String displayName,
            Material itemMaterial,
            Material favoriteFood,
            String rarity,
            int captureDifficulty,
            int sellPrice,
            int interestSeconds,
            int size
    ) {
        this.id = id;
        this.displayName = displayName;
        this.itemMaterial = itemMaterial;
        this.favoriteFood = favoriteFood;
        this.rarity = rarity;
        this.captureDifficulty = captureDifficulty;
        this.sellPrice = sellPrice;
        this.interestSeconds = interestSeconds;
        this.size = size;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Material getItemMaterial() {
        return this.itemMaterial;
    }

    public Material getFavoriteFood() {
        return this.favoriteFood;
    }

    public String getRarity() {
        return this.rarity;
    }

    public int getCaptureDifficulty() {
        return this.captureDifficulty;
    }

    public int getSellPrice() {
        return this.sellPrice;
    }

    public int getInterestSeconds() {
        return this.interestSeconds;
    }

    public int getSize() {
        return this.size;
    }
}
