package dev.lhoopy.location;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocationRealmConfigTest {
    @Test
    void allSixRealmsHaveValidUniqueResourceNodes() {
        YamlConfiguration realms = load("location-realms.yml");
        YamlConfiguration locations = load("locations.yml");
        YamlConfiguration resources = load("resources.yml");

        ConfigurationSection realmLocations = realms.getConfigurationSection("locations");
        assertNotNull(realmLocations);
        assertEquals(6, realmLocations.getKeys(false).size());
        assertTrue(realms.getBoolean("slimes.enabled"));
        assertEquals(6, realms.getMapList("slimes.spawn-points").size());

        Set<Integer> realmIds = new HashSet<>();
        for (String locationId : realmLocations.getKeys(false)) {
            ConfigurationSection realmLocation = realmLocations.getConfigurationSection(locationId);
            assertNotNull(realmLocation);
            assertTrue(realmIds.add(realmLocation.getInt("realm-id")), "Duplicate realm id for " + locationId);
            assertNotNull(Material.matchMaterial(realmLocation.getString("icon")), "Unknown icon for " + locationId);

            ConfigurationSection location = locations.getConfigurationSection("locations." + locationId);
            assertNotNull(location, "Missing content location " + locationId);
            Set<String> allowedResources = new HashSet<>(location.getStringList("resources"));
            ConfigurationSection nodes = realmLocation.getConfigurationSection("resource-nodes");
            assertNotNull(nodes, "Missing resource nodes for " + locationId);
            assertTrue(nodes.getKeys(false).size() >= allowedResources.size(),
                    "Every configured resource needs at least one node in " + locationId);

            Set<String> representedResources = new HashSet<>();
            for (String nodeId : nodes.getKeys(false)) {
                ConfigurationSection node = nodes.getConfigurationSection(nodeId);
                assertNotNull(node);
                String resourceId = node.getString("resource-id");
                representedResources.add(resourceId);
                assertTrue(allowedResources.contains(resourceId), resourceId + " is not assigned to " + locationId);
                assertNotNull(resources.getConfigurationSection("resources." + resourceId), "Unknown resource " + resourceId);
                assertNotNull(Material.matchMaterial(node.getString("material")), "Unknown material for " + locationId + "/" + nodeId);
            }
            assertTrue(representedResources.containsAll(allowedResources),
                    "Some resources have no nodes in " + locationId);
        }
    }

    private static YamlConfiguration load(String name) {
        InputStream stream = LocationRealmConfigTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, "Missing test resource " + name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
