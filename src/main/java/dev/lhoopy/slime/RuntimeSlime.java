package dev.lhoopy.slime;

import dev.lhoopy.content.SlimeDef;

final class RuntimeSlime {
    final SlimeDef definition;
    SlimeState state;

    RuntimeSlime(SlimeDef definition, SlimeState state) {
        this.definition = definition;
        this.state = state;
    }
}
