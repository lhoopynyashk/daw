package dev.lhoopy.profile;

import dev.lhoopy.quest.QuestProgress;
import ru.cristalix.core.database.document.IDocument;
import ru.cristalix.core.database.nosql.mongo.MongoDocument;

import java.util.ArrayList;

final class QuestProfileCodec {
    private static final String QUEST_ENTRIES = "entries";
    private static final String QUEST_VALUE = "value";
    private static final String QUEST_COMPLETED = "completed";
    private static final String QUEST_REWARD_CLAIMED = "rewardClaimed";
    private static final String BATTLE_PASS_LEVEL = "battlePassLevel";
    private static final String BATTLE_PASS_EXPERIENCE = "battlePassExperience";
    private static final String BATTLE_PASS_PREMIUM = "battlePassPremium";
    private static final String BATTLE_PASS_CLAIMED_REWARDS = "battlePassClaimedRewards";

    private QuestProfileCodec() {
    }

    static void readInto(PlayerProfile profile, IDocument quests) {
        profile.getQuestData().setBattlePassLevel(DocumentValues.readInt(quests.get(BATTLE_PASS_LEVEL), 0));
        profile.getQuestData().setBattlePassExperience(DocumentValues.readInt(quests.get(BATTLE_PASS_EXPERIENCE), 0));
        profile.getQuestData().setPremiumBattlePass(DocumentValues.readBoolean(quests.get(BATTLE_PASS_PREMIUM), false));
        profile.getQuestData().setClaimedBattlePassRewards(DocumentValues.readStringList(quests.get(BATTLE_PASS_CLAIMED_REWARDS)));

        IDocument entries = quests.getDocument(QUEST_ENTRIES);
        if (entries == null) {
            return;
        }
        profile.getQuestData().clearQuests();
        for (String questId : entries.keys()) {
            IDocument questDocument = entries.getDocument(questId);
            if (questDocument == null) {
                continue;
            }
            QuestProgress progress = profile.getQuestData().getOrCreate(questId);
            progress.setValue(DocumentValues.readInt(questDocument.get(QUEST_VALUE), 0));
            progress.setCompleted(DocumentValues.readBoolean(questDocument.get(QUEST_COMPLETED), false));
            progress.setRewardClaimed(DocumentValues.readBoolean(questDocument.get(QUEST_REWARD_CLAIMED), false));
        }
    }

    static IDocument write(PlayerProfile profile) {
        IDocument quests = new MongoDocument();
        IDocument entries = new MongoDocument();
        quests.put(BATTLE_PASS_LEVEL, profile.getQuestData().getBattlePassLevel());
        quests.put(BATTLE_PASS_EXPERIENCE, profile.getQuestData().getBattlePassExperience());
        quests.put(BATTLE_PASS_PREMIUM, profile.getQuestData().hasPremiumBattlePass());
        quests.put(BATTLE_PASS_CLAIMED_REWARDS, new ArrayList<>(profile.getQuestData().getClaimedBattlePassRewards()));
        for (QuestProgress progress : profile.getQuestData().getQuests()) {
            IDocument quest = new MongoDocument();
            quest.put(QUEST_VALUE, progress.getValue());
            quest.put(QUEST_COMPLETED, progress.isCompleted());
            quest.put(QUEST_REWARD_CLAIMED, progress.isRewardClaimed());
            entries.put(progress.getQuestId(), quest);
        }
        quests.put(QUEST_ENTRIES, entries);
        return quests;
    }
}
