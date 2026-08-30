package dev.lhoopy.core.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Команда, которая имеет смысл только для игрока. Проверку выполняет роутер. */
@FunctionalInterface
public interface PlayerCommand extends RoutedCommand {
    String PLAYERS_ONLY = ChatColor.RED + "Эта команда доступна только игроку.";

    void execute(Player player, String[] args);

    @Override
    default void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            execute(player, args);
        } else {
            sender.sendMessage(PLAYERS_ONLY);
        }
    }
}
