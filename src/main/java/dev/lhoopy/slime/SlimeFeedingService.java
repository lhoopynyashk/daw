package dev.lhoopy.slime;

import dev.lhoopy.core.SlimesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public final class SlimeFeedingService {
    private final SlimesPlugin plugin;
    private final SlimeRuntimeRegistry registry;

    SlimeFeedingService(SlimesPlugin plugin, SlimeRuntimeRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void handleFoodClick(Player player, Slime slime, RuntimeSlime runtime, ItemStack itemInHand) {
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            return;
        }

        if (runtime.state == SlimeState.INTERESTED) {
            player.sendMessage(ChatColor.YELLOW + "Слайм уже заинтересован. Используй сосалку.");
            return;
        }

        if (itemInHand.getType() != runtime.definition.getFavoriteFood()) {
            player.sendMessage(ChatColor.RED + "Эта еда не интересует слайма.");
            player.sendMessage(ChatColor.GRAY + "Нужная еда: " + runtime.definition.getFavoriteFood().name());
            return;
        }

        consumeOne(player, itemInHand);
        runtime.state = SlimeState.INTERESTED;
        SlimeRuntimeRegistry.applyName(slime, runtime);
        scheduleInterestReset(slime, runtime);
        player.sendMessage(ChatColor.GREEN + "Слайм заинтересовался! "
                + ChatColor.GRAY + "Время: " + runtime.definition.getInterestSeconds() + "с.");
    }

    private void scheduleInterestReset(Slime slime, RuntimeSlime runtime) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            RuntimeSlime current = this.registry.get(slime.getUniqueId());
            if (current == runtime && !slime.isDead()) {
                current.state = SlimeState.NORMAL;
                SlimeRuntimeRegistry.applyName(slime, current);
            }
            this.registry.removeInterestTask(slime.getUniqueId());
        }, runtime.definition.getInterestSeconds() * 20L);
        this.registry.putInterestTask(slime.getUniqueId(), task);
    }

    private static void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() <= 1) {
            player.setItemInHand(null);
            return;
        }
        item.setAmount(item.getAmount() - 1);
        player.setItemInHand(item);
    }
}
