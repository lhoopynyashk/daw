package dev.lhoopy.core.command;

import dev.lhoopy.storage.StorageService;
import dev.lhoopy.storage.StoredItem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StorageCommand {
    private final StorageService storageService;

    public StorageCommand(StorageService storageService) {
        this.storageService = storageService;
    }

    public void handle(Player player, String[] args) {
        if (!player.hasPermission("slimes.storage")) {
            player.sendMessage("\u00a7cНет доступа.");
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendUsage(player);
            return;
        }

        String action = args[0].toLowerCase(java.util.Locale.ROOT);
        if (action.equals("list")) {
            listItems(player);
            return;
        }

        if (args.length < 2) {
            sendUsage(player);
            return;
        }

        String itemId = args[1];
        if (action.equals("protect") || action.equals("unprotect")) {
            setProtected(player, itemId, action.equals("protect"));
            return;
        }

        if (args.length < 3) {
            sendUsage(player);
            return;
        }

        int amount = parsePositiveInt(args[2]);
        if (amount <= 0) {
            player.sendMessage("\u00a7cКоличество должно быть больше нуля.");
            return;
        }

        if (action.equals("add")) {
            addItem(player, itemId, amount);
            return;
        }

        if (action.equals("remove")) {
            removeItem(player, itemId, amount);
            return;
        }

        sendUsage(player);
    }

    private void listItems(Player player) {
        if (!this.storageService.ensureReady(player)) {
            return;
        }
        if (this.storageService.getItems(player).isEmpty()) {
            player.sendMessage("\u00a7eСклад пуст.");
            return;
        }
        player.sendMessage("\u00a7aСклад:");
        for (StoredItem item : this.storageService.getItems(player)) {
            String locked = item.isProtectedItem() ? " \u00a77[защищено]" : "";
            player.sendMessage("\u00a77- \u00a7f" + item.getItemId() + "\u00a77: \u00a7e" + item.getAmount() + locked);
        }
    }

    private void setProtected(Player player, String itemId, boolean protectedItem) {
        if (this.storageService.setProtected(player, itemId, protectedItem)) {
            player.sendMessage(protectedItem
                    ? "\u00a7aПредмет защищён на складе: \u00a7f" + itemId
                    : "\u00a7eЗащита предмета снята: \u00a7f" + itemId);
        } else {
            player.sendMessage("\u00a7eПрофиль ещё загружается, попробуй через пару секунд.");
        }
    }

    private void addItem(Player player, String itemId, int amount) {
        if (this.storageService.addItem(player, itemId, amount)) {
            player.sendMessage("\u00a7aДобавлено на склад: \u00a7f" + amount + "x " + itemId);
        } else {
            player.sendMessage("\u00a7eПрофиль ещё загружается, попробуй через пару секунд.");
        }
    }

    private void removeItem(Player player, String itemId, int amount) {
        if (this.storageService.removeItem(player, itemId, amount)) {
            player.sendMessage("\u00a7aУбрано со склада: \u00a7f" + amount + "x " + itemId);
        } else {
            player.sendMessage("\u00a7cНе хватает предметов или профиль ещё загружается.");
        }
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage("\u00a7e/storage add <itemId> <amount>");
        sender.sendMessage("\u00a7e/storage remove <itemId> <amount>");
        sender.sendMessage("\u00a7e/storage protect <itemId>");
        sender.sendMessage("\u00a7e/storage unprotect <itemId>");
        sender.sendMessage("\u00a7e/storage list");
    }

    private static int parsePositiveInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
