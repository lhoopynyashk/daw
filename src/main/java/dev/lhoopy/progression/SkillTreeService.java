package dev.lhoopy.progression;

import dev.lhoopy.profile.PlayerProfile;

public final class SkillTreeService {
    public boolean spendPoint(PlayerProfile profile, String skillId) {
        ProgressData progress = profile.getProgressData();
        if (progress.getSkillPoints() <= 0 || progress.isUnlocked(skillId)) {
            return false;
        }
        progress.setSkillPoints(progress.getSkillPoints() - 1);
        progress.unlock(skillId);
        return true;
    }
}
