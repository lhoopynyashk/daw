package dev.lhoopy.storage;

import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;

public final class StorageService {
    private final ProfileService profileService;

    public StorageService(ProfileService profileService) {
        this.profileService = profileService;
    }

    public boolean addItem(Player player, String itemId, int amount) {
        PlayerProfile profile = getProfile(player);
        if (profile == null || amount <= 0) {
            return false;
        }
        profile.getStorage().add(itemId, amount);
        this.profileService.saveLoaded(player.getUniqueId());
        return true;
    }

    public boolean ensureReady(Player player) {
        return getProfile(player) != null;
    }

    public Collection<StoredItem> getItems(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile == null ? Collections.emptyList() : profile.getStorage().getItems();
    }

    public Collection<StoredItem> getVacpackItems(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile == null ? Collections.emptyList() : profile.getVacpackStorage().getItems();
    }

    public int getVacpackUsed(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile == null ? 0 : profile.getVacpackStorage().getTotalAmount();
    }

    public int getVacpackCapacity(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile == null ? 0 : profile.getVacpackCapacity();
    }

    public int getVacpackUsed(Player player, String category) {
        PlayerProfile profile = getProfile(player);
        return profile == null ? 0 : VacpackLimits.used(profile.getVacpackStorage(), category);
    }

    public int getVacpackCapacity(Player player, String category) {
        PlayerProfile profile = getProfile(player);
        if (profile == null) {
            return 0;
        }
        if (category.equals("plorts")) {
            return profile.getVacpackPlortCapacity();
        }
        if (category.equals("food")) {
            return profile.getVacpackFoodCapacity();
        }
        if (category.equals("seeds")) {
            return profile.getVacpackSeedCapacity();
        }
        if (category.equals("resources")) {
            return profile.getVacpackResourceCapacity();
        }
        return profile.getVacpackOtherCapacity();
    }

    public boolean removeItem(Player player, String itemId, int amount) {
        PlayerProfile profile = getProfile(player);
        if (profile == null || amount <= 0 || !profile.getStorage().remove(itemId, amount)) {
            return false;
        }
        this.profileService.saveLoaded(player.getUniqueId());
        return true;
    }

    public int moveStorageToVacpack(Player player, String itemId, int amount) {
        PlayerProfile profile = getProfile(player);
        if (profile == null || amount <= 0) {
            return 0;
        }
        int available = profile.getStorage().getAmount(itemId);
        int requested = Math.min(amount, available);
        if (requested <= 0) {
            return 0;
        }
        int accepted = VacpackLimits.add(profile, itemId, requested);
        if (accepted <= 0) {
            return 0;
        }
        profile.getStorage().remove(itemId, accepted);
        this.profileService.saveLoaded(player.getUniqueId());
        return accepted;
    }

    public int moveVacpackToStorage(Player player, String itemId, int amount) {
        PlayerProfile profile = getProfile(player);
        if (profile == null || amount <= 0) {
            return 0;
        }
        int available = profile.getVacpackStorage().getAmount(itemId);
        int moved = Math.min(amount, available);
        if (moved <= 0) {
            return 0;
        }
        profile.getVacpackStorage().remove(itemId, moved);
        profile.getStorage().add(itemId, moved);
        this.profileService.saveLoaded(player.getUniqueId());
        return moved;
    }

    public int depositAllVacpack(Player player) {
        PlayerProfile profile = getProfile(player);
        if (profile == null) {
            return 0;
        }
        int moved = 0;
        java.util.List<StoredItem> copy = new java.util.ArrayList<>(profile.getVacpackStorage().getItems());
        for (StoredItem item : copy) {
            moved += moveVacpackToStorage(player, item.getItemId(), item.getAmount());
        }
        return moved;
    }

    public boolean hasItem(Player player, String itemId, int amount) {
        PlayerProfile profile = getProfile(player);
        return profile != null && profile.getStorage().has(itemId, amount);
    }

    public int getAmount(Player player, String itemId) {
        PlayerProfile profile = getProfile(player);
        return profile == null ? 0 : profile.getStorage().getAmount(itemId);
    }

    public boolean setProtected(Player player, String itemId, boolean protectedItem) {
        PlayerProfile profile = getProfile(player);
        if (profile == null) {
            return false;
        }
        profile.getStorage().setProtected(itemId, protectedItem);
        this.profileService.saveLoaded(player.getUniqueId());
        return true;
    }

    public boolean isProtected(Player player, String itemId) {
        PlayerProfile profile = getProfile(player);
        return profile != null && profile.getStorage().isProtected(itemId);
    }

    private PlayerProfile getProfile(Player player) {
        if (!this.profileService.ensureLoaded(player)) {
            return null;
        }
        return this.profileService.getLoaded(player.getUniqueId());
    }
}
