package dev.lhoopy.slime;

import org.bukkit.entity.Slime;

final class RuntimeSlimeTarget {
    final Slime slime;
    final RuntimeSlime runtime;

    RuntimeSlimeTarget(Slime slime, RuntimeSlime runtime) {
        this.slime = slime;
        this.runtime = runtime;
    }
}
