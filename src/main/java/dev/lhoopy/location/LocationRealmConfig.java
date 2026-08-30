package dev.lhoopy.location;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.LocationDef;
import dev.lhoopy.content.ResourceDef;
import dev.lhoopy.core.SlimesPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class LocationRealmConfig {
    private static final String FILE_NAME = "location-realms.yml";

    private final boolean enabled;
    private final String realmType;
    private final int maxPlayers;
    private final int platformY;
    private final int platformRadius;
    private final boolean buildPlatform;
    private final NpcSettings npc;
    private final SlimeSettings slimes;
    private final Map<String, RealmLocation> byLocationId;
    private final Map<Integer, RealmLocation> byRealmId;

    private LocationRealmConfig(
            boolean enabled,
            String realmType,
            int maxPlayers,
            int platformY,
            int platformRadius,
            boolean buildPlatform,
            NpcSettings npc,
            SlimeSettings slimes,
            Map<String, RealmLocation> byLocationId,
            Map<Integer, RealmLocation> byRealmId
    ) {
        this.enabled = enabled;
        this.realmType = realmType;
        this.maxPlayers = maxPlayers;
        this.platformY = platformY;
        this.platformRadius = platformRadius;
        this.buildPlatform = buildPlatform;
        this.npc = npc;
        this.slimes = slimes;
        this.byLocationId = Collections.unmodifiableMap(byLocationId);
        this.byRealmId = Collections.unmodifiableMap(byRealmId);
    }

    static LocationRealmConfig load(SlimesPlugin plugin, ContentRegistry contentRegistry) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.isFile()) {
            plugin.saveResource(FILE_NAME, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String realmType = config.getString("realm-type", "THUT").trim();
        Map<String, RealmLocation> byLocation = new LinkedHashMap<>();
        Map<Integer, RealmLocation> byRealm = new LinkedHashMap<>();
        ConfigurationSection locations = config.getConfigurationSection("locations");
        if (locations == null) {
            throw new IllegalStateException(FILE_NAME + " has no locations section");
        }

        for (String rawLocationId : locations.getKeys(false)) {
            String locationId = normalize(rawLocationId);
            LocationDef contentLocation = contentRegistry.getLocation(locationId);
            if (contentLocation == null || contentLocation.getTier() <= 0) {
                throw new IllegalStateException("Unknown hunting location in " + FILE_NAME + ": " + locationId);
            }
            ConfigurationSection location = locations.getConfigurationSection(rawLocationId);
            if (location == null) {
                continue;
            }
            int realmId = location.getInt("realm-id", contentLocation.getTier());
            Material icon = material(location.getString("icon", "COMPASS"), Material.COMPASS);
            List<ResourceNode> nodes = loadNodes(contentLocation, location, contentRegistry);
            RealmLocation definition = new RealmLocation(contentLocation, realmId, icon, nodes);
            if (byRealm.put(realmId, definition) != null) {
                throw new IllegalStateException("Duplicate realm-id " + realmId + " in " + FILE_NAME);
            }
            byLocation.put(locationId, definition);
        }

        if (byLocation.size() != 6) {
            throw new IllegalStateException(FILE_NAME + " must define exactly six hunting locations, found " + byLocation.size());
        }

        NpcSettings npc = new NpcSettings(
                config.getBoolean("npc.enabled", true),
                config.getDouble("npc.x", 12.5D),
                config.getDouble("npc.y", 91.0D),
                config.getDouble("npc.z", 0.5D),
                (float) config.getDouble("npc.yaw", 270.0D),
                config.getString("npc.name", "§bВыбор локации")
        );
        return new LocationRealmConfig(
                config.getBoolean("enabled", true),
                realmType,
                Math.max(1, config.getInt("max-players", 25)),
                config.getInt("platform.y", 90),
                Math.max(5, config.getInt("platform.radius", 24)),
                config.getBoolean("platform.build-fallback", true),
                npc,
                loadSlimeSettings(config),
                byLocation,
                byRealm
        );
    }

    private static SlimeSettings loadSlimeSettings(YamlConfiguration config) {
        List<SlimeSpawnPoint> points = new ArrayList<>();
        for (Map<?, ?> raw : config.getMapList("slimes.spawn-points")) {
            points.add(new SlimeSpawnPoint(
                    number(raw.get("x"), 0.0D),
                    number(raw.get("y"), 90.0D),
                    number(raw.get("z"), 0.0D)
            ));
        }
        if (points.isEmpty()) {
            points.add(new SlimeSpawnPoint(-16.0D, 90.0D, 16.0D));
            points.add(new SlimeSpawnPoint(-9.0D, 90.0D, 15.0D));
            points.add(new SlimeSpawnPoint(-3.0D, 90.0D, 17.0D));
            points.add(new SlimeSpawnPoint(4.0D, 90.0D, 16.0D));
            points.add(new SlimeSpawnPoint(10.0D, 90.0D, 14.0D));
            points.add(new SlimeSpawnPoint(16.0D, 90.0D, 17.0D));
        }
        return new SlimeSettings(
                config.getBoolean("slimes.enabled", true),
                Math.max(5L, config.getLong("slimes.respawn-seconds", 45L)),
                Collections.unmodifiableList(points)
        );
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private static List<ResourceNode> loadNodes(LocationDef contentLocation, ConfigurationSection location, ContentRegistry contentRegistry) {
        String locationId = contentLocation.getId();
        ConfigurationSection nodes = location.getConfigurationSection("resource-nodes");
        if (nodes == null) {
            throw new IllegalStateException("Location " + locationId + " has no resource-nodes");
        }
        List<ResourceNode> result = new ArrayList<>();
        for (String nodeId : nodes.getKeys(false)) {
            ConfigurationSection node = nodes.getConfigurationSection(nodeId);
            if (node == null) {
                continue;
            }
            String resourceId = normalize(node.getString("resource-id", nodeId));
            ResourceDef resource = contentRegistry.getResource(resourceId);
            if (resource == null) {
                throw new IllegalStateException("Unknown resource " + resourceId + " in node " + locationId + "/" + nodeId);
            }
            if (!contentLocation.getResourceIds().contains(resourceId)) {
                throw new IllegalStateException("Resource " + resourceId + " is not listed for " + locationId + " in locations.yml");
            }
            result.add(new ResourceNode(
                    normalize(nodeId),
                    resource,
                    node.getInt("x"),
                    node.getInt("y", 90),
                    node.getInt("z"),
                    material(node.getString("material", "GLOWSTONE"), Material.GLOWSTONE),
                    (byte) node.getInt("data", 0),
                    Math.max(1, node.getInt("amount", 1)),
                    Math.max(1L, node.getLong("respawn-seconds", 60L))
            ));
        }
        return Collections.unmodifiableList(result);
    }

    private static Material material(String name, Material fallback) {
        Material material = Material.matchMaterial(name == null ? "" : name.toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    boolean isEnabled() {
        return this.enabled;
    }

    String getRealmType() {
        return this.realmType;
    }

    int getPlatformY() {
        return this.platformY;
    }

    int getMaxPlayers() {
        return this.maxPlayers;
    }

    int getPlatformRadius() {
        return this.platformRadius;
    }

    boolean shouldBuildPlatform() {
        return this.buildPlatform;
    }

    NpcSettings getNpc() {
        return this.npc;
    }

    SlimeSettings getSlimes() {
        return this.slimes;
    }

    Collection<RealmLocation> getLocations() {
        return this.byLocationId.values();
    }

    RealmLocation getByRealmId(int realmId) {
        return this.byRealmId.get(realmId);
    }

    static final class RealmLocation {
        private final LocationDef content;
        private final int realmId;
        private final Material icon;
        private final List<ResourceNode> nodes;

        private RealmLocation(LocationDef content, int realmId, Material icon, List<ResourceNode> nodes) {
            this.content = content;
            this.realmId = realmId;
            this.icon = icon;
            this.nodes = nodes;
        }

        LocationDef getContent() {
            return this.content;
        }

        int getRealmId() {
            return this.realmId;
        }

        Material getIcon() {
            return this.icon;
        }

        List<ResourceNode> getNodes() {
            return this.nodes;
        }
    }

    static final class ResourceNode {
        private final String id;
        private final ResourceDef resource;
        private final int x;
        private final int y;
        private final int z;
        private final Material material;
        private final byte data;
        private final int amount;
        private final long respawnSeconds;

        private ResourceNode(String id, ResourceDef resource, int x, int y, int z, Material material, byte data, int amount, long respawnSeconds) {
            this.id = id;
            this.resource = resource;
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
            this.data = data;
            this.amount = amount;
            this.respawnSeconds = respawnSeconds;
        }

        String getId() { return this.id; }
        ResourceDef getResource() { return this.resource; }
        int getX() { return this.x; }
        int getY() { return this.y; }
        int getZ() { return this.z; }
        Material getMaterial() { return this.material; }
        byte getData() { return this.data; }
        int getAmount() { return this.amount; }
        long getRespawnSeconds() { return this.respawnSeconds; }
    }

    static final class NpcSettings {
        private final boolean enabled;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final String name;

        private NpcSettings(boolean enabled, double x, double y, double z, float yaw, String name) {
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.name = name;
        }

        boolean isEnabled() { return this.enabled; }
        double getX() { return this.x; }
        double getY() { return this.y; }
        double getZ() { return this.z; }
        float getYaw() { return this.yaw; }
        String getName() { return this.name; }
    }

    static final class SlimeSettings {
        private final boolean enabled;
        private final long respawnSeconds;
        private final List<SlimeSpawnPoint> spawnPoints;

        private SlimeSettings(boolean enabled, long respawnSeconds, List<SlimeSpawnPoint> spawnPoints) {
            this.enabled = enabled;
            this.respawnSeconds = respawnSeconds;
            this.spawnPoints = spawnPoints;
        }

        boolean isEnabled() { return this.enabled; }
        long getRespawnSeconds() { return this.respawnSeconds; }
        List<SlimeSpawnPoint> getSpawnPoints() { return this.spawnPoints; }
    }

    static final class SlimeSpawnPoint {
        private final double x;
        private final double y;
        private final double z;

        private SlimeSpawnPoint(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        double getX() { return this.x; }
        double getY() { return this.y; }
        double getZ() { return this.z; }
    }
}
