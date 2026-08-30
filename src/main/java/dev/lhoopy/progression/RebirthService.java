package dev.lhoopy.progression;

import dev.lhoopy.profile.PlayerProfile;

public final class RebirthService {
    public void addRebirth(PlayerProfile profile) {
        ProgressData progress = profile.getProgressData();
        progress.setRebirths(progress.getRebirths() + 1);
    }
}
