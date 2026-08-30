package dev.lhoopy.quest;

public final class QuestProgress {
    private final String questId;
    private int value;
    private boolean completed;
    private boolean rewardClaimed;

    public QuestProgress(String questId) {
        this.questId = questId;
    }

    public String getQuestId() {
        return this.questId;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = Math.max(0, value);
    }

    public void addValue(int amount) {
        this.value = Math.max(0, this.value + amount);
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isRewardClaimed() {
        return this.rewardClaimed;
    }

    public void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }
}
