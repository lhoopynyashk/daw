package dev.lhoopy.quest.battlepass;

import java.util.Collections;
import java.util.List;

public final class BattlePassContent {
    private final String title;
    private final int maxLevel;
    private final int experiencePerLevel;
    private final int durationDays;
    private final int purchasePrice;
    private final int giftPrice;
    private final int specialPrice;
    private final List<BattlePassRewardDef> rewards;
    private final List<BattlePassQuestDef> quests;

    public BattlePassContent(
            String title,
            int maxLevel,
            int experiencePerLevel,
            int durationDays,
            int purchasePrice,
            int giftPrice,
            int specialPrice,
            List<BattlePassRewardDef> rewards,
            List<BattlePassQuestDef> quests
    ) {
        this.title = title == null ? "SlimeRancher Pass" : title;
        this.maxLevel = Math.max(1, maxLevel);
        this.experiencePerLevel = Math.max(1, experiencePerLevel);
        this.durationDays = Math.max(1, durationDays);
        this.purchasePrice = Math.max(0, purchasePrice);
        this.giftPrice = Math.max(0, giftPrice);
        this.specialPrice = Math.max(0, specialPrice);
        this.rewards = rewards == null ? Collections.emptyList() : Collections.unmodifiableList(rewards);
        this.quests = quests == null ? Collections.emptyList() : Collections.unmodifiableList(quests);
    }

    public String getTitle() {
        return this.title;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public int getExperiencePerLevel() {
        return this.experiencePerLevel;
    }

    public int getDurationDays() {
        return this.durationDays;
    }

    public int getPurchasePrice() {
        return this.purchasePrice;
    }

    public int getGiftPrice() {
        return this.giftPrice;
    }

    public int getSpecialPrice() {
        return this.specialPrice;
    }

    public List<BattlePassRewardDef> getRewards() {
        return this.rewards;
    }

    public List<BattlePassQuestDef> getQuests() {
        return this.quests;
    }

    public BattlePassRewardDef findReward(int level, int index, boolean premium) {
        BattlePassRewardTrack track = premium ? BattlePassRewardTrack.PREMIUM : BattlePassRewardTrack.DEFAULT;
        for (BattlePassRewardDef reward : this.rewards) {
            if (reward.getLevel() == level && reward.getIndex() == index && reward.getTrack() == track) {
                return reward;
            }
        }
        return null;
    }

    public BattlePassQuestDef findQuest(String questId) {
        for (BattlePassQuestDef quest : this.quests) {
            if (quest.getId().equalsIgnoreCase(questId)) {
                return quest;
            }
        }
        return null;
    }
}
