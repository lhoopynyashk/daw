package dev.lhoopy.core.command;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.slime.SlimeVacuumItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class StarterCommand {
    private static final long STARTER_COINS = 50L;
    private static final String STARTER_SLIME = "slime_pink";
    private static final String STARTER_SEED = "seed_sweetroot";
    private static final String STARTER_FOOD = "plant_sweetroot";

    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;

    public StarterCommand(ContentRegistry contentRegistry, ProfileService profileService) {
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
    }

    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use starter.");
            return;
        }

        Player player = (Player) sender;
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль загружается, попробуй ещё раз через пару секунд.");
            return;
        }

        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.YELLOW + "Профиль ещё не готов.");
            return;
        }

        boolean changed = grantStarter(profile);
        giveVacpackIfMissing(player);
        giveWaterBucketIfMissing(player);
        this.profileService.saveLoaded(player.getUniqueId());

        player.sendMessage(ChatColor.GOLD + "Минимальный цикл SlimeRancher готов.");
        if (changed) {
            player.sendMessage(ChatColor.GRAY + "Я досыпал стартовые ресурсы, чтобы можно было пройти цикл без админки.");
        } else {
            player.sendMessage(ChatColor.GRAY + "Стартовые ресурсы уже были у тебя в профиле.");
        }
        sendGuide(player);
    }

    private boolean grantStarter(PlayerProfile profile) {
        boolean changed = false;
        if (profile.getCoins() < STARTER_COINS) {
            profile.setCoins(STARTER_COINS);
            changed = true;
        }
        changed |= ensureStorage(profile, STARTER_SEED, 3);
        changed |= ensureStorage(profile, STARTER_FOOD, 3);
        if (this.contentRegistry.getSlime(STARTER_SLIME) != null && profile.getPenSlimeIds().isEmpty()) {
            changed |= profile.addPenSlime(STARTER_SLIME);
        }
        if (this.contentRegistry.getSlime(STARTER_SLIME) != null && profile.getCapturedSlimeIds().isEmpty()) {
            profile.addCapturedSlime(STARTER_SLIME);
            changed = true;
        }
        return changed;
    }

    private static boolean ensureStorage(PlayerProfile profile, String itemId, int minimum) {
        int current = profile.getStorage().getAmount(itemId);
        if (current >= minimum) {
            return false;
        }
        profile.getStorage().add(itemId, minimum - current);
        return true;
    }

    private static void giveVacpackIfMissing(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (SlimeVacuumItem.isVacuum(item)) {
                return;
            }
        }
        player.getInventory().addItem(SlimeVacuumItem.create());
    }

    private static void giveWaterBucketIfMissing(Player player) {
        if (player.getInventory().contains(Material.WATER_BUCKET)) {
            return;
        }
        player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET));
    }

    private static void sendGuide(Player player) {
        player.sendMessage(ChatColor.YELLOW + "1. " + ChatColor.WHITE + "ПКМ по пустой грядке " + ChatColor.GRAY + "- посадить первое семя со склада");
        player.sendMessage(ChatColor.YELLOW + "2. " + ChatColor.WHITE + "ПКМ с ведром воды " + ChatColor.GRAY + "- полить грядку");
        player.sendMessage(ChatColor.YELLOW + "3. " + ChatColor.WHITE + "ПКМ по выросшей грядке " + ChatColor.GRAY + "- собрать урожай");
        player.sendMessage(ChatColor.YELLOW + "4. " + ChatColor.WHITE + "Shift+ПКМ " + ChatColor.GRAY + "- сменить стиль грядки");
        player.sendMessage(ChatColor.YELLOW + "5. " + ChatColor.WHITE + "/penfeed all " + ChatColor.GRAY + "- покормить слаймов едой со склада");
        player.sendMessage(ChatColor.YELLOW + "6. " + ChatColor.WHITE + "/plorts collect");
        player.sendMessage(ChatColor.YELLOW + "7. " + ChatColor.WHITE + "/sellterminal sell");
    }
}
