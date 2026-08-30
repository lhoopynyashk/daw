package dev.lhoopy.slime;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

final class PacketSlimeMovementService {
    static final long MOVEMENT_PERIOD_TICKS = 30L;

    private static final double ACTIVE_DISTANCE = 42.0D;
    private static final double ACTIVE_DISTANCE_SQUARED = ACTIVE_DISTANCE * ACTIVE_DISTANCE;
    private static final double NORMAL_WANDER_RADIUS = 4.0D;
    private static final double INTERESTED_RADIUS_MIN = 1.8D;
    private static final double INTERESTED_RADIUS_MAX = 3.2D;

    private final PacketSlimeView view;
    private final Consumer<UUID> interestResetCanceller;

    PacketSlimeMovementService(PacketSlimeView view, Consumer<UUID> interestResetCanceller) {
        this.view = view;
        this.interestResetCanceller = interestResetCanceller;
    }

    void tick(List<PacketSlime> slimes) {
        long now = System.currentTimeMillis();
        Iterator<PacketSlime> iterator = slimes.iterator();
        while (iterator.hasNext()) {
            PacketSlime slime = iterator.next();
            Player viewer = Bukkit.getPlayer(slime.getViewerId());
            if (viewer == null || !viewer.isOnline()) {
                this.interestResetCanceller.accept(slime.getUniqueId());
                iterator.remove();
                continue;
            }
            if (!slime.getLocation().getWorld().equals(viewer.getWorld())) {
                this.view.destroy(viewer, slime);
                this.interestResetCanceller.accept(slime.getUniqueId());
                iterator.remove();
                continue;
            }
            if (slime.getLocation().distanceSquared(viewer.getLocation()) > ACTIVE_DISTANCE_SQUARED) {
                continue;
            }
            if (slime.getNextMoveAtMillis() > now) {
                continue;
            }

            slime.setLocation(nextLocation(viewer, slime));
            this.view.teleport(viewer, slime);
            slime.setNextMoveAtMillis(now + nextMoveDelayMillis(slime.getState()));
        }
    }

    private Location nextLocation(Player viewer, PacketSlime slime) {
        if (slime.getState() == SlimeState.INTERESTED) {
            return interestedLocation(viewer, slime);
        }
        return normalLocation(slime);
    }

    private Location normalLocation(PacketSlime slime) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2.0D);
        double radius = random.nextDouble(0.8D, NORMAL_WANDER_RADIUS);
        Location origin = slime.getOrigin();
        Location current = slime.getLocation();
        Location next = origin.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
        next.setY(origin.getY());
        next.setYaw(yawTo(current, next));
        next.setPitch(0.0F);
        return next;
    }

    private Location interestedLocation(Player viewer, PacketSlime slime) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2.0D);
        double radius = random.nextDouble(INTERESTED_RADIUS_MIN, INTERESTED_RADIUS_MAX);
        Location playerLocation = viewer.getLocation();
        Location next = playerLocation.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
        next.setY(slime.getOrigin().getY() + random.nextDouble(0.0D, 0.18D));
        next.setYaw(yawTo(next, playerLocation));
        next.setPitch(0.0F);
        return next;
    }

    private long nextMoveDelayMillis(SlimeState state) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (state == SlimeState.INTERESTED) {
            return random.nextLong(650L, 1201L);
        }
        return random.nextLong(1400L, 2601L);
    }

    private static float yawTo(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
