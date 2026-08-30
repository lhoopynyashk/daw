package dev.lhoopy.ui.sell;

import dev.lhoopy.ui.common.CristalixMenuService;
import org.bukkit.entity.Player;

public final class SellTerminalMenu {
    private final CristalixMenuService menuService;

    public SellTerminalMenu(CristalixMenuService menuService) {
        this.menuService = menuService;
    }

    public boolean open(Player player) {
        return this.menuService.canOpen(player);
    }
}
