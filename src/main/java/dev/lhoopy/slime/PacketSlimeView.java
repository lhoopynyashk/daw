package dev.lhoopy.slime;

import dev.lhoopy.content.SlimeDef;
import net.minecraft.server.v1_12_R1.EntitySlime;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityLiving;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

final class PacketSlimeView {
    PacketSlime spawn(Player player, SlimeDef definition, SlimeState state, Location location) {
        return spawn(player, definition, state, location, PacketSlimePurpose.HUNT, -1);
    }

    PacketSlime spawn(Player player, SlimeDef definition, SlimeState state, Location location, PacketSlimePurpose purpose, int penIndex) {
        EntitySlime entity = createEntity(player, definition, state, location);
        send(player, new PacketPlayOutSpawnEntityLiving(entity));
        send(player, new PacketPlayOutEntityMetadata(entity.getId(), entity.getDataWatcher(), true));
        send(player, new PacketPlayOutEntityTeleport(entity));
        return new PacketSlime(entity.getId(), entity.getUniqueID(), player.getUniqueId(), definition, state, location, purpose, penIndex);
    }

    void destroy(Player player, PacketSlime slime) {
        send(player, new PacketPlayOutEntityDestroy(slime.getEntityId()));
    }

    void refreshMetadata(Player player, PacketSlime slime) {
        EntitySlime entity = createEntity(player, slime.getDefinition(), slime.getState(), slime.getLocation());
        setEntityId(entity, slime.getEntityId());
        send(player, new PacketPlayOutEntityMetadata(slime.getEntityId(), entity.getDataWatcher(), true));
    }

    void teleport(Player player, PacketSlime slime) {
        Location location = slime.getLocation();
        PacketPlayOutEntityTeleport packet = new PacketPlayOutEntityTeleport();
        packet.a = slime.getEntityId();
        packet.b = location.getX();
        packet.c = location.getY();
        packet.d = location.getZ();
        packet.e = angleToByte(location.getYaw());
        packet.f = angleToByte(location.getPitch());
        packet.g = true;
        send(player, packet);
    }

    private EntitySlime createEntity(Player player, SlimeDef definition, SlimeState state, Location location) {
        EntitySlime entity = new EntitySlime(((CraftWorld) player.getWorld()).getHandle());
        entity.setSize(definition.getSize(), false);
        entity.setNoAI(true);
        entity.setNoGravity(true);
        entity.setHealth(1.0F);
        entity.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        entity.setCustomName(displayName(definition, state));
        entity.setCustomNameVisible(true);
        return entity;
    }

    private static String displayName(SlimeDef definition, SlimeState state) {
        String stateText = state == SlimeState.INTERESTED
                ? ChatColor.YELLOW + "заинтересован"
                : ChatColor.GRAY + "обычный";
        return ChatColor.GREEN + definition.getDisplayName() + ChatColor.DARK_GRAY + " [" + stateText + ChatColor.DARK_GRAY + "]";
    }

    private static void send(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    private static byte angleToByte(float angle) {
        return (byte) ((int) (angle * 256.0F / 360.0F));
    }

    private static void setEntityId(EntitySlime entity, int entityId) {
        try {
            java.lang.reflect.Field field = net.minecraft.server.v1_12_R1.Entity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.setInt(entity, entityId);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
