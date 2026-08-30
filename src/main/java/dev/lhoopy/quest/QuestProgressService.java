package dev.lhoopy.quest;

import dev.lhoopy.profile.PlayerProfile;

public final class QuestProgressService {
    public QuestProgress addProgress(PlayerProfile profile, String questId, int amount, int target) {
        QuestProgress progress = profile.getQuestData().getOrCreate(questId);
        progress.addValue(amount);
        if (target > 0 && progress.getValue() >= target) {
            progress.setCompleted(true);
        }
        return progress;
    }
}
