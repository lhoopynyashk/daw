package dev.lhoopy.core.command;

import dev.lhoopy.core.bootstrap.ServiceRegistry;

public final class EconomyCommands {
    private final ServiceRegistry services;

    public EconomyCommands(ServiceRegistry services) {
        this.services = services;
    }

    public void register(SlimesCommandRouter router) {
        router.register("plorts", (PlayerCommand) this.services.economyService()::handlePlortsCommand);
        router.register("sellterminal", (PlayerCommand) this.services.economyService()::handleSellTerminalCommand);
    }
}
