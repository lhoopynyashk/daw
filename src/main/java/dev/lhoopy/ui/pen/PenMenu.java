package dev.lhoopy.ui.pen;

import dev.lhoopy.ui.common.CristalixMenuService;
import org.bukkit.entity.Player;

public final class PenMenu {
    private final CristalixMenuService menuService;

    public PenMenu(CristalixMenuService menuService) {
        this.menuService = menuService;
    }

    public boolean open(Player player) {
        return this.menuService.canOpen(player);
    }
}
