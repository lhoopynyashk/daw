package dev.lhoopy.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LocationDef {
    private final String id;
    private final String displayName;
    private final int tier;
    private final List<String> normalSlimeIds;
    private final List<String> secretSlimeIds;
    private final List<String> resourceIds;
    private final List<String> unlockRequirements;
    private final String completionReward;

    public LocationDef(String id, String displayName, int tier, List<String> normalSlimeIds, List<String> secretSlimeIds, List<String> resourceIds, List<String> unlockRequirements, String completionReward) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.normalSlimeIds = immutableList(normalSlimeIds);
        this.secretSlimeIds = immutableList(secretSlimeIds);
        this.resourceIds = immutableList(resourceIds);
        this.unlockRequirements = immutableList(unlockRequirements);
        this.completionReward = completionReward;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getTier() {
        return this.tier;
    }

    public List<String> getNormalSlimeIds() {
        return this.normalSlimeIds;
    }

    public List<String> getSecretSlimeIds() {
        return this.secretSlimeIds;
    }

    public List<String> getResourceIds() {
        return this.resourceIds;
    }

    public List<String> getUnlockRequirements() {
        return this.unlockRequirements;
    }

    public String getCompletionReward() {
        return this.completionReward;
    }

    private static List<String> immutableList(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
