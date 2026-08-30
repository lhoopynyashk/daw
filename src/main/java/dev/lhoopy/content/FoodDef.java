package dev.lhoopy.content;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

public final class FoodDef {
    private final String id;
    private final String displayName;
    private final String type;
    private final List<Material> materials;

    public FoodDef(String id, String displayName, String type, List<Material> materials) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.materials = materials == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(materials);
    }

    public List<Material> getMaterials() {
        return this.materials;
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
