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
        router.register("starter", (PlayerCommand) starterCommand::handle);
        router.register("sosat", (sender, args) -> this.services.slimeService().giveVacuum(sender));
        router.register("ranch", this::returnToRanch);
        router.register("visit", (PlayerCommand) this.services.farmService()::visit);
        router.register("farm", (PlayerCommand) this.services.farmService()::handleCommand);
        router.register("slimefood", (PlayerCommand) this.services.slimeService()::giveFavoriteFood);
        router.register("music", (PlayerCommand) musicCommand::handle);
        router.register("storage", (PlayerCommand) storageCommand::handle);
        router.register("vacpack", (PlayerCommand) vacpackCommand::handle);
        router.register("crafting", craftingCommand::handle);
        router.register("farmertable", (PlayerCommand) this.services.farmerTableMenuService()::open);
        router.register("penfeed", (PlayerCommand) this.services.penService()::handleFeedCommand);
        router.register("battlepass", (PlayerCommand) this.services.questService()::handleBattlePassCommand);
        router.register("bp", (PlayerCommand) this.services.questService()::handleBattlePassCommand);
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
