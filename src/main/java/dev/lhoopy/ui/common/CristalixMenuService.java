package dev.lhoopy.ui.common;

import org.bukkit.entity.Player;

public final class CristalixMenuService {
    public boolean canOpen(Player player) {
        return player != null && player.isOnline();
    }
}
