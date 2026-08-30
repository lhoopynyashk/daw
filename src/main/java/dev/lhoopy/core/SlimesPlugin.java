package dev.lhoopy.core;

import dev.lhoopy.core.bootstrap.PluginBootstrap;
import dev.lhoopy.world.VoidChunkGenerator;
import gg.cristalix.wada.Wada;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public final class SlimesPlugin extends JavaPlugin {
    private PluginBootstrap bootstrap;

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        String realmType = System.getenv("REALM_TYPE");
        if (realmType != null && realmType.equalsIgnoreCase("THUT")) {
            return new VoidChunkGenerator();
        }
        return null;
    }

    @Override
    public void onEnable() {
        Wada.initialize(this);
        saveDefaultConfig();
        getLogger().info("SlimeRancher build marker: ranch-world-v1");

        this.bootstrap = new PluginBootstrap(this);
        this.bootstrap.createServices();
        this.bootstrap.enable();

        getLogger().info("SlimeRancher enabled");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.disable();
            this.bootstrap = null;
        }

        getLogger().info("SlimeRancher disabled");
    }
}
