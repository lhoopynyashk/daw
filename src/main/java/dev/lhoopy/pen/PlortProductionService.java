package dev.lhoopy.pen;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.ContentIds;
import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.storage.VacpackLimits;
import dev.lhoopy.storage.StoredItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class PlortProductionService implements PluginService {
    private static final String CONFIG_ENABLED = "plort-production.enabled";
    private static final String CONFIG_INTERVAL_SECONDS = "plort-production.interval-seconds";
    private static final String CONFIG_MAX_BATCHES = "plort-production.max-batches-per-tick";
    private static final String CONFIG_PEN_VISUAL_STACKS = "plort-production.pen-visual-stacks";
    private static final String CONFIG_PLORTS_PER_STACK = "plort-production.plorts-per-stack";

    private final SlimesPlugin plugin;
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final PenStyleCatalog styleCatalog;
    private int taskId = -1;

    public PlortProductionService(SlimesPlugin plugin, ContentRegistry contentRegistry, ProfileService profileService,
                                  PenStyleCatalog styleCatalog) {
        this.plugin = plugin;
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.styleCatalog = styleCatalog;
    }

    @Override
    public void enable() {
        if (!this.plugin.getConfig().getBoolean(CONFIG_ENABLED, true)) {
            this.plugin.getLogger().info("Plort production disabled");
            return;
        }
        this.taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickOnlinePlayers, 20L * 15L, 20L * 15L).getTaskId();
        this.plugin.getLogger().info("Plort production enabled");
    }

    @Override
    public void shutdown() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
            this.taskId = -1;
        }
    }

    public void updateProduction(Player player, PlayerProfile profile, boolean notify) {
        long now = System.currentTimeMillis();
        if (profile.getPenSlimeIds().isEmpty()) {
            if (profile.getLastPlortProductionMillis() == 0L) {
                profile.setLastPlortProductionMillis(now);
            }
            return;
        }

        PenStyleDef activeStyle = this.styleCatalog.get(profile.getActivePenStyleId());
        long baseInterval = Math.max(10L, this.plugin.getConfig().getLong(CONFIG_INTERVAL_SECONDS, 60L)) * 1000L;
        long intervalMillis = Math.max(1000L, (long) (baseInterval / activeStyle.getProductionMultiplier()));
        long lastProduction = profile.getLastPlortProductionMillis();
        if (lastProduction <= 0L) {
            profile.setLastPlortProductionMillis(now);
            this.profileService.saveLoaded(player.getUniqueId());
            return;
        }

        long batches = (now - lastProduction) / intervalMillis;
        if (batches <= 0L) {
            return;
        }

        int maxBatches = Math.max(1, this.plugin.getConfig().getInt(CONFIG_MAX_BATCHES, 30));
        int appliedBatches = (int) Math.min(batches, maxBatches);
        Map<String, Integer> produced = new LinkedHashMap<>();

        for (PenSlime slime : profile.getPenSlimes()) {
            String plortId = ContentIds.resolvePlortForSlime(this.contentRegistry, slime.getSlimeId());
            int amount = slime.produceForBatches(appliedBatches, now);
            int baseAmount = amount;
            for (int batch = 0; batch < baseAmount; batch++) {
                if (ThreadLocalRandom.current().nextDouble() < activeStyle.getExtraPlortChance()) {
                    amount++;
                }
            }
            if (amount > 0) {
                produced.merge(plortId, amount, Integer::sum);
            }
        }

        if (produced.isEmpty()) {
            profile.setLastPlortProductionMillis(lastProduction + (appliedBatches * intervalMillis));
            this.profileService.saveLoaded(player.getUniqueId());
            return;
        }

        int storedTotal = 0;
        int rejectedTotal = 0;
        for (Map.Entry<String, Integer> entry : produced.entrySet()) {
            int stored = addToPenStorage(profile, entry.getKey(), entry.getValue());
            storedTotal += stored;
            rejectedTotal += entry.getValue() - stored;
        }
        if (storedTotal <= 0) {
            if (notify) {
                player.sendMessage(ChatColor.RED + "Накопитель загона полон. Плорты не поместились.");
            }
            return;
        }
        profile.setLastPlortProductionMillis(lastProduction + (appliedBatches * intervalMillis));
        this.profileService.saveLoaded(player.getUniqueId());

        if (notify) {
            player.sendMessage(ChatColor.GREEN + "В загоне появились плорты: " + ChatColor.WHITE + storedTotal);
            if (rejectedTotal > 0) {
                player.sendMessage(ChatColor.RED + "Не поместилось: " + ChatColor.WHITE + rejectedTotal);
            }
        }
    }

    public int collectPenPlorts(Player player, PlayerProfile profile, boolean notify) {
        updateProduction(player, profile, false);
        int moved = 0;
        int left = 0;
        for (StoredItem item : new ArrayList<>(profile.getPenPlortStorage().getItems())) {
            if (this.contentRegistry.getPlort(item.getItemId()) == null || item.getAmount() <= 0) {
                continue;
            }
            int accepted = VacpackLimits.add(profile, item.getItemId(), item.getAmount());
            if (accepted > 0) {
                profile.getPenPlortStorage().remove(item.getItemId(), accepted);
                moved += accepted;
            }
            left += profile.getPenPlortStorage().getAmount(item.getItemId());
        }
        if (moved > 0) {
            this.profileService.saveLoaded(player.getUniqueId());
            player.playSound(player.getLocation(), "random.orb", 0.6F, 1.2F);
        }
        if (notify) {
            if (moved <= 0) {
                player.sendMessage(ChatColor.YELLOW + "В загоне нет плортов или вакпак полон.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Собрано плортов из загона: " + ChatColor.WHITE + moved);
                if (left > 0) {
                    player.sendMessage(ChatColor.YELLOW + "Осталось в загоне: " + ChatColor.WHITE + left);
                }
            }
        }
        return moved;
    }

    private void tickOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
            if (profile != null) {
                updateProduction(player, profile, false);
            }
        }
    }

    private int addToPenStorage(PlayerProfile profile, String plortId, int amount) {
        int capacity = getPenPlortCapacity();
        int used = profile.getPenPlortStorage().getTotalAmount();
        return profile.getPenPlortStorage().addLimited(plortId, amount, used, capacity);
    }

    private int getPenPlortCapacity() {
        int stacks = Math.max(1, this.plugin.getConfig().getInt(CONFIG_PEN_VISUAL_STACKS, 3));
        int perStack = Math.max(1, this.plugin.getConfig().getInt(CONFIG_PLORTS_PER_STACK, 20));
        return stacks * perStack;
    }

}
