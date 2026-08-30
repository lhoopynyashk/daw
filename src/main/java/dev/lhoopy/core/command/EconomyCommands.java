package dev.lhoopy.core.command;

import dev.lhoopy.core.bootstrap.ServiceRegistry;

public final class EconomyCommands {
    private final ServiceRegistry services;

    public EconomyCommands(ServiceRegistry services) {
        this.services = services;
    }

    public void register(SlimesCommandRouter router) {
        router.register("plorts", this.services.economyService()::handlePlortsCommand);
        router.register("sellterminal", this.services.economyService()::handleSellTerminalCommand);
    }
}
