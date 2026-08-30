package dev.lhoopy.quest.battlepass;

public final class BattlePassQuestDef {
    private final String id;
    private final String category;
    private final String title;
    private final String description;
    private final int target;
    private final int experience;

    public BattlePassQuestDef(String id, String category, String title, String description, int target, int experience) {
        this.id = id;
        this.category = category == null ? "daily" : category;
        this.title = title == null ? id : title;
        this.description = description == null ? "" : description;
        this.target = Math.max(1, target);
        this.experience = Math.max(0, experience);
    }

    public String getId() {
        return this.id;
    }

    public String getCategory() {
        return this.category;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public int getTarget() {
        return this.target;
    }

    public int getExperience() {
        return this.experience;
    }
}
