package dev.lhoopy.crafting;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.RecipeDef;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.core.command.PlayerCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class CraftingCommand {
    private final ProfileService profileService;
    private final CraftingService craftingService;
    private final CraftingMessages messages;

    public CraftingCommand(ContentRegistry contentRegistry, ProfileService profileService, CraftingService craftingService) {
        this.profileService = profileService;
        this.craftingService = craftingService;
        this.messages = new CraftingMessages(contentRegistry);
    }

    public void handle(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            this.messages.sendHelp(sender);
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            this.messages.sendRecipeList(sender, args.length >= 2 ? args[1] : null);
            return;
        }

        if (action.equals("info")) {
            if (args.length < 2) {
                sender.sendMessage("§cИспользование: /crafting info <recipeId>");
                return;
            }
            this.messages.sendRecipeInfo(sender, args[1]);
            return;
        }

        if (action.equals("make")) {
            if (sender instanceof Player player) {
                craftFromCommand(player, args);
            } else {
                sender.sendMessage(PlayerCommand.PLAYERS_ONLY);
            }
            return;
        }

        this.messages.sendHelp(sender);
    }

    private void craftFromCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cИспользование: /crafting make <farmer_table|slime_lab> <recipeId> [amount]");
            return;
        }
        Integer amount = parseAmount(player, args, 3);
        if (amount == null) {
            return;
        }
        craft((Player) player, args[1], args[2], amount);
    }

    private void craft(Player player, String stationId, String recipeId, int amount) {
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage("§cПрофиль ещё загружается.");
            return;
        }

        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        CraftingResult result = this.craftingService.craft(profile, recipeId, stationId, amount);
        if (!result.isSuccess()) {
            this.messages.sendCraftFailure(player, recipeId, result);
            return;
        }

        this.profileService.saveLoaded(player.getUniqueId());
        RecipeDef recipe = result.getRecipe();
        int totalResultAmount = recipe.getResultAmount() * result.getCraftedAmount();
        player.sendMessage("§aСкрафчено: §f" + recipe.getResultId() + " x" + totalResultAmount
                + " §8(" + result.getCraftedAmount() + "/" + result.getRequestedAmount() + ")");
    }

    private static Integer parseAmount(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            return 1;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§cКоличество должно быть числом.");
            return null;
        }
    }
}
