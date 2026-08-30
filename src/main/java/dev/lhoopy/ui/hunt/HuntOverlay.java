package dev.lhoopy.ui.hunt;

import dev.lhoopy.ui.common.CristalixMenuService;
import org.bukkit.entity.Player;

public final class HuntOverlay {
    private final CristalixMenuService menuService;

    public HuntOverlay(CristalixMenuService menuService) {
        this.menuService = menuService;
    }

    public boolean show(Player player) {
        return this.menuService.canOpen(player);
    }
}
