package dev.lhoopy.pen;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.SlimeDef;
import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.slime.SlimeService;
import gg.cristalix.wada.transfer.ModTransfer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class PenMenuBridge {
    private final SlimesPlugin plugin;
    private final EnginexHuntBridge enginexHuntBridge;
    private final ContentRegistry contentRegistry;
    private final SlimeService slimeService;
    private final ProfileService profileService;
    private final PenStyleCatalog styleCatalog;

    public PenMenuBridge(SlimesPlugin plugin, EnginexHuntBridge enginexHuntBridge, ContentRegistry contentRegistry,
                         SlimeService slimeService, ProfileService profileService, PenStyleCatalog styleCatalog) {
        this.plugin = plugin;
        this.enginexHuntBridge = enginexHuntBridge;
        this.contentRegistry = contentRegistry;
        this.slimeService = slimeService;
        this.profileService = profileService;
        this.styleCatalog = styleCatalog;
    }

    public void open(Player player) {
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher загружается, повтори через пару секунд.");
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher ещё загружается.");
            return;
        }
        List<SlimeDef> capturedSlimes = this.slimeService.findCapturedSlimes(player);
        if (!this.enginexHuntBridge.isClientModLoaded(player)) {
            if (!this.enginexHuntBridge.sendClientModTo(player)) {
                player.sendMessage(ChatColor.RED + "Клиентский мод SlimeHunt не загрузился.");
                return;
            }
            player.sendMessage(ChatColor.YELLOW + "Загружаю клиентский мод SlimeHunt...");
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline()) {
                    open(player);
                }
            }, 80L);
            return;
        }

        sendPenScreen(player, profile, capturedSlimes);
    }

    public void moveCapturedSlime(Player player, String slimeId) {
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher загружается.");
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher ещё загружается.");
            return;
        }
        SlimeDef definition = this.contentRegistry.getSlime(slimeId);
        if (definition == null) {
            player.sendMessage(ChatColor.RED + "Неизвестный тип слайма: " + slimeId);
            open(player);
            return;
        }
        int effectiveCapacity = this.styleCatalog.effectiveCapacity(profile);
        if (profile.isPenFull(effectiveCapacity)) {
            player.sendMessage(ChatColor.RED + "Загон заполнен.");
            open(player);
            return;
        }
        if (!this.slimeService.removeCapturedSlime(player, definition.getId())) {
            player.sendMessage(ChatColor.RED + "Этого слайма уже нет в инвентаре.");
            open(player);
            return;
        }
        profile.addPenSlime(definition.getId(), effectiveCapacity);
        this.profileService.saveLoaded(player.getUniqueId());
        player.playSound(player.getLocation(), "random.orb", 0.8f, 1.25f);
        player.sendMessage(ChatColor.GREEN + "Слайм переселен в загон: " + ChatColor.WHITE + definition.getDisplayName());
        open(player);
    }

    public void removePenSlime(Player player, int index) {
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher загружается.");
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher ещё загружается.");
            return;
        }
        String slimeId = profile.removePenSlime(index);
        if (slimeId == null) {
            player.sendMessage(ChatColor.RED + "В этом слоте нет слайма.");
            open(player);
            return;
        }
        profile.addCapturedSlime(slimeId);
        this.profileService.saveLoaded(player.getUniqueId());
        player.playSound(player.getLocation(), "random.pop", 0.7F, 1.1F);
        SlimeDef definition = this.contentRegistry.getSlime(slimeId);
        String displayName = definition == null ? slimeId : definition.getDisplayName();
        player.sendMessage(ChatColor.YELLOW + "Слайм убран из загона: " + ChatColor.WHITE + displayName);
        open(player);
    }

    private void sendPenScreen(Player player, PlayerProfile profile, List<SlimeDef> capturedSlimes) {
        ModTransfer transfer = new ModTransfer()
                .writeInt(this.styleCatalog.effectiveCapacity(profile))
                .writeInt(profile.getPenSlimeIds().size());

        for (String slimeId : profile.getPenSlimeIds()) {
            SlimeDef definition = this.contentRegistry.getSlime(slimeId);
            if (definition == null) {
                transfer.writeString(slimeId).writeString(slimeId).writeString("unknown");
                continue;
            }
            writeSlime(transfer, definition);
        }

        transfer.writeInt(capturedSlimes.size());
        for (SlimeDef definition : capturedSlimes) {
            writeSlime(transfer, definition);
        }

        transfer.writeInt(productionPerMinuteX100(profile))
                .writeInt(countFoodInInventory(player))
                .writeInt(profile.getPenPlortStorage().getTotalAmount())
                .writeInt(penPlortCapacity());

        transfer.send(PenService.PEN_OPEN_CHANNEL, player);
    }

    /**
     * Сколько плортов в минуту приносит загон прямо сейчас: учитывает сытость
     * каждого слайма, множитель стиля и шанс дополнительного плорта.
     */
    private int productionPerMinuteX100(PlayerProfile profile) {
        if (profile.getPenSlimes().isEmpty()) {
            return 0;
        }
        PenStyleDef style = this.styleCatalog.get(profile.getActivePenStyleId());
        long baseInterval = Math.max(10L, this.plugin.getConfig().getLong("plort-production.interval-seconds", 60L)) * 1000L;
        long intervalMillis = Math.max(1000L, (long) (baseInterval / style.getProductionMultiplier()));
        long now = System.currentTimeMillis();

        double perBatch = 0.0D;
        for (PenSlime slime : profile.getPenSlimes()) {
            perBatch += slime.getFeedQuality(now).getProductionMultiplier();
        }
        double perMinute = perBatch * (1.0D + style.getExtraPlortChance()) * (60000.0D / intervalMillis);
        return (int) Math.round(perMinute * 100.0D);
    }

    /** Сколько предметов в инвентаре подходят слаймам как еда. */
    private int countFoodInInventory(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            for (SlimeDef slime : this.contentRegistry.slimes()) {
                if (slime.getFavoriteFood() == item.getType()) {
                    total += item.getAmount();
                    break;
                }
            }
        }
        return total;
    }

    private int penPlortCapacity() {
        int stacks = Math.max(1, this.plugin.getConfig().getInt("plort-production.pen-visual-stacks", 3));
        int perStack = Math.max(1, this.plugin.getConfig().getInt("plort-production.plorts-per-stack", 20));
        return stacks * perStack;
    }

    private static void writeSlime(ModTransfer transfer, SlimeDef definition) {
        transfer.writeString(definition.getId())
                .writeString(definition.getDisplayName())
                .writeString(definition.getRarity());
    }
}
