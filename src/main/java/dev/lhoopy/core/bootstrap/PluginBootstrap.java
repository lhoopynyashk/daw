package dev.lhoopy.core.bootstrap;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.command.AdminCommands;
import dev.lhoopy.core.command.EconomyCommands;
import dev.lhoopy.core.command.FarmCommands;
import dev.lhoopy.core.command.HuntCommands;
import dev.lhoopy.core.command.SlimesCommandRouter;
import dev.lhoopy.core.command.PenCommands;
import dev.lhoopy.profile.ProfileListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;

public final class PluginBootstrap {
    private final SlimesPlugin plugin;
    private ServiceRegistry services;

    public PluginBootstrap(SlimesPlugin plugin) {
        this.plugin = plugin;
    }

    public void createServices() {
        this.services = new ServiceRegistry(this.plugin);
        this.services.createServices();
    }

    public void enable() {
        if (this.services == null) {
            throw new IllegalStateException("Services were not created before enable");
        }

        this.services.enableServices();
        this.plugin.getServer().getPluginManager().registerEvents(new ProfileListener(this.services.profileService()), this.plugin);
        registerCommands(createCommandRouter());
    }

    public void disable() {
        if (this.services != null) {
            this.services.disableServices();
            this.services = null;
        }
    }

    private SlimesCommandRouter createCommandRouter() {
        SlimesCommandRouter router = new SlimesCommandRouter();
        new HuntCommands(this.services).register(router);
        new FarmCommands(this.services).register(router);
        new EconomyCommands(this.services).register(router);
        new PenCommands(this.services).register(router);
        new AdminCommands(this.services).register(router);
        return router;
    }

    private void registerCommands(CommandExecutor executor) {
        registerCommand("arbyz", executor);
        registerCommand("starter", executor);
        registerCommand("hunt", executor);
        registerCommand("hunt1", executor);
        registerCommand("huntzone", executor);
        registerCommand("locations", executor);
        registerCommand("sosat", executor);
        registerCommand("ranch", executor);
        registerCommand("visit", executor);
        registerCommand("farm", executor);
        registerCommand("slimefood", executor);
        registerCommand("music", executor);
        registerCommand("storage", executor);
        registerCommand("vacpack", executor);
        registerCommand("crafting", executor);
        registerCommand("farmertable", executor);
        registerCommand("plorts", executor);
        registerCommand("sellterminal", executor);
        registerCommand("penfeed", executor);
        registerCommand("pencase", executor);
        registerCommand("penstyle", executor);
        registerCommand("battlepass", executor);
        registerCommand("bp", executor);
        registerCommand("slimesadmin", executor);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = this.plugin.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command is missing from plugin.yml: " + name);
        }
        command.setExecutor(executor);
    }
}
