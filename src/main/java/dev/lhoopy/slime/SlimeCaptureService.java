package dev.lhoopy.slime;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.SlimeDef;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SlimeCaptureService {
    private final ContentRegistry contentRegistry;
    private final EnginexHuntBridge huntBridge;
    private final ProfileService profileService;
    private final SlimeRuntimeRegistry registry;
    private final PacketSlimeService packetSlimeService;

    SlimeCaptureService(ContentRegistry contentRegistry, EnginexHuntBridge huntBridge, ProfileService profileService, SlimeRuntimeRegistry registry, PacketSlimeService packetSlimeService) {
        this.contentRegistry = contentRegistry;
        this.huntBridge = huntBridge;
        this.profileService = profileService;
        this.registry = registry;
        this.packetSlimeService = packetSlimeService;
    }

    public void startCapture(Player player, Slime slime, RuntimeSlime runtime) {
        if (runtime.state != SlimeState.INTERESTED) {
            player.sendMessage(ChatColor.RED + "Слайм не заинтересован.");
            player.sendMessage(ChatColor.GRAY + "Дай ему любимую еду: " + runtime.definition.getFavoriteFood().name());
            return;
        }

        player.playSound(player.getLocation(), "random.orb", 0.7f, 1.55f);
        this.huntBridge.startCapture(player, new SlimeCaptureTarget(slime.getUniqueId(), runtime.definition));
    }

    public void startCapture(Player player, PacketSlime slime) {
        if (slime.getState() != SlimeState.INTERESTED) {
            player.sendMessage(ChatColor.RED + "Слайм не заинтересован.");
            player.sendMessage(ChatColor.GRAY + "Дай ему любимую еду: " + slime.getDefinition().getFavoriteFood().name());
            return;
        }

        player.playSound(player.getLocation(), "random.orb", 0.7f, 1.55f);
        this.huntBridge.startCapture(player, new SlimeCaptureTarget(slime.getUniqueId(), slime.getDefinition()));
    }

    public void completeCapture(Player player, SlimeCaptureTarget target, int hits, int total) {
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher загружается, поимку нужно повторить.");
            return;
        }
        Entity entity = SlimeRuntimeRegistry.findEntity(target.getEntityUuid());
        if (entity != null && !entity.isDead()) {
            entity.remove();
        } else {
            this.packetSlimeService.remove(player, target.getEntityUuid());
        }
        this.registry.remove(target.getEntityUuid());

        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher ещё загружается, поимка не сохранена.");
            return;
        }
        if (!profile.canCaptureSlime()) {
            player.sendMessage(ChatColor.RED + "Вакпак для слаймов полон: "
                    + ChatColor.WHITE + profile.getCapturedSlimeIds().size() + "/" + profile.getVacpackSlimeCapacity());
            return;
        }
        profile.addCapturedSlime(target.getDefinition().getId());
        this.profileService.saveLoaded(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Слайм пойман: "
                + ChatColor.WHITE + target.getDefinition().getDisplayName()
                + ChatColor.GRAY + " " + hits + "/" + total);
    }

    public List<SlimeDef> findCapturedSlimes(Player player) {
        List<SlimeDef> result = new ArrayList<>();
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile != null) {
            for (String slimeId : profile.getCapturedSlimeIds()) {
                SlimeDef definition = this.contentRegistry.getSlime(slimeId);
                if (definition != null) {
                    result.add(definition);
                }
            }
        }
        for (ItemStack item : player.getInventory().getContents()) {
            String slimeId = readCapturedSlimeId(item);
            if (slimeId == null) {
                continue;
            }
            SlimeDef definition = this.contentRegistry.getSlime(slimeId);
            if (definition != null) {
                result.add(definition);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public boolean removeCapturedSlime(Player player, String slimeId) {
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile != null && profile.removeCapturedSlime(slimeId)) {
            this.profileService.saveLoaded(player.getUniqueId());
            return true;
        }

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!slimeId.equals(readCapturedSlimeId(item))) {
                continue;
            }

            if (item.getAmount() <= 1) {
                player.getInventory().setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItem(slot, item);
            }
            return true;
        }
        return false;
    }

    public int clearCapturedSlimes(Player player) {
        int removed = 0;
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile != null) {
            removed += profile.clearCapturedSlimes();
            removed += profile.clearPenSlimes();
            this.profileService.saveLoaded(player.getUniqueId());
        }

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (readCapturedSlimeId(item) == null) {
                continue;
            }
            removed += item.getAmount();
            player.getInventory().setItem(slot, null);
        }
        player.updateInventory();
        return removed;
    }

    @SuppressWarnings("unused")
    private static ItemStack createCapturedSlime(SlimeDef definition) {
        ItemStack item = new ItemStack(definition.getItemMaterial(), 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + definition.getDisplayName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "ID: " + definition.getId(),
                ChatColor.DARK_GRAY + "slimes:captured"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static String readCapturedSlimeId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore() || !meta.hasDisplayName()) {
            return null;
        }

        String slimeId = null;
        boolean captured = false;
        for (String line : meta.getLore()) {
            if (line.startsWith(ChatColor.GRAY + "ID: ")) {
                slimeId = ChatColor.stripColor(line).substring("ID: ".length());
            }
            if ((ChatColor.DARK_GRAY + "slimes:captured").equals(line)) {
                captured = true;
            }
        }
        return captured ? slimeId : null;
    }
}
