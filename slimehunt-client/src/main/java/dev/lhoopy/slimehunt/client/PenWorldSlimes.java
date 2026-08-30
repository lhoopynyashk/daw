package dev.lhoopy.slimehunt.client;

import dev.xdark.clientapi.resource.ResourceLocation;
import gg.cristalix.enginex.Enginex;
import gg.cristalix.enginex.color.palette.Palette;
import gg.cristalix.enginex.element.Container;
import gg.cristalix.enginex.element.Image;
import gg.cristalix.enginex.event.tick.PreTickEvent;
import gg.cristalix.enginex.math.V3;
import gg.cristalix.enginex.render.context.type.WorldContext;

import java.util.ArrayList;
import java.util.List;

final class PenWorldSlimes {
    private static final List<Container> CURRENT = new ArrayList<>();
    private static ResourceLocation pinkSlimeTexture;

    private PenWorldSlimes() {
    }

    /** Читает пакет slimehunt:pen_visual. */
    static SlimeVisual[] read(gg.cristalix.enginex.transfer.ModTransfer transfer) {
        int count = transfer.readInt();
        SlimeVisual[] visuals = new SlimeVisual[Math.max(0, count)];
        for (int index = 0; index < count; index++) {
            visuals[index] = new SlimeVisual(
                    transfer.readString(),
                    transfer.readDouble(),
                    transfer.readDouble(),
                    transfer.readDouble(),
                    transfer.readInt()
            );
        }
        return visuals;
    }

    static void replace(SlimeVisual[] visuals) {
        clear();
        if (visuals == null || visuals.length == 0) {
            return;
        }

        for (SlimeVisual visual : visuals) {
            if (visual == null || "unknown".equalsIgnoreCase(visual.slimeId)) {
                continue;
            }
            Container slime = createSlime(visual);
            CURRENT.add(slime);
            WorldContext.add(slime);
        }
    }

    static void clear() {
        if (CURRENT.isEmpty()) {
            return;
        }
        WorldContext.remove(CURRENT.toArray(new Container[0]));
        CURRENT.clear();
    }

    private static Container createSlime(SlimeVisual visual) {
        Container root = new Container();
        root.setSize(0.95D, 0.95D, 0.0D);
        root.setPos(visual.x, visual.y, visual.z);
        root.setOrigin(0.5D, 0.5D, 0.0D);
        root.setEnableFrustumCulling(true);
        root.setWorldRenderDistance(36.0D);

        Image image = new Image();
        image.setTexture(pinkSlimeTexture());
        image.setColor(Palette.WHITE);
        image.setSize(0.95D, 0.95D, 0.0D);
        image.setPos(-0.475D, 0.0D, 0.0D);
        image.setSkipRenderIfTextureNotLoaded(true);
        root.addChild(image);

        long seed = 613L + visual.index * 97L;
        root.registerEvent(PreTickEvent.class, event -> animate(root, visual, seed));
        return root;
    }

    private static void animate(Container root, SlimeVisual visual, long seed) {
        long now = System.currentTimeMillis();
        double phase = (now + seed) / 420.0D;
        double bounce = Math.abs(Math.sin(phase)) * 0.13D;
        double squash = 1.0D + Math.sin(phase) * 0.035D;

        V3 camera = Enginex.getGameSettings().getCameraPosition();
        double dx = camera.getX() - visual.x;
        double dz = camera.getZ() - visual.z;
        double yaw = Math.toDegrees(Math.atan2(dx, dz));

        root.setPos(visual.x, visual.y + bounce, visual.z);
        root.setRotation(0.0D, yaw, 0.0D);
        root.setScale(squash, 1.0D / Math.max(0.92D, squash), 1.0D);
    }

    private static ResourceLocation pinkSlimeTexture() {
        if (pinkSlimeTexture == null) {
            pinkSlimeTexture = Enginex.getTextureManager().loadTextureFromJar(
                    "slimehunt",
                    "textures/slimes/pink_world",
                    "assets/slimehunt/textures/slimes/pink.png"
            );
        }
        return pinkSlimeTexture;
    }

    static final class SlimeVisual {
        final String slimeId;
        final double x;
        final double y;
        final double z;
        final int index;

        SlimeVisual(String slimeId, double x, double y, double z, int index) {
            this.slimeId = slimeId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.index = index;
        }
    }
}
