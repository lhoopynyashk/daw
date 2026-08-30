package dev.lhoopy.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SlimesCommandRouter implements CommandExecutor {
    private final Map<String, RoutedCommand> commands = new LinkedHashMap<>();

    public void register(String name, RoutedCommand command) {
        this.commands.put(name.toLowerCase(Locale.ROOT), command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        RoutedCommand routedCommand = this.commands.get(command.getName().toLowerCase(Locale.ROOT));
        if (routedCommand == null) {
            return false;
        }

        routedCommand.execute(sender, args);
        return true;
    }
}
