package dev.lhoopy.core.command;

import dev.lhoopy.sound.service.SoundtrackService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MusicCommand {
    private final SoundtrackService soundtrackService;

    public MusicCommand(SoundtrackService soundtrackService) {
        this.soundtrackService = soundtrackService;
    }

    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }

        Player player = (Player) sender;
        if (args.length >= 1 && args[0].equalsIgnoreCase("stop")) {
            this.soundtrackService.stopFor(player);
            return;
        }

        this.soundtrackService.playDefaultSoundtrack(player);
    }
}
