package dev.lhoopy.realm;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.location.LocationRealmService;
import org.bukkit.Bukkit;
import ru.cristalix.core.realm.IRealmService;
import ru.cristalix.core.realm.RealmInfo;
import ru.cristalix.core.realm.RealmStatus;

public final class RealmMetadataService implements PluginService {
    private static final String CONFIG_ENABLED = "realm-metadata.enabled";
    private static final String CONFIG_READABLE_NAME = "realm-metadata.readable-name";
    private static final String CONFIG_MAX_PLAYERS = "realm-metadata.max-players";
    private static final String CONFIG_REFRESH_SECONDS = "realm-metadata.refresh-seconds";

    private final SlimesPlugin plugin;
    private final LocationRealmService locationRealmService;
    private int taskId = -1;

    public RealmMetadataService(SlimesPlugin plugin, LocationRealmService locationRealmService) {
        this.plugin = plugin;
        this.locationRealmService = locationRealmService;
    }

    @Override
    public void enable() {
        if (!this.plugin.getConfig().getBoolean(CONFIG_ENABLED, true)) {
            this.plugin.getLogger().info("Realm metadata updates disabled");
            return;
        }

        updateMetadata();
        long refreshTicks = Math.max(5L, this.plugin.getConfig().getLong(CONFIG_REFRESH_SECONDS, 30L)) * 20L;
        this.taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, this::updateMetadata, refreshTicks, refreshTicks).getTaskId();
        this.plugin.getLogger().info("Realm metadata updates enabled");
    }

    @Override
    public void shutdown() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
            this.taskId = -1;
        }
    }

    private void updateMetadata() {
        try {
            IRealmService realmService = IRealmService.get();
            if (realmService == null) {
                return;
            }
            RealmInfo realmInfo = realmService.getCurrentRealmInfo();
            if (realmInfo == null) {
                return;
            }

            String readableName = resolveReadableName();
            int maxPlayers = this.locationRealmService.isCurrentLocationRealm()
                    ? this.locationRealmService.currentRealmMaxPlayers()
                    : Math.max(1, this.plugin.getConfig().getInt(CONFIG_MAX_PLAYERS, 20));
            realmInfo.setReadableName(readableName);
            realmInfo.setMaxPlayers(maxPlayers);
            realmInfo.setStatus(RealmStatus.WAITING_FOR_PLAYERS);
            realmService.update();
        } catch (NoClassDefFoundError | Exception exception) {
            this.plugin.getLogger().warning("Could not update realm metadata: " + exception.getMessage());
        }
    }

    private String resolveReadableName() {
        String locationName = this.locationRealmService.currentLocationDisplayName();
        if (locationName != null) {
            return "SlimeRancher - " + locationName;
        }
        String readableName = this.plugin.getConfig().getString(CONFIG_READABLE_NAME, "SlimeRancher");
        if ("Slimes".equalsIgnoreCase(readableName) || "Slimes Test".equalsIgnoreCase(readableName)) {
            return "SlimeRancher";
        }
        return readableName;
    }
}
