package dev.lhoopy.quest.battlepass;

public enum BattlePassRewardTrack {
    DEFAULT,
    PREMIUM;

    public boolean isPremium() {
        return this == PREMIUM;
    }

    public static BattlePassRewardTrack fromString(String value) {
        if (value != null && value.equalsIgnoreCase("premium")) {
            return PREMIUM;
        }
        return DEFAULT;
    }
}
