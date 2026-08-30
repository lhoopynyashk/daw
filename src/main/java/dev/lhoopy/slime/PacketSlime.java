package dev.lhoopy.slime;

import dev.lhoopy.content.SlimeDef;
import org.bukkit.Location;

import java.util.UUID;

final class PacketSlime {
    private final int entityId;
    private final UUID uniqueId;
    private final UUID viewerId;
    private final SlimeDef definition;
    private final PacketSlimePurpose purpose;
    private final int penIndex;
    private SlimeState state;
    private final Location origin;
    private Location location;
    private long nextMoveAtMillis;

    PacketSlime(int entityId, UUID uniqueId, UUID viewerId, SlimeDef definition, SlimeState state, Location location) {
        this(entityId, uniqueId, viewerId, definition, state, location, PacketSlimePurpose.HUNT, -1);
    }

    PacketSlime(int entityId, UUID uniqueId, UUID viewerId, SlimeDef definition, SlimeState state, Location location, PacketSlimePurpose purpose, int penIndex) {
        this.entityId = entityId;
        this.uniqueId = uniqueId;
        this.viewerId = viewerId;
        this.definition = definition;
        this.purpose = purpose;
        this.penIndex = penIndex;
        this.state = state;
        this.origin = location.clone();
        this.location = location.clone();
    }

    int getEntityId() {
        return this.entityId;
    }

    UUID getUniqueId() {
        return this.uniqueId;
    }

    UUID getViewerId() {
        return this.viewerId;
    }

    SlimeDef getDefinition() {
        return this.definition;
    }

    PacketSlimePurpose getPurpose() {
        return this.purpose;
    }

    int getPenIndex() {
        return this.penIndex;
    }

    SlimeState getState() {
        return this.state;
    }

    void setState(SlimeState state) {
        this.state = state;
    }

    Location getLocation() {
        return this.location.clone();
    }

    void setLocation(Location location) {
        this.location = location.clone();
    }

    Location getOrigin() {
        return this.origin.clone();
    }

    long getNextMoveAtMillis() {
        return this.nextMoveAtMillis;
    }

    void setNextMoveAtMillis(long nextMoveAtMillis) {
        this.nextMoveAtMillis = nextMoveAtMillis;
    }
}
