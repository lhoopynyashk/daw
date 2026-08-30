package dev.lhoopy.content;

public final class FoodDef {
    private final String id;
    private final String displayName;
    private final String type;

    public FoodDef(String id, String displayName, String type) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getType() {
        return this.type;
    }
}
