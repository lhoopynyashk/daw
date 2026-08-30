package dev.lhoopy.core.command;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface RoutedCommand {
    void execute(CommandSender sender, String[] args);
}
