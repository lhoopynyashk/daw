package dev.lhoopy.content;

public final class ResourceDef {
    private final String id;
    private final String displayName;
    private final String locationId;
    private final String rarity;
    private final String spawnZone;
    private final String use;

    public ResourceDef(String id, String displayName, String locationId, String rarity, String spawnZone, String use) {
        this.id = id;
        this.displayName = displayName;
        this.locationId = locationId;
        this.rarity = rarity;
        this.spawnZone = spawnZone;
        this.use = use;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getLocationId() {
        return this.locationId;
    }

    public String getRarity() {
        return this.rarity;
    }

    public String getSpawnZone() {
        return this.spawnZone;
    }

    public String getUse() {
        return this.use;
    }
}
