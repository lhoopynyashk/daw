package dev.lhoopy.farm;

import dev.lhoopy.world.RanchWorldService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.BooleanSupplier;

final class FarmTravelService {
    private final RanchWorldService ranchWorldService;
    private final FarmWorldService farmWorldService;
    private final BooleanSupplier huntZoneRealm;

    FarmTravelService(RanchWorldService ranchWorldService, FarmWorldService farmWorldService, BooleanSupplier huntZoneRealm) {
        this.ranchWorldService = ranchWorldService;
        this.farmWorldService = farmWorldService;
        this.huntZoneRealm = huntZoneRealm;
    }

    void teleportHome(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }

        Player player = (Player) sender;
        if (this.huntZoneRealm.getAsBoolean()) {
            player.sendMessage(ChatColor.YELLOW + "На охоте ферма не создаётся.");
            return;
        }

        if (this.ranchWorldService.isEnabled()) {
            Location spawn = this.ranchWorldService.openRanch(player);
            this.farmWorldService.ensureRanchPenNpc(player, spawn.getWorld());
        } else {
            player.teleport(this.farmWorldService.ensureSharedFarm(player));
        }

        player.sendMessage(ChatColor.GREEN + "Ты вернулся на свою ферму.");
    }

    void visit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }

        Player player = (Player) sender;
        if (args.length < 1) {
            player.sendMessage(ChatColor.YELLOW + "Используй: /visit <ник>");
            return;
        }
        if (this.huntZoneRealm.getAsBoolean()) {
            player.sendMessage(ChatColor.YELLOW + "На охоте нельзя перейти на чужое ранчо.");
            return;
        }

        Player target = findOnlinePlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден или не в сети.");
            return;
        }
        if (target.equals(player)) {
            teleportHome(sender);
            return;
        }

        if (this.ranchWorldService.isEnabled()) {
            Location spawn = this.ranchWorldService.openRanch(player, target);
            this.farmWorldService.ensureRanchPenNpc(target, spawn.getWorld());
        } else {
            player.teleport(target.getLocation());
        }

        player.sendMessage(ChatColor.GREEN + "Ты пришёл на ранчо игрока " + target.getName() + ".");
        target.sendMessage(ChatColor.YELLOW + player.getName() + " пришёл на твоё ранчо.");
    }

    Location ensureSharedFarm(Player player) {
        return this.farmWorldService.ensureSharedFarm(player);
    }

    private Player findOnlinePlayer(String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
}
