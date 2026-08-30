package dev.lhoopy.quest;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.profile.ProfileService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class QuestService implements PluginService {
    private final SlimesPlugin plugin;
    private final QuestProgressService progressService = new QuestProgressService();
    private final BattlePassService battlePassService;

    public QuestService(SlimesPlugin plugin, ProfileService profileService) {
        this.plugin = plugin;
        this.battlePassService = new BattlePassService(plugin, profileService);
    }

    @Override
    public void enable() {
        this.battlePassService.enable();
        this.plugin.getLogger().info("Quest service enabled");
    }

    @Override
    public void shutdown() {
        this.battlePassService.shutdown();
        this.plugin.getLogger().info("Quest service disabled");
    }

    public QuestProgressService progress() {
        return this.progressService;
    }

    public BattlePassService battlePass() {
        return this.battlePassService;
    }

    public void handleBattlePassCommand(Player sender, String[] args) {
        this.battlePassService.handleCommand(sender, args);
    }
}
