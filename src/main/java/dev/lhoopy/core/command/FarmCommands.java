package dev.lhoopy.core.command;

import dev.lhoopy.core.bootstrap.ServiceRegistry;
import dev.lhoopy.crafting.CraftingCommand;

public final class FarmCommands {
    private final ServiceRegistry services;

    public FarmCommands(ServiceRegistry services) {
        this.services = services;
    }

    public void register(SlimesCommandRouter router) {
        StorageCommand storageCommand = new StorageCommand(this.services.storageService());
        VacpackCommand vacpackCommand = new VacpackCommand(this.services.storageService());
        MusicCommand musicCommand = new MusicCommand(this.services.soundtrackService());
        StarterCommand starterCommand = new StarterCommand(this.services.contentRegistry(), this.services.profileService());
        CraftingCommand craftingCommand = new CraftingCommand(
                this.services.contentRegistry(),
                this.services.profileService(),
                this.services.craftingService()
        );

        router.register("arbyz", (sender, args) -> sender.sendMessage("Арбуз"));
        router.register("starter", starterCommand::handle);
        router.register("sosat", (sender, args) -> this.services.slimeService().giveVacuum(sender));
        router.register("ranch", this::returnToRanch);
        router.register("visit", this.services.farmService()::visit);
        router.register("farm", this.services.farmService()::handleCommand);
        router.register("slimefood", this.services.slimeService()::giveFavoriteFood);
        router.register("music", musicCommand::handle);
        router.register("storage", storageCommand::handle);
        router.register("vacpack", vacpackCommand::handle);
        router.register("crafting", craftingCommand::handle);
        router.register("farmertable", this.services.farmerTableMenuService()::open);
        router.register("penfeed", this.services.penService()::handleFeedCommand);
        router.register("battlepass", this.services.questService()::handleBattlePassCommand);
        router.register("bp", this.services.questService()::handleBattlePassCommand);
    }

    private void returnToRanch(org.bukkit.command.CommandSender sender, String[] args) {
        if (this.services.locationRealmService().returnToRanchIfInLocation(sender)) {
            return;
        }
        if (!this.services.huntZoneService().returnToRanchIfInHuntZone(sender)) {
            this.services.farmService().teleportHome(sender);
        }
    }
}
