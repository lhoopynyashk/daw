package dev.lhoopy.slime;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.SlimeDef;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;

public final class SlimeSpawnService {
    private final ContentRegistry contentRegistry;
    private final SlimeRuntimeRegistry registry;

    SlimeSpawnService(ContentRegistry contentRegistry, SlimeRuntimeRegistry registry) {
        this.contentRegistry = contentRegistry;
        this.registry = registry;
    }

    public void spawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }

        Player player = (Player) sender;
        String id = args.length >= 1 ? args[0] : null;
        SlimeDef definition = id == null ? this.contentRegistry.getDefaultSlime() : this.contentRegistry.getSlime(id);
        if (definition == null) {
            player.sendMessage(ChatColor.RED + "Неизвестный тип слайма. Загружено типов: " + this.contentRegistry.slimes().size());
            return;
        }

        SlimeState state = args.length >= 2 && args[1].equalsIgnoreCase("interested")
                ? SlimeState.INTERESTED
                : SlimeState.NORMAL;

        Slime slime = (Slime) player.getWorld().spawnEntity(player.getLocation(), EntityType.SLIME);
        slime.setSize(definition.getSize());
        RuntimeSlime runtime = new RuntimeSlime(definition, state);
        this.registry.put(slime.getUniqueId(), runtime);
        SlimeRuntimeRegistry.applyName(slime, runtime);

        player.sendMessage(ChatColor.GREEN + "Спавн: " + definition.getDisplayName()
                + ChatColor.GRAY + " (" + state.name().toLowerCase() + ")");
    }

    public void giveFavoriteFood(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }

        Player player = (Player) sender;
        String id = args.length >= 1 ? args[0] : null;
        SlimeDef definition = id == null ? this.contentRegistry.getDefaultSlime() : this.contentRegistry.getSlime(id);
        if (definition == null) {
            player.sendMessage(ChatColor.RED + "Неизвестный тип слайма. Загружено типов: " + this.contentRegistry.slimes().size());
            return;
        }

        player.getInventory().addItem(new ItemStack(definition.getFavoriteFood(), 8));
        player.sendMessage(ChatColor.GREEN + "Приманка для "
                + ChatColor.WHITE + definition.getDisplayName()
                + ChatColor.GREEN + ": " + ChatColor.WHITE + definition.getFavoriteFood().name());
    }
}
