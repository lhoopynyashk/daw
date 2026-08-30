package dev.lhoopy.core.command;

import dev.lhoopy.core.bootstrap.ServiceRegistry;

public final class HuntCommands {
    private final ServiceRegistry services;

    public HuntCommands(ServiceRegistry services) {
        this.services = services;
    }

    public void register(SlimesCommandRouter router) {
        router.register("hunt", (sender, args) -> this.services.huntService().start(sender));
        router.register("hunt1", (sender, args) -> this.services.enginexHuntBridge().start(sender));
        router.register("huntzone", (PlayerCommand) this.services.huntZoneService()::transferToHuntZone);
        router.register("locations", (PlayerCommand) this.services.locationRealmService()::openMenu);
    }
}
