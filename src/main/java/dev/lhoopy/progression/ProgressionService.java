package dev.lhoopy.progression;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;

public final class ProgressionService implements PluginService {
    private final SlimesPlugin plugin;
    private final UnlockService unlockService = new UnlockService();
    private final SkillTreeService skillTreeService = new SkillTreeService();
    private final RebirthService rebirthService = new RebirthService();

    public ProgressionService(SlimesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        this.plugin.getLogger().info("Progression service enabled");
    }

    @Override
    public void shutdown() {
        this.plugin.getLogger().info("Progression service disabled");
    }

    public UnlockService unlocks() {
        return this.unlockService;
    }

    public SkillTreeService skillTree() {
        return this.skillTreeService;
    }

    public RebirthService rebirths() {
        return this.rebirthService;
    }
}
