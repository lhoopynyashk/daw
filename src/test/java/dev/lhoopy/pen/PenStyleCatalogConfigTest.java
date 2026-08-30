package dev.lhoopy.pen;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class PenStyleCatalogConfigTest {
    @Test
    void caseContainsEveryNotionBlueprintAndTotalsOneHundredPercent() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("pen-styles.yml");
        assertNotNull(stream);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        ConfigurationSection styles = config.getConfigurationSection("styles");
        assertNotNull(styles);
        assertEquals(30, styles.getKeys(false).size());

        double total = 0.0D;
        int caseEntries = 0;
        for (String id : styles.getKeys(false)) {
            double chance = styles.getDouble(id + ".chance", 0.0D);
            if (chance > 0.0D) {
                total += chance;
                caseEntries++;
            }
        }
        assertEquals(29, caseEntries);
        assertEquals(100.0D, total, 0.0001D);
    }
}
