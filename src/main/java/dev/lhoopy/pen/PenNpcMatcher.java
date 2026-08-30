package dev.lhoopy.pen;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

final class PenNpcMatcher {
    private PenNpcMatcher() {
    }

    static boolean isPenNpc(Entity entity) {
        if (!(entity instanceof Villager)) {
            return false;
        }
        if (entity.getScoreboardTags().contains(PenService.NPC_TAG)) {
            return true;
        }
        String customName = entity.getCustomName();
        return customName != null && PenService.NPC_PLAIN_NAME.equals(ChatColor.stripColor(customName));
    }
}
