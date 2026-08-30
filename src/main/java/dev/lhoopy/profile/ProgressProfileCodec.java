package dev.lhoopy.profile;

import ru.cristalix.core.database.document.IDocument;
import ru.cristalix.core.database.nosql.mongo.MongoDocument;

import java.util.ArrayList;

final class ProgressProfileCodec {
    private static final String UNLOCKS = "unlocks";
    private static final String FLAGS = "flags";
    private static final String SKILL_POINTS = "skillPoints";
    private static final String REBIRTHS = "rebirths";

    private ProgressProfileCodec() {
    }

    static void readInto(PlayerProfile profile, IDocument progression) {
        profile.getProgressData().setUnlockedIds(DocumentValues.readStringList(progression.get(UNLOCKS)));
        profile.getProgressData().setFlags(DocumentValues.readStringList(progression.get(FLAGS)));
        profile.getProgressData().setSkillPoints(DocumentValues.readInt(progression.get(SKILL_POINTS), 0));
        profile.getProgressData().setRebirths(DocumentValues.readInt(progression.get(REBIRTHS), 0));
    }

    static IDocument write(PlayerProfile profile) {
        IDocument progression = new MongoDocument();
        progression.put(UNLOCKS, new ArrayList<>(profile.getProgressData().getUnlockedIds()));
        progression.put(FLAGS, new ArrayList<>(profile.getProgressData().getFlags()));
        progression.put(SKILL_POINTS, profile.getProgressData().getSkillPoints());
        progression.put(REBIRTHS, profile.getProgressData().getRebirths());
        return progression;
    }
}
