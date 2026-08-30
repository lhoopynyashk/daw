package dev.lhoopy.pen;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.ContentIds;
import dev.lhoopy.content.SlimeDef;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.slime.PenPacketSlimeTarget;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
final class PenFeedingService {
    private final Plugin plugin;
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final PenVisualService visualService;
    private final long fedDurationMillis;
    private final PenStyleCatalog styleCatalog;

    PenFeedingService(Plugin plugin, ContentRegistry contentRegistry, ProfileService profileService,
                      PenVisualService visualService, PenStyleCatalog styleCatalog, long fedDurationMillis) {
        this.plugin = plugin;
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.visualService = visualService;
        this.styleCatalog = styleCatalog;
        this.fedDurationMillis = fedDurationMillis;
    }

    boolean feedNearest(Player player, ItemStack item) {
        PenPacketSlimeTarget target = findNearestPenSlot(player, 4.5D);
        if (target == null) {
            return false;
        }
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher загружается.");
            return true;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            return true;
        }
        int penIndex = target.getPenIndex();
        if (penIndex < 0 || penIndex >= profile.getPenSlimes().size()) {
            this.visualService.refresh(player);
            return true;
        }
        SlimeDef definition = target.getDefinition();
        if (!isKnownFood(item)) {
            player.sendMessage(ChatColor.RED + "Это не еда для слаймов.");
            return true;
        }

        PenSlime penSlime = profile.getPenSlimes().get(penIndex);
        String plortId = produceImmediatePlort(profile, definition);
        if (plortId == null) {
            player.sendMessage(ChatColor.RED + "Накопитель загона полон.");
            return true;
        }
        double foodUse = this.styleCatalog.get(profile.getActivePenStyleId()).getFoodUseMultiplier();
        long effectiveFedDuration = (long) (this.fedDurationMillis / Math.max(0.1D, foodUse));
        boolean favorite = item.getType() == definition.getFavoriteFood();
        penSlime.feed(System.currentTimeMillis(), effectiveFedDuration,
                favorite ? PenFeedQuality.FAVORITE : PenFeedQuality.FED);
        consumeOne(player, item);
        this.profileService.saveLoaded(player.getUniqueId());
        player.playSound(player.getLocation(), "random.orb", 0.8F, 1.35F);
        player.sendMessage(ChatColor.GREEN + "Слайм накормлен: " + ChatColor.WHITE + definition.getDisplayName());
        player.sendMessage(ChatColor.GRAY + "Производство: " + ChatColor.WHITE
                + (favorite ? "125% (любимая еда)" : "100%"));
        return true;
    }

    private boolean isKnownFood(ItemStack item) {
        for (SlimeDef slime : this.contentRegistry.slimes()) {
            if (slime.getFavoriteFood() == item.getType()) {
                return true;
            }
        }
        return false;
    }

    private String produceImmediatePlort(PlayerProfile profile, SlimeDef definition) {
        String plortId = ContentIds.resolvePlortForSlime(this.contentRegistry, definition.getId());
        int stacks = Math.max(1, this.plugin.getConfig().getInt("plort-production.pen-visual-stacks", 3));
        int perStack = Math.max(1, this.plugin.getConfig().getInt("plort-production.plorts-per-stack", 20));
        int capacity = stacks * perStack;
        int used = profile.getPenPlortStorage().getTotalAmount();
        return profile.getPenPlortStorage().addLimited(plortId, 1, used, capacity) <= 0 ? null : plortId;
    }

    private PenPacketSlimeTarget findNearestPenSlot(Player player, double range) {
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            return null;
        }
        Location center = PenLayout.centerNear(player);
        double nearestDistance = range * range;
        PenPacketSlimeTarget nearest = null;
        List<PenSlime> penSlimes = profile.getPenSlimes();
        int visibleCount = Math.min(penSlimes.size(), this.styleCatalog.effectiveCapacity(profile));
        for (int index = 0; index < visibleCount; index++) {
            SlimeDef definition = this.contentRegistry.getSlime(penSlimes.get(index).getSlimeId());
            if (definition == null) {
                continue;
            }
            double distance = PenLayout.slot(center, index).distanceSquared(player.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = new PenPacketSlimeTarget(definition, index);
            }
        }
        return nearest;
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
