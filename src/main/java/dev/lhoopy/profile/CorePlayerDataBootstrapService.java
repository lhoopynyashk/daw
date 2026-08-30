package dev.lhoopy.profile;

import dev.lhoopy.core.lifecycle.PluginService;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.data.IPlayerDataService;
import ru.cristalix.core.data.PlayerDataService;
import ru.cristalix.core.data.listener.PlayerChangeConnectionStateListener;
import ru.cristalix.core.database.nosql.mongo.MongoDatabase;

public final class CorePlayerDataBootstrapService implements PluginService {
    private static final String ENV_MONGO_URI = "SLIMES_MONGO_URI";
    private static final String ENV_COLLECTION_REALM = "SLIMES_PLAYERDATA_REALM";
    private static final String DEFAULT_COLLECTION_REALM = "SLIMES";

    private final Plugin plugin;
    private MongoDatabase database;
    private Listener connectionStateListener;
    private boolean registered;

    public CorePlayerDataBootstrapService(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        if (CoreApi.get().getService(IPlayerDataService.class) != null) {
            this.plugin.getLogger().info("Core PlayerData service is already registered.");
            return;
        }

        String uri = System.getenv(ENV_MONGO_URI);
        if (uri == null || uri.trim().isEmpty()) {
            this.plugin.getLogger().warning("SLIMES_MONGO_URI is not configured. Player profiles will use local YAML fallback.");
            return;
        }

        String collectionRealm = System.getenv(ENV_COLLECTION_REALM);
        if (collectionRealm == null || collectionRealm.trim().isEmpty()) {
            collectionRealm = DEFAULT_COLLECTION_REALM;
        }

        try {
            this.database = new MongoDatabase();
            this.database.connect(uri).join();

            PlayerDataService service = new PlayerDataService(this.database, collectionRealm);
            CoreApi.get().registerService(IPlayerDataService.class, service);
            this.connectionStateListener = new PlayerChangeConnectionStateListener(service);
            Bukkit.getPluginManager().registerEvents(this.connectionStateListener, this.plugin);
            this.registered = true;
            this.plugin.getLogger().info("Registered SlimeRancher Core PlayerData service: " + service.getCollectionName());
        } catch (RuntimeException error) {
            this.plugin.getLogger().warning("Could not initialize SlimeRancher Core PlayerData service: " + error.getMessage());
            this.database = null;
            this.registered = false;
        }
    }

    @Override
    public void shutdown() {
        if (this.connectionStateListener != null) {
            HandlerList.unregisterAll(this.connectionStateListener);
            this.connectionStateListener = null;
        }
        if (this.registered) {
            try {
                CoreApi.get().unregisterService(IPlayerDataService.class);
            } catch (RuntimeException ignored) {
            }
            this.registered = false;
        }
        if (this.database != null && this.database.isConnected()) {
            this.database.disconnect();
            this.database = null;
        }
    }
}
