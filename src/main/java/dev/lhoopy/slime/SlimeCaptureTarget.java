package dev.lhoopy.slime;

import dev.lhoopy.content.SlimeDef;

import java.util.UUID;

public final class SlimeCaptureTarget {
    private final UUID entityUuid;
    private final SlimeDef definition;

    public SlimeCaptureTarget(UUID entityUuid, SlimeDef definition) {
        this.entityUuid = entityUuid;
        this.definition = definition;
    }

    public UUID getEntityUuid() {
        return this.entityUuid;
    }

    public SlimeDef getDefinition() {
        return this.definition;
    }
}
