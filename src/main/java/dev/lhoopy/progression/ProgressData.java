package dev.lhoopy.progression;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ProgressData {
    private final Set<String> unlockedIds = new LinkedHashSet<>();
    private final Set<String> flags = new LinkedHashSet<>();
    private int skillPoints;
    private int rebirths;

    public Set<String> getUnlockedIds() {
        return Collections.unmodifiableSet(this.unlockedIds);
    }

    public void setUnlockedIds(Iterable<String> ids) {
        this.unlockedIds.clear();
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            if (id != null && !id.isEmpty()) {
                this.unlockedIds.add(id);
            }
        }
    }

    public boolean unlock(String id) {
        return id != null && !id.isEmpty() && this.unlockedIds.add(id);
    }

    public boolean isUnlocked(String id) {
        return this.unlockedIds.contains(id);
    }

    public Set<String> getFlags() {
        return Collections.unmodifiableSet(this.flags);
    }

    public void setFlags(Iterable<String> flags) {
        this.flags.clear();
        if (flags == null) {
            return;
        }
        for (String flag : flags) {
            if (flag != null && !flag.isEmpty()) {
                this.flags.add(flag);
            }
        }
    }

    public boolean setFlag(String flag) {
        return flag != null && !flag.isEmpty() && this.flags.add(flag);
    }

    public boolean hasFlag(String flag) {
        return this.flags.contains(flag);
    }

    public int getSkillPoints() {
        return this.skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = Math.max(0, skillPoints);
    }

    public int getRebirths() {
        return this.rebirths;
    }

    public void setRebirths(int rebirths) {
        this.rebirths = Math.max(0, rebirths);
    }
}
