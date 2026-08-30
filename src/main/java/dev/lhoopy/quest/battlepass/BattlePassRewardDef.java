package dev.lhoopy.quest.battlepass;

import gg.cristalix.wada.common.economy.Quality;
import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

public final class BattlePassRewardDef {
    private final int level;
    private final int index;
    private final BattlePassRewardTrack track;
    private final String title;
    private final String description;
    private final Quality quality;
    private final Material iconMaterial;
    private final List<BattlePassRewardAction> actions;

    public BattlePassRewardDef(
            int level,
            int index,
            BattlePassRewardTrack track,
            String title,
            String description,
            Quality quality,
            Material iconMaterial,
            List<BattlePassRewardAction> actions
    ) {
        this.level = Math.max(1, level);
        this.index = Math.max(0, index);
        this.track = track == null ? BattlePassRewardTrack.DEFAULT : track;
        this.title = title == null ? "Награда" : title;
        this.description = description == null ? "" : description;
        this.quality = quality == null ? Quality.COMMON : quality;
        this.iconMaterial = iconMaterial == null ? Material.CHEST : iconMaterial;
        this.actions = actions == null ? Collections.emptyList() : Collections.unmodifiableList(actions);
    }

    public int getLevel() {
        return this.level;
    }

    public int getIndex() {
        return this.index;
    }

    public BattlePassRewardTrack getTrack() {
        return this.track;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public Quality getQuality() {
        return this.quality;
    }

    public Material getIconMaterial() {
        return this.iconMaterial;
    }

    public List<BattlePassRewardAction> getActions() {
        return this.actions;
    }

    public String claimKey() {
        return this.track.name().toLowerCase() + ":" + this.level + ":" + this.index;
    }
}
