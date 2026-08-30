package dev.lhoopy.core.command;

import dev.lhoopy.core.bootstrap.ServiceRegistry;

public final class PenCommands {
    private final ServiceRegistry services;

    public PenCommands(ServiceRegistry services) {
        this.services = services;
    }

    public void register(SlimesCommandRouter router) {
        router.register("pencase", this.services.penCaseService()::handleCaseCommand);
        router.register("penstyle", this.services.penCaseService()::handleStyleCommand);
    }
}
