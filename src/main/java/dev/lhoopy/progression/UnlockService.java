package dev.lhoopy.progression;

import dev.lhoopy.profile.PlayerProfile;

public final class UnlockService {
    public boolean unlock(PlayerProfile profile, String unlockId) {
        return profile.getProgressData().unlock(unlockId);
    }

    public boolean isUnlocked(PlayerProfile profile, String unlockId) {
        return profile.getProgressData().isUnlocked(unlockId);
    }
}
