package dev.lhoopy.crafting;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.RecipeDef;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

final class CraftingMessages {
    private final ContentRegistry contentRegistry;

    CraftingMessages(ContentRegistry contentRegistry) {
        this.contentRegistry = contentRegistry;
    }

    void sendHelp(CommandSender sender) {
        sender.sendMessage("§eКрафты:");
        sender.sendMessage("§7/crafting list [station] §8- список рецептов");
        sender.sendMessage("§7/crafting info <recipeId> §8- состав рецепта");
        sender.sendMessage("§7/crafting make <station> <recipeId> [amount] §8- скрафтить");
        sender.sendMessage("§8Станции: farmer_table, slime_lab.");
    }

    void sendRecipeList(CommandSender sender, String stationId) {
        sender.sendMessage("§eДоступные рецепты:");
        int count = 0;
        for (RecipeDef recipe : this.contentRegistry.recipes()) {
            if (stationId != null && !recipe.getStationId().equalsIgnoreCase(stationId)) {
                continue;
            }
            String price = recipe.getCoinCost() > 0L ? " §6$" + recipe.getCoinCost() : "";
            sender.sendMessage("§7- §f" + recipe.getId() + " §8[" + recipe.getStationId() + "/" + recipe.getCategoryId() + "] §a-> "
                    + recipe.getResultId() + " x" + recipe.getResultAmount() + price);
            count++;
        }
        if (count == 0) {
            sender.sendMessage("§cРецептов не найдено.");
        }
    }

    void sendRecipeInfo(CommandSender sender, String recipeId) {
        RecipeDef recipe = this.contentRegistry.getRecipe(recipeId);
        if (recipe == null) {
            sender.sendMessage("§cНеизвестный рецепт: " + recipeId);
            return;
        }

        sender.sendMessage("§e" + recipe.getId() + " §8[" + recipe.getStationId() + "]");
        sender.sendMessage("§7Категория: §f" + recipe.getCategoryId());
        sender.sendMessage("§7Результат: §f" + recipe.getResultId() + " x" + recipe.getResultAmount());
        sender.sendMessage("§7Цена: §f" + recipe.getCoinCost() + " §8| шанс: §f" + Math.round(recipe.getSuccessChance() * 100.0D)
                + "% §8| максимум: §f" + recipe.getMaxCraftsPerAction());
        sendRequirements(sender, "Анлоки", recipe.getUnlockRequirements());
        sendRequirements(sender, "Флаги", recipe.getFlagRequirements());
        sender.sendMessage("§7Ингредиенты:");
        for (Map.Entry<String, Integer> ingredient : recipe.getIngredients().entrySet()) {
            sender.sendMessage("§8- §f" + ingredient.getKey() + " x" + ingredient.getValue());
        }
    }

    void sendCraftFailure(Player player, String recipeId, CraftingResult result) {
        CraftingFailureReason reason = result.getFailureReason();
        if (reason == CraftingFailureReason.UNKNOWN_RECIPE) {
            player.sendMessage("§cНеизвестный рецепт: " + recipeId);
            return;
        }
        if (reason == CraftingFailureReason.WRONG_STATION) {
            player.sendMessage("§cЭтот рецепт делается на другой станции: " + result.getRecipe().getStationId());
            return;
        }
        if (reason == CraftingFailureReason.INVALID_AMOUNT) {
            player.sendMessage("§cКоличество должно быть больше 0.");
            return;
        }
        if (reason == CraftingFailureReason.AMOUNT_LIMIT_EXCEEDED) {
            player.sendMessage("§cЗа раз можно скрафтить максимум: " + result.getRecipe().getMaxCraftsPerAction());
            return;
        }
        if (reason == CraftingFailureReason.MISSING_INGREDIENTS) {
            player.sendMessage("§cНе хватает ингредиентов:");
            for (Map.Entry<String, Integer> missing : result.getMissingIngredients().entrySet()) {
                player.sendMessage("§8- §f" + missing.getKey() + " x" + missing.getValue());
            }
            return;
        }
        if (reason == CraftingFailureReason.MISSING_COINS) {
            player.sendMessage("§cНе хватает монет: " + result.getMissingCoins());
            return;
        }
        if (reason == CraftingFailureReason.LOCKED) {
            player.sendMessage("§cРецепт ещё закрыт.");
            sendMissingList(player, "Анлоки", result.getMissingUnlocks());
            sendMissingList(player, "Флаги", result.getMissingFlags());
            return;
        }
        if (reason == CraftingFailureReason.STORAGE_FULL) {
            player.sendMessage("§cХранилище переполнено для результата рецепта.");
            return;
        }
        if (reason == CraftingFailureReason.CHANCE_FAILED) {
            player.sendMessage("§cКрафт не удался по шансу.");
            return;
        }
        player.sendMessage("§cКрафт не удался: " + reason);
    }

    private static void sendRequirements(CommandSender sender, String label, List<String> values) {
        if (!values.isEmpty()) {
            sender.sendMessage("§7" + label + ": §f" + String.join(", ", values));
        }
    }

    private static void sendMissingList(CommandSender sender, String label, List<String> values) {
        if (!values.isEmpty()) {
            sender.sendMessage("§8- " + label + ": §f" + String.join(", ", values));
        }
    }
}
