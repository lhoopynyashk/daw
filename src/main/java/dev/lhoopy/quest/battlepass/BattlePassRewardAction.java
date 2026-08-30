package dev.lhoopy.quest.battlepass;

import dev.lhoopy.profile.PlayerProfile;

public final class BattlePassRewardAction {
    private final String type;
    private final String itemId;
    private final int amount;

    public BattlePassRewardAction(String type, String itemId, int amount) {
        this.type = type == null ? "storage" : type;
        this.itemId = itemId;
        this.amount = Math.max(0, amount);
    }

    public String getType() {
        return this.type;
    }

    public String getItemId() {
        return this.itemId;
    }

    public int getAmount() {
        return this.amount;
    }

    public void apply(PlayerProfile profile) {
        if (profile == null || this.amount <= 0) {
            return;
        }
        if (this.type.equalsIgnoreCase("coins")) {
            profile.setCoins(profile.getCoins() + this.amount);
            return;
        }
        if (this.itemId != null && !this.itemId.isBlank()) {
            profile.getStorage().add(this.itemId, this.amount);
        }
    }
}
