package dev.lhoopy.ui.farm;

import dev.lhoopy.ui.common.CristalixMenuService;
import org.bukkit.entity.Player;

public final class FarmMenu {
    private final CristalixMenuService menuService;

    public FarmMenu(CristalixMenuService menuService) {
        this.menuService = menuService;
    }

    public boolean open(Player player) {
        return this.menuService.canOpen(player);
    }
}
