package dev.lhoopy.quest;

import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.quest.battlepass.BattlePassContent;
import dev.lhoopy.quest.battlepass.BattlePassContentLoader;
import dev.lhoopy.quest.battlepass.BattlePassQuestDef;
import dev.lhoopy.quest.battlepass.BattlePassRewardDef;
import gg.cristalix.wada.Wada;
import gg.cristalix.wada.common.economy.Pricing;
import gg.cristalix.wada.common.economy.TimePeriod;
import gg.cristalix.wada.component.keybinding.data.KeyBinding;
import gg.cristalix.wada.component.keybinding.data.KeyBindingAnimation;
import gg.cristalix.wada.component.keybinding.event.PlayerKeyBindingPressedEvent;
import gg.cristalix.wada.common.menu.icon.ItemIcon;
import gg.cristalix.wada.common.menu.tooltip.Tooltip;
import gg.cristalix.wada.component.menu.battlepass.common.BattlePass;
import gg.cristalix.wada.component.menu.battlepass.data.LevelSkip;
import gg.cristalix.wada.component.menu.battlepass.data.quest.Quest;
import gg.cristalix.wada.component.menu.battlepass.data.quest.QuestCategory;
import gg.cristalix.wada.component.menu.battlepass.data.purchase.PurchaseModalOffer;
import gg.cristalix.wada.component.menu.battlepass.data.purchase.PurchaseModalSettings;
import gg.cristalix.wada.component.menu.battlepass.data.reward.Reward;
import gg.cristalix.wada.component.menu.battlepass.data.reward.RewardTier;
import gg.cristalix.wada.component.menu.battlepass.data.reward.RewardType;
import gg.cristalix.wada.component.menu.battlepass.event.PlayerBattlePassResetEvent;
import gg.cristalix.wada.component.menu.battlepass.event.purchase.PlayerBattlePassOpenPurchaseEvent;
import gg.cristalix.wada.component.menu.battlepass.event.purchase.PlayerBattlePassPuchaseEvent;
import gg.cristalix.wada.component.menu.battlepass.event.reward.PlayerBattlePassClaimAllRewardsEvent;
import gg.cristalix.wada.component.menu.battlepass.event.reward.PlayerBattlePassRewardClaimEvent;
import gg.cristalix.wada.component.menu.battlepass.event.skip.PlayerBattlePassOpenSkipLevelEvent;
import gg.cristalix.wada.component.menu.battlepass.event.skip.PlayerBattlePassSkipLevelEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BattlePassService implements Listener {
    private static final int KEY_B = 48;

    private final Plugin plugin;
    private final ProfileService profileService;
    private final Map<UUID, BattlePass> cache = new LinkedHashMap<>();
    private final Map<UUID, Long> lastKeyPress = new LinkedHashMap<>();
    private final KeyBinding battlePassKeyBinding;
    private BattlePassContent content;

    public BattlePassService(Plugin plugin, ProfileService profileService) {
        this.plugin = plugin;
        this.profileService = profileService;
        this.battlePassKeyBinding = KeyBinding.builder()
                .defaultKey(KEY_B)
                .category("SlimeRancher")
                .description("BattlePass")
                .onPlayerPressed((player, keyBinding) -> handleKeyPress(player))
                .build();
    }

    public void enable() {
        reload();
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            addBattlePassKeyBinding(player);
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Wada.get().getKeyBindingManager().remove(player, this.battlePassKeyBinding);
        }
        HandlerList.unregisterAll(this);
        this.cache.clear();
        this.lastKeyPress.clear();
    }

    public void reload() {
        this.content = new BattlePassContentLoader(this.plugin).load();
        this.cache.clear();
        this.plugin.getLogger().info("BattlePass content loaded: rewards=" + this.content.getRewards().size()
                + ", quests=" + this.content.getQuests().size());
    }

    public void handleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }
        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("slimes.battlepass.admin")) {
                player.sendMessage(ChatColor.RED + "Нет прав.");
                return;
            }
            reload();
            player.sendMessage(ChatColor.GREEN + "Конфиги BattlePass перезагружены.");
            return;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("addxp")) {
            if (!player.hasPermission("slimes.battlepass.admin")) {
                player.sendMessage(ChatColor.RED + "Нет прав.");
                return;
            }
            PlayerProfile profile = requireProfile(player);
            if (profile == null) {
                return;
            }
            addExperience(profile, parseInt(args[1], 0));
            this.profileService.saveLoaded(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Опыт BattlePass: " + profile.getQuestData().getBattlePassExperience()
                    + "/" + this.content.getExperiencePerLevel()
                    + ", уровень=" + profile.getQuestData().getBattlePassLevel());
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("premium")) {
            if (!player.hasPermission("slimes.battlepass.admin")) {
                player.sendMessage(ChatColor.RED + "Нет прав.");
                return;
            }
            PlayerProfile profile = requireProfile(player);
            if (profile == null) {
                return;
            }
            profile.getQuestData().setPremiumBattlePass(true);
            this.profileService.saveLoaded(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Премиум BattlePass включён.");
            return;
        }
        if (args.length > 2 && args[0].equalsIgnoreCase("quest")) {
            if (!player.hasPermission("slimes.battlepass.admin")) {
                player.sendMessage(ChatColor.RED + "Нет прав.");
                return;
            }
            PlayerProfile profile = requireProfile(player);
            if (profile == null) {
                return;
            }
            addQuestProgress(profile, args[1], parseInt(args[2], 1));
            this.profileService.saveLoaded(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Прогресс задания обновлён.");
            return;
        }
        open(player);
    }

    public void open(Player player) {
        PlayerProfile profile = requireProfile(player);
        if (profile == null) {
            return;
        }
        BattlePass battlePass = createBattlePass(profile);
        this.cache.put(player.getUniqueId(), battlePass);
        Wada.get().getMenuManager().open(battlePass, player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> addBattlePassKeyBinding(event.getPlayer()), 40L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.cache.remove(event.getPlayer().getUniqueId());
        this.lastKeyPress.remove(event.getPlayer().getUniqueId());
        Wada.get().getKeyBindingManager().remove(event.getPlayer(), this.battlePassKeyBinding);
    }

    @EventHandler
    public void onKeyBindingPressed(PlayerKeyBindingPressedEvent event) {
        if (!event.getKeyBinding().getUuid().equals(this.battlePassKeyBinding.getUuid())) {
            return;
        }
        handleKeyPress(event.getPlayer());
    }

    public void addExperience(PlayerProfile profile, int experience) {
        QuestData questData = profile.getQuestData();
        questData.setBattlePassExperience(questData.getBattlePassExperience() + Math.max(0, experience));
        while (questData.getBattlePassExperience() >= this.content.getExperiencePerLevel()
                && questData.getBattlePassLevel() < this.content.getMaxLevel()) {
            questData.setBattlePassExperience(questData.getBattlePassExperience() - this.content.getExperiencePerLevel());
            questData.setBattlePassLevel(questData.getBattlePassLevel() + 1);
        }
        if (questData.getBattlePassLevel() >= this.content.getMaxLevel()) {
            questData.setBattlePassExperience(0);
        }
    }

    public QuestProgress addQuestProgress(PlayerProfile profile, String questId, int amount) {
        BattlePassQuestDef quest = this.content.findQuest(questId);
        if (quest == null) {
            QuestProgress progress = profile.getQuestData().getOrCreate(questId);
            progress.addValue(amount);
            return progress;
        }
        QuestProgress progress = profile.getQuestData().getOrCreate(quest.getId());
        boolean wasCompleted = progress.isCompleted();
        progress.addValue(amount);
        if (progress.getValue() >= quest.getTarget()) {
            progress.setCompleted(true);
        }
        if (!wasCompleted && progress.isCompleted() && !progress.isRewardClaimed()) {
            addExperience(profile, quest.getExperience());
            progress.setRewardClaimed(true);
        }
        return progress;
    }

    @EventHandler
    public void onRewardClaim(PlayerBattlePassRewardClaimEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        BattlePass battlePass = this.cache.get(player.getUniqueId());
        if (profile == null || battlePass == null) {
            event.setResult(PlayerBattlePassRewardClaimEvent.ClaimResult.INTERNAL_ERROR);
            return;
        }

        BattlePassRewardDef reward = this.content.findReward(event.getLevel(), event.getRewardIndex(), event.isPremium());
        if (reward == null) {
            event.setResult(PlayerBattlePassRewardClaimEvent.ClaimResult.INTERNAL_ERROR);
            return;
        }
        if (event.isPremium() && !profile.getQuestData().hasPremiumBattlePass()) {
            event.setResult(PlayerBattlePassRewardClaimEvent.ClaimResult.NO_PREMIUM_PASS);
            return;
        }
        if (event.getLevel() > normalizedLevel(profile)) {
            event.setResult(PlayerBattlePassRewardClaimEvent.ClaimResult.INSUFFICIENT_LEVEL);
            return;
        }
        if (profile.getQuestData().isBattlePassRewardClaimed(reward.claimKey())) {
            event.setResult(PlayerBattlePassRewardClaimEvent.ClaimResult.ALREADY_CLAIMED);
            return;
        }

        giveReward(profile, reward);
        profile.getQuestData().markBattlePassRewardClaimed(reward.claimKey());
        battlePass.getRewards().claimReward(reward.getLevel(), reward.getIndex(), reward.getTrack().isPremium());
        this.profileService.saveLoaded(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Получена награда BattlePass: " + ChatColor.WHITE + reward.getTitle());
        event.setResult(PlayerBattlePassRewardClaimEvent.ClaimResult.SUCCESS);
    }

    @EventHandler
    public void onClaimAll(PlayerBattlePassClaimAllRewardsEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        BattlePass battlePass = this.cache.get(player.getUniqueId());
        if (profile == null || battlePass == null) {
            event.setResult(PlayerBattlePassClaimAllRewardsEvent.ClaimResult.INTERNAL_ERROR);
            return;
        }

        List<PlayerBattlePassClaimAllRewardsEvent.ClaimedRewardInfo> claimed = new ArrayList<>();
        int playerLevel = normalizedLevel(profile);
        for (BattlePassRewardDef reward : this.content.getRewards()) {
            if (reward.getLevel() > playerLevel) {
                continue;
            }
            if (reward.getTrack().isPremium() && !profile.getQuestData().hasPremiumBattlePass()) {
                continue;
            }
            if (profile.getQuestData().isBattlePassRewardClaimed(reward.claimKey())) {
                continue;
            }
            giveReward(profile, reward);
            profile.getQuestData().markBattlePassRewardClaimed(reward.claimKey());
            battlePass.getRewards().claimReward(reward.getLevel(), reward.getIndex(), reward.getTrack().isPremium());
            RewardTier tier = battlePass.getRewards().getRewardTier(reward.getLevel(), reward.getTrack().isPremium());
            if (tier != null && reward.getIndex() < tier.getRewards().size()) {
                claimed.add(new PlayerBattlePassClaimAllRewardsEvent.ClaimedRewardInfo(
                        reward.getLevel(),
                        reward.getIndex(),
                        reward.getTrack().isPremium(),
                        tier.getRewards().get(reward.getIndex())
                ));
            }
        }
        if (claimed.isEmpty()) {
            event.setResult(PlayerBattlePassClaimAllRewardsEvent.ClaimResult.NO_REWARDS_TO_CLAIM);
            return;
        }
        event.addAllClaimedRewards(claimed);
        this.profileService.saveLoaded(player.getUniqueId());
        event.setResult(PlayerBattlePassClaimAllRewardsEvent.ClaimResult.SUCCESS);
    }

    @EventHandler
    public void onOpenPurchase(PlayerBattlePassOpenPurchaseEvent event) {
        PlayerProfile profile = this.profileService.getLoaded(event.getPlayer().getUniqueId());
        event.setPlayerBalance(profile == null ? 0 : safeInt(profile.getCoins()));
        event.setSettings(PurchaseModalSettings.builder()
                .description("Открой премиум-ветку BattlePass и забирай дополнительные награды сезона.")
                .regularOffer(PurchaseModalOffer.builder()
                        .title("Премиум")
                        .benefits(Arrays.asList(
                                "Дополнительные награды на каждом уровне",
                                "Больше ресурсов для ранчо",
                                "Премиум-прогресс сохраняется в профиле"
                        ))
                        .build())
                .specialOffer(PurchaseModalOffer.builder()
                        .title("Премиум + уровни")
                        .benefits(Arrays.asList(
                                "Премиум-ветка наград",
                                "+5 уровней BattlePass",
                                "Быстрый старт сезона"
                        ))
                        .build())
                .build());
        event.setResult(PlayerBattlePassOpenPurchaseEvent.OpenResult.ACCESS);
    }

    @EventHandler
    public void onPurchase(PlayerBattlePassPuchaseEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        BattlePass battlePass = this.cache.get(player.getUniqueId());
        if (profile == null || battlePass == null) {
            event.setResult(PlayerBattlePassPuchaseEvent.PurchaseResult.INTERNAL_ERROR);
            return;
        }
        if (profile.getQuestData().hasPremiumBattlePass()) {
            event.setResult(PlayerBattlePassPuchaseEvent.PurchaseResult.ALREADY_PURCHASED);
            return;
        }
        int price = event.getPurchaseType() == PlayerBattlePassPuchaseEvent.PurchaseType.SPECIAL
                ? this.content.getSpecialPrice()
                : this.content.getPurchasePrice();
        if (profile.getCoins() < price) {
            event.setResult(PlayerBattlePassPuchaseEvent.PurchaseResult.INSUFFICIENT_FUNDS);
            return;
        }
        profile.setCoins(profile.getCoins() - price);
        profile.getQuestData().setPremiumBattlePass(true);
        if (event.getPurchaseType() == PlayerBattlePassPuchaseEvent.PurchaseType.SPECIAL) {
            profile.getQuestData().setBattlePassLevel(Math.min(this.content.getMaxLevel(), normalizedLevel(profile) + 5));
        }
        battlePass.setPurchased(true);
        this.profileService.saveLoaded(player.getUniqueId());
        event.setResult(PlayerBattlePassPuchaseEvent.PurchaseResult.SUCCESS);
    }

    @EventHandler
    public void onOpenSkip(PlayerBattlePassOpenSkipLevelEvent event) {
        PlayerProfile profile = this.profileService.getLoaded(event.getPlayer().getUniqueId());
        event.setPlayerBalance(profile == null ? 0 : safeInt(profile.getCoins()));
        event.setResult(PlayerBattlePassOpenSkipLevelEvent.OpenResult.ACCESS);
    }

    @EventHandler
    public void onSkip(PlayerBattlePassSkipLevelEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            event.setResult(PlayerBattlePassSkipLevelEvent.SkipResult.INTERNAL_ERROR);
            return;
        }
        if (normalizedLevel(profile) >= this.content.getMaxLevel()) {
            event.setResult(PlayerBattlePassSkipLevelEvent.SkipResult.MAX_LEVEL_REACHED);
            return;
        }
        int levels = Math.max(1, event.getLevelsToSkip());
        int price = levels * 100;
        if (profile.getCoins() < price) {
            event.setResult(PlayerBattlePassSkipLevelEvent.SkipResult.INSUFFICIENT_FUNDS);
            return;
        }
        profile.setCoins(profile.getCoins() - price);
        profile.getQuestData().setBattlePassLevel(Math.min(this.content.getMaxLevel(), normalizedLevel(profile) + levels));
        this.profileService.saveLoaded(player.getUniqueId());
        event.setResult(PlayerBattlePassSkipLevelEvent.SkipResult.SUCCESS);
    }

    @EventHandler
    public void onReset(PlayerBattlePassResetEvent event) {
        PlayerProfile profile = this.profileService.getLoaded(event.getPlayer().getUniqueId());
        if (profile == null) {
            event.setResult(PlayerBattlePassResetEvent.ResetResult.INTERNAL_ERROR);
            return;
        }
        profile.getQuestData().setBattlePassLevel(0);
        profile.getQuestData().setBattlePassExperience(0);
        profile.getQuestData().clearClaimedBattlePassRewards();
        this.profileService.saveLoaded(event.getPlayer().getUniqueId());
        event.setResult(PlayerBattlePassResetEvent.ResetResult.SUCCESS);
    }

    private BattlePass createBattlePass(PlayerProfile profile) {
        BattlePass.Builder builder = BattlePass.builder()
                .level(normalizedLevel(profile))
                .maxLevel(this.content.getMaxLevel())
                .experience(profile.getQuestData().getBattlePassExperience())
                .maxExperience(this.content.getExperiencePerLevel())
                .levelBroadcastButtonEnabled(true)
                .claimAllRewardsButtonEnabled(true)
                .purchased(profile.getQuestData().hasPremiumBattlePass())
                .purchase(Pricing.builder().basePrice(this.content.getPurchasePrice()).build())
                .gift(Pricing.builder().basePrice(this.content.getGiftPrice()).build())
                .special(Pricing.builder().basePrice(this.content.getSpecialPrice()).build())
                .duration(TimePeriod.builder().durationDays(this.content.getDurationDays()).build());

        builder.skipLevels(createSkipLevels());
        builder.questCategories(createQuestCategories(profile));
        for (BattlePassRewardDef reward : this.content.getRewards()) {
            RewardTier tier = createRewardTier(profile, reward);
            if (reward.getTrack().isPremium()) {
                builder.premiumReward(tier);
            } else {
                builder.defaultReward(tier);
            }
        }
        return builder.build();
    }

    private List<LevelSkip> createSkipLevels() {
        List<LevelSkip> skips = new ArrayList<>();
        for (int i = 1; i <= Math.min(10, this.content.getMaxLevel()); i++) {
            skips.add(LevelSkip.builder()
                    .level(i)
                    .price(Pricing.builder().basePrice(i * 100).build())
                    .build());
        }
        return skips;
    }

    private RewardTier createRewardTier(PlayerProfile profile, BattlePassRewardDef reward) {
        Reward wadaReward = Reward.builder()
                .quality(reward.getQuality())
                .claimed(profile.getQuestData().isBattlePassRewardClaimed(reward.claimKey()))
                .icon(ItemIcon.builder().itemStack(new ItemStack(reward.getIconMaterial())).build())
                .tooltip(Tooltip.builder()
                        .title(reward.getTitle())
                        .description(reward.getDescription())
                        .cornerColor(reward.getQuality().getColor())
                        .build())
                .build();
        return RewardTier.builder()
                .level(reward.getLevel())
                .type(RewardType.DIRECT)
                .rewards(java.util.Collections.singletonList(wadaReward))
                .build();
    }

    private List<QuestCategory> createQuestCategories(PlayerProfile profile) {
        Map<String, List<Quest>> questsByCategory = new LinkedHashMap<>();
        for (BattlePassQuestDef questDef : this.content.getQuests()) {
            QuestProgress progress = profile.getQuestData().getOrCreate(questDef.getId());
            Quest quest = Quest.builder()
                    .description(questDef.getDescription())
                    .tooltip(Tooltip.builder()
                            .title(questDef.getTitle())
                            .description(questDef.getDescription())
                            .build())
                    .currentProgress(Math.min(progress.getValue(), questDef.getTarget()))
                    .maxProgress(questDef.getTarget())
                    .rewardExp(questDef.getExperience())
                    .displayProgress(Math.min(progress.getValue(), questDef.getTarget()) + "/" + questDef.getTarget())
                    .build();
            questsByCategory.computeIfAbsent(questDef.getCategory(), ignored -> new ArrayList<>()).add(quest);
        }

        List<QuestCategory> categories = new ArrayList<>();
        for (Map.Entry<String, List<Quest>> entry : questsByCategory.entrySet()) {
            categories.add(QuestCategory.builder()
                    .key(entry.getKey())
                    .displayName(categoryTitle(entry.getKey()))
                    .updateTime(TimePeriod.builder().durationDays(entry.getKey().equalsIgnoreCase("weekly") ? 7 : 1).build())
                    .quests(entry.getValue())
                    .build());
        }
        return categories;
    }

    private PlayerProfile requireProfile(Player player) {
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль ещё загружается.");
            return null;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile != null && profile.getQuestData().getBattlePassLevel() <= 0) {
            profile.getQuestData().setBattlePassLevel(1);
        }
        return profile;
    }

    private void addBattlePassKeyBinding(Player player) {
        if (!player.isOnline()) {
            return;
        }
        Wada.get().getKeyBindingManager().add(player, this.battlePassKeyBinding);
        Wada.get().getKeyBindingManager().playKeyBindingAnimation(
                player,
                KeyBindingAnimation.builder().hideTime(4.0).build()
        );
    }

    private void handleKeyPress(Player player) {
        long now = System.currentTimeMillis();
        Long previous = this.lastKeyPress.get(player.getUniqueId());
        if (previous != null && now - previous < 500L) {
            return;
        }
        this.lastKeyPress.put(player.getUniqueId(), now);
        Bukkit.getScheduler().runTask(this.plugin, () -> open(player));
    }

    private void giveReward(PlayerProfile profile, BattlePassRewardDef reward) {
        for (dev.lhoopy.quest.battlepass.BattlePassRewardAction action : reward.getActions()) {
            action.apply(profile);
        }
    }

    private int normalizedLevel(PlayerProfile profile) {
        return Math.max(1, profile.getQuestData().getBattlePassLevel());
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private static String categoryTitle(String category) {
        if (category == null) {
            return "Задания";
        }
        switch (category.toLowerCase(java.util.Locale.ROOT)) {
            case "daily":
                return "Ежедневные";
            case "weekly":
                return "Еженедельные";
            case "ranch":
                return "Ранчо";
            default:
                return category;
        }
    }
}
