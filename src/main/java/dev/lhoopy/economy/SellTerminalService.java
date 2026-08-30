package dev.lhoopy.economy;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.PlortDef;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.pen.PenStyleCatalog;
import dev.lhoopy.storage.PlayerStorage;
import dev.lhoopy.storage.StoredItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class SellTerminalService {
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final PriceService priceService;
    private final PenStyleCatalog penStyleCatalog;

    public SellTerminalService(ContentRegistry contentRegistry, ProfileService profileService, PriceService priceService,
                               PenStyleCatalog penStyleCatalog) {
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.priceService = priceService;
        this.penStyleCatalog = penStyleCatalog;
    }

    public void sellAllPlorts(Player player, PlayerProfile profile) {
        SellResult result = sellAllPlorts(profile);
        if (result.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Продавать пока нечего.");
            return;
        }

        this.profileService.saveLoaded(player.getUniqueId());
        player.playSound(player.getLocation(), "random.orb", 0.8F, 1.1F);
        player.sendMessage(ChatColor.GREEN + "Продано плортов: " + ChatColor.WHITE + result.getTotalAmount()
                + ChatColor.GRAY + " (" + result.getVacpackAmount() + " вакпак, " + result.getStorageAmount() + " склад)"
                + ChatColor.GREEN + ". Получено монет: " + ChatColor.GOLD + result.getCoins()
                + ChatColor.GREEN + ". Баланс: " + ChatColor.GOLD + profile.getCoins());
    }

    public SellResult sellAllPlorts(PlayerProfile profile) {
        double sellMultiplier = this.penStyleCatalog.get(profile.getActivePenStyleId()).getSellMultiplier();
        SellResult vacpack = sellPlortsFrom(profile.getVacpackStorage(), false, sellMultiplier);
        SellResult storage = sellPlortsFrom(profile.getStorage(), true, sellMultiplier);
        SellResult total = new SellResult(
                vacpack.getTotalAmount(),
                storage.getTotalAmount(),
                vacpack.getCoins() + storage.getCoins()
        );
        if (!total.isEmpty()) {
            profile.setCoins(saturatingAdd(profile.getCoins(), total.getCoins()));
        }
        return total;
    }

    public SellResult sellPlort(PlayerProfile profile, String plortId, int requestedAmount) {
        PlortDef plort = this.contentRegistry.getPlort(plortId);
        if (plort == null || requestedAmount <= 0) {
            return new SellResult(0, 0, 0L);
        }

        double multiplier = this.penStyleCatalog.get(profile.getActivePenStyleId()).getSellMultiplier();
        int remaining = requestedAmount;
        int vacpackAmount = removeAvailable(profile.getVacpackStorage(), plort.getId(), remaining, false);
        remaining -= vacpackAmount;
        int storageAmount = removeAvailable(profile.getStorage(), plort.getId(), remaining, true);
        int soldAmount = vacpackAmount + storageAmount;
        long coins = (long) soldAmount * effectivePrice(plort, multiplier);
        if (coins > 0L) {
            profile.setCoins(saturatingAdd(profile.getCoins(), coins));
        }
        return new SellResult(vacpackAmount, storageAmount, coins);
    }

    public int getAvailableAmount(PlayerProfile profile, String plortId) {
        if (this.contentRegistry.getPlort(plortId) == null) {
            return 0;
        }
        int vacpackAmount = profile.getVacpackStorage().getAmount(plortId);
        StoredItem storageItem = profile.getStorage().get(plortId);
        int storageAmount = storageItem == null || storageItem.isProtectedItem() ? 0 : storageItem.getAmount();
        return vacpackAmount + storageAmount;
    }

    public int getEffectivePrice(PlayerProfile profile, PlortDef plort) {
        double multiplier = this.penStyleCatalog.get(profile.getActivePenStyleId()).getSellMultiplier();
        return effectivePrice(plort, multiplier);
    }

    public long estimateAllPlorts(PlayerProfile profile) {
        double sellMultiplier = this.penStyleCatalog.get(profile.getActivePenStyleId()).getSellMultiplier();
        return estimatePlorts(profile.getVacpackStorage(), false, sellMultiplier)
                + estimatePlorts(profile.getStorage(), true, sellMultiplier);
    }

    private SellResult sellPlortsFrom(PlayerStorage storage, boolean skipProtected, double sellMultiplier) {
        int soldAmount = 0;
        long coins = 0L;
        List<StoredItem> copy = new ArrayList<>(storage.getItems());
        for (StoredItem item : copy) {
            if (skipProtected && item.isProtectedItem()) {
                continue;
            }
            PlortDef plort = this.contentRegistry.getPlort(item.getItemId());
            if (plort == null || item.getAmount() <= 0) {
                continue;
            }
            int amount = item.getAmount();
            if (storage.remove(item.getItemId(), amount)) {
                soldAmount += amount;
                coins += (long) amount * effectivePrice(plort, sellMultiplier);
            }
        }
        return new SellResult(soldAmount, 0, coins);
    }

    private long estimatePlorts(PlayerStorage storage, boolean skipProtected, double sellMultiplier) {
        long coins = 0L;
        for (StoredItem item : storage.getItems()) {
            if (skipProtected && item.isProtectedItem()) {
                continue;
            }
            PlortDef plort = this.contentRegistry.getPlort(item.getItemId());
            if (plort != null && item.getAmount() > 0) {
                coins += (long) item.getAmount() * effectivePrice(plort, sellMultiplier);
            }
        }
        return coins;
    }

    private int removeAvailable(PlayerStorage storage, String plortId, int requestedAmount, boolean skipProtected) {
        if (requestedAmount <= 0) {
            return 0;
        }
        StoredItem item = storage.get(plortId);
        if (item == null || item.getAmount() <= 0 || skipProtected && item.isProtectedItem()) {
            return 0;
        }
        int removed = Math.min(requestedAmount, item.getAmount());
        return storage.remove(plortId, removed) ? removed : 0;
    }

    private int effectivePrice(PlortDef plort, double multiplier) {
        return Math.max(0, (int) Math.round(this.priceService.getSellPrice(plort) * multiplier));
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
