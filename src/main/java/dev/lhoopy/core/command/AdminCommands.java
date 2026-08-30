package dev.lhoopy.core.command;

import dev.lhoopy.core.bootstrap.ServiceRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public final class AdminCommands {
    private final ServiceRegistry services;

    public AdminCommands(ServiceRegistry services) {
        this.services = services;
    }

    public void register(SlimesCommandRouter router) {
        router.register("slimesadmin", this::handle);
    }

    private void handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("slimes.admin")) {
            sender.sendMessage(ChatColor.RED + "Нет доступа.");
            return;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String action = args[0].toLowerCase(java.util.Locale.ROOT);
        String[] nestedArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (action) {
            case "spawn":
                this.services.slimeService().spawn(sender, nestedArgs);
                break;
            case "bukkit":
                this.services.slimeService().spawnBukkit(sender, nestedArgs);
                break;
            case "packet":
                this.services.slimeService().spawnPacket(sender, nestedArgs);
                break;
            case "clear":
                clearSlimes(sender, nestedArgs);
                break;
            case "whitelist":
                this.services.whitelistService().handleCommand(sender, nestedArgs);
                break;
            default:
                sendUsage(sender);
                break;
        }
    }

    private void clearSlimes(CommandSender sender, String[] args) {
        this.services.slimeService().clearCapturedSlimes(sender);
        if (sender instanceof Player) {
            this.services.penService().refreshPenVisuals((Player) sender);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/slimesadmin spawn <slimeId> [interested]");
        sender.sendMessage(ChatColor.YELLOW + "/slimesadmin bukkit <slimeId> [interested]");
        sender.sendMessage(ChatColor.YELLOW + "/slimesadmin packet [slimeId|clear] [interested]");
        sender.sendMessage(ChatColor.YELLOW + "/slimesadmin clear");
        sender.sendMessage(ChatColor.YELLOW + "/slimesadmin whitelist <add|remove|list|enforce> [nick]");
    }
}
