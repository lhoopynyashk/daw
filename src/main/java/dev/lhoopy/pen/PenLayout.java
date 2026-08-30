package dev.lhoopy.pen;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

final class PenLayout {
    private PenLayout() {
    }

    static Location centerNear(Player player) {
        Villager nearest = null;
        double nearestDistance = 64.0D * 64.0D;
        for (Entity entity : player.getWorld().getEntities()) {
            if (!PenNpcMatcher.isPenNpc(entity)) {
                continue;
            }
            double distance = entity.getLocation().distanceSquared(player.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = (Villager) entity;
            }
        }
        Location base = nearest == null ? player.getLocation() : nearest.getLocation();
        return base.clone().add(-4.0D, 0.0D, 0.0D);
    }

    static Location slot(Location center, int index) {
        int column = index % 3;
        int row = index / 3;
        double x = (column - 1) * 1.7D;
        double z = row * 1.7D;
        Location location = center.clone().add(x, 0.0D, z);
        location.setYaw(180.0F);
        location.setPitch(0.0F);
        return location;
    }
}
