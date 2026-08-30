package dev.lhoopy.hunt;

import gg.cristalix.wada.component.menu.choice.common.ChoiceMenu;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

final class HuntSession {
    final UUID playerId;
    int round;
    int roundToken;
    int hits;
    int misses;
    BukkitTask timeoutTask;
    ChoiceMenu activeMenu;

    HuntSession(UUID playerId) {
        this.playerId = playerId;
    }
}
