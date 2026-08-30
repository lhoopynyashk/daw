package dev.lhoopy.quest;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class QuestData {
    private final Map<String, QuestProgress> quests = new LinkedHashMap<>();
    private final Set<String> claimedBattlePassRewards = new LinkedHashSet<>();
    private int battlePassLevel;
    private int battlePassExperience;
    private boolean premiumBattlePass;

    public QuestProgress getOrCreate(String questId) {
        return this.quests.computeIfAbsent(questId, QuestProgress::new);
    }

    public Collection<QuestProgress> getQuests() {
        return Collections.unmodifiableCollection(this.quests.values());
    }

    public void clearQuests() {
        this.quests.clear();
    }

    public int getBattlePassLevel() {
        return this.battlePassLevel;
    }

    public void setBattlePassLevel(int battlePassLevel) {
        this.battlePassLevel = Math.max(0, battlePassLevel);
    }

    public int getBattlePassExperience() {
        return this.battlePassExperience;
    }

    public void setBattlePassExperience(int battlePassExperience) {
        this.battlePassExperience = Math.max(0, battlePassExperience);
    }

    public boolean hasPremiumBattlePass() {
        return this.premiumBattlePass;
    }

    public void setPremiumBattlePass(boolean premiumBattlePass) {
        this.premiumBattlePass = premiumBattlePass;
    }

    public Set<String> getClaimedBattlePassRewards() {
        return Collections.unmodifiableSet(this.claimedBattlePassRewards);
    }

    public void setClaimedBattlePassRewards(Collection<String> rewardKeys) {
        this.claimedBattlePassRewards.clear();
        if (rewardKeys != null) {
            for (String rewardKey : rewardKeys) {
                if (rewardKey != null && !rewardKey.isBlank()) {
                    this.claimedBattlePassRewards.add(rewardKey);
                }
            }
        }
    }

    public boolean isBattlePassRewardClaimed(String rewardKey) {
        return this.claimedBattlePassRewards.contains(rewardKey);
    }

    public void markBattlePassRewardClaimed(String rewardKey) {
        if (rewardKey != null && !rewardKey.isBlank()) {
            this.claimedBattlePassRewards.add(rewardKey);
        }
    }

    public void clearClaimedBattlePassRewards() {
        this.claimedBattlePassRewards.clear();
    }
}
