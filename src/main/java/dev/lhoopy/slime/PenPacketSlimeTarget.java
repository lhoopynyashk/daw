package dev.lhoopy.slime;

import dev.lhoopy.content.SlimeDef;

public final class PenPacketSlimeTarget {
    private final SlimeDef definition;
    private final int penIndex;

    public PenPacketSlimeTarget(SlimeDef definition, int penIndex) {
        this.definition = definition;
        this.penIndex = penIndex;
    }

    public SlimeDef getDefinition() {
        return this.definition;
    }

    public int getPenIndex() {
        return this.penIndex;
    }
}
