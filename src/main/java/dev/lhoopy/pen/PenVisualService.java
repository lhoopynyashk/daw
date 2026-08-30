package dev.lhoopy.pen;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.SlimeDef;
import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import gg.cristalix.wada.transfer.ModTransfer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

final class PenVisualService {
    private static final int REFRESH_ATTEMPTS = 12;
    private static final long REFRESH_INTERVAL_TICKS = 20L;

    private final SlimesPlugin plugin;
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final PenStyleCatalog styleCatalog;

    PenVisualService(SlimesPlugin plugin, ContentRegistry contentRegistry, ProfileService profileService,
                     PenStyleCatalog styleCatalog) {
        this.plugin = plugin;
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.styleCatalog = styleCatalog;
    }

    void refresh(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            return;
        }

        Location center = PenLayout.centerNear(player);
        List<PenSlime> penSlimes = profile.getPenSlimes();
        int visibleCount = Math.min(penSlimes.size(), this.styleCatalog.effectiveCapacity(profile));
        ModTransfer transfer = new ModTransfer().writeInt(visibleCount);
        for (int index = 0; index < visibleCount; index++) {
            SlimeDef definition = this.contentRegistry.getSlime(penSlimes.get(index).getSlimeId());
            if (definition == null) {
                transfer.writeString("unknown").writeDouble(0.0D).writeDouble(0.0D).writeDouble(0.0D).writeInt(index);
                continue;
            }
            Location location = PenLayout.slot(center, index);
            transfer.writeString(definition.getId())
                    .writeDouble(location.getX())
                    .writeDouble(location.getY() + 0.15D)
                    .writeDouble(location.getZ())
                    .writeInt(index);
        }
        transfer.send(PenService.PEN_VISUAL_CHANNEL, player);
    }

    void clear(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        new ModTransfer().writeInt(0).send(PenService.PEN_VISUAL_CHANNEL, player);
    }

    void scheduleRefresh(Player player) {
        scheduleRefresh(player.getUniqueId(), 0);
    }

    private void scheduleRefresh(UUID playerId, int attempt) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                return;
            }
            if (this.profileService.getLoaded(playerId) != null) {
                refresh(player);
                return;
            }
            if (attempt + 1 < REFRESH_ATTEMPTS) {
                scheduleRefresh(playerId, attempt + 1);
            }
        }, REFRESH_INTERVAL_TICKS);
    }
}
