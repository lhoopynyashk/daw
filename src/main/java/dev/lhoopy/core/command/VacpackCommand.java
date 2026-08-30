package dev.lhoopy.core.command;

import dev.lhoopy.storage.StorageService;
import dev.lhoopy.storage.StoredItem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class VacpackCommand {
    private final StorageService storageService;

    public VacpackCommand(StorageService storageService) {
        this.storageService = storageService;
    }

    public void handle(Player player, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            listItems(player);
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("deposit")) {
            deposit(player, args);
            return;
        }
        if (action.equals("withdraw")) {
            withdraw(player, args);
            return;
        }

        sendUsage(player);
    }

    private void listItems(Player player) {
        if (!this.storageService.ensureReady(player)) {
            player.sendMessage("\u00a7eПрофиль ещё загружается, попробуй через пару секунд.");
            return;
        }
        player.sendMessage("\u00a7dВакпак:");
        sendCategory(player, "plorts");
        sendCategory(player, "food");
        sendCategory(player, "seeds");
        sendCategory(player, "resources");
        sendCategory(player, "other");
        if (this.storageService.getVacpackItems(player).isEmpty()) {
            player.sendMessage("\u00a77- пусто");
            return;
        }
        for (StoredItem item : this.storageService.getVacpackItems(player)) {
            if (item.getAmount() > 0) {
                player.sendMessage("\u00a77- \u00a7f" + item.getItemId() + "\u00a77: \u00a7e" + item.getAmount());
            }
        }
    }

    private void sendCategory(Player player, String category) {
        player.sendMessage("\u00a77- " + categoryTitle(category) + ": \u00a7f" + this.storageService.getVacpackUsed(player, category)
                + "\u00a77/\u00a7f" + this.storageService.getVacpackCapacity(player, category));
    }

    private void deposit(Player player, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
            int moved = this.storageService.depositAllVacpack(player);
            player.sendMessage("\u00a7aПереложено на склад ранчо: \u00a7f" + moved);
            return;
        }
        if (args.length < 3) {
            sendUsage(player);
            return;
        }
        String itemId = args[1];
        int amount = parseAmount(args[2], this.storageService.getVacpackItems(player), itemId);
        int moved = this.storageService.moveVacpackToStorage(player, itemId, amount);
        player.sendMessage(moved > 0
                ? "\u00a7aПереложено на склад ранчо: \u00a7f" + moved + "x " + itemId
                : "\u00a7cНичего не переложено.");
    }

    private void withdraw(Player player, String[] args) {
        if (args.length < 3) {
            sendUsage(player);
            return;
        }
        String itemId = args[1];
        int amount = parsePositiveInt(args[2]);
        if (amount <= 0) {
            player.sendMessage("\u00a7cКоличество должно быть больше нуля.");
            return;
        }
        int moved = this.storageService.moveStorageToVacpack(player, itemId, amount);
        player.sendMessage(moved > 0
                ? "\u00a7aПереложено в вакпак: \u00a7f" + moved + "x " + itemId
                : "\u00a7cНичего не переложено. Проверь склад и лимит вакпака.");
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage("\u00a7e/vacpack list");
        sender.sendMessage("\u00a7e/vacpack deposit all");
        sender.sendMessage("\u00a7e/vacpack deposit <itemId> <amount|all>");
        sender.sendMessage("\u00a7e/vacpack withdraw <itemId> <amount>");
    }

    private static int parseAmount(String raw, Iterable<StoredItem> items, String itemId) {
        if (raw.equalsIgnoreCase("all")) {
            for (StoredItem item : items) {
                if (item.getItemId().equalsIgnoreCase(itemId)) {
                    return item.getAmount();
                }
            }
            return 0;
        }
        return parsePositiveInt(raw);
    }

    private static int parsePositiveInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String categoryTitle(String category) {
        switch (category.toLowerCase(Locale.ROOT)) {
            case "plorts":
                return "плорты";
            case "food":
                return "еда";
            case "seeds":
                return "семена";
            case "resources":
                return "ресурсы";
            default:
                return "прочее";
        }
    }
}
