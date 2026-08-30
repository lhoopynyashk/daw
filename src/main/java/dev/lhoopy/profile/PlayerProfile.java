package dev.lhoopy.profile;

import dev.lhoopy.farm.FarmData;
import dev.lhoopy.pen.PenSlime;
import dev.lhoopy.progression.ProgressData;
import dev.lhoopy.quest.QuestData;
import dev.lhoopy.storage.PlayerStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerProfile {
    private final UUID playerId;
    private final FarmData farmData = new FarmData();
    private final PlayerStorage storage = new PlayerStorage();
    private final PlayerStorage vacpackStorage = new PlayerStorage();
    private final PlayerStorage penPlortStorage = new PlayerStorage();
    private final ProgressData progressData = new ProgressData();
    private final QuestData questData = new QuestData();
    private final Map<String, Long> resourceNodeRespawns = new ConcurrentHashMap<>();
    private final Map<String, Integer> penBlueprints = new ConcurrentHashMap<>();
    private final List<String> capturedSlimeIds = new ArrayList<>();
    private final List<PenSlime> penSlimes = new ArrayList<>();
    private long coins;
    private long lastPlortProductionMillis;
    private int penCapacity = 6;
    private int penCaseKeys;
    private String activePenStyleId = "pen_basic";
    private int vacpackPlortCapacity = 32;
    private int vacpackSlimeCapacity = 4;
    private int vacpackFoodCapacity = 64;
    private int vacpackSeedCapacity = 64;
    private int vacpackResourceCapacity = 64;
    private int vacpackOtherCapacity = 32;

    public PlayerProfile(UUID playerId, long coins) {
        this.playerId = playerId;
        this.coins = coins;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public PlayerStorage getStorage() {
        return this.storage;
    }

    public PlayerStorage getVacpackStorage() {
        return this.vacpackStorage;
    }

    public PlayerStorage getPenPlortStorage() {
        return this.penPlortStorage;
    }

    public FarmData getFarmData() {
        return this.farmData;
    }

    public ProgressData getProgressData() {
        return this.progressData;
    }

    public QuestData getQuestData() {
        return this.questData;
    }

    public Map<String, Long> getResourceNodeRespawns() {
        return this.resourceNodeRespawns;
    }

    public long getResourceNodeRespawn(String nodeKey) {
        Long respawnAt = this.resourceNodeRespawns.get(nodeKey);
        return respawnAt == null ? 0L : respawnAt;
    }

    public void setResourceNodeRespawn(String nodeKey, long respawnAtMillis) {
        if (nodeKey == null || nodeKey.trim().isEmpty()) {
            return;
        }
        if (respawnAtMillis <= 0L) {
            this.resourceNodeRespawns.remove(nodeKey);
            return;
        }
        this.resourceNodeRespawns.put(nodeKey, respawnAtMillis);
    }

    public void removeExpiredResourceNodeRespawns(long nowMillis) {
        this.resourceNodeRespawns.entrySet().removeIf(entry -> entry.getValue() <= nowMillis);
    }

    public long getCoins() {
        return this.coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public long getLastPlortProductionMillis() {
        return this.lastPlortProductionMillis;
    }

    public void setLastPlortProductionMillis(long lastPlortProductionMillis) {
        this.lastPlortProductionMillis = Math.max(0L, lastPlortProductionMillis);
    }

    public int getPenCapacity() {
        return this.penCapacity;
    }

    public void setPenCapacity(int penCapacity) {
        this.penCapacity = Math.max(1, penCapacity);
    }

    public int getVacpackCapacity() {
        return this.vacpackPlortCapacity
                + this.vacpackFoodCapacity
                + this.vacpackSeedCapacity
                + this.vacpackResourceCapacity
                + this.vacpackOtherCapacity;
    }

    public void setVacpackCapacity(int vacpackCapacity) {
        setVacpackPlortCapacity(vacpackCapacity);
    }

    public int getVacpackPlortCapacity() {
        return this.vacpackPlortCapacity;
    }

    public void setVacpackPlortCapacity(int vacpackPlortCapacity) {
        this.vacpackPlortCapacity = Math.max(1, vacpackPlortCapacity);
    }

    public int getVacpackSlimeCapacity() {
        return this.vacpackSlimeCapacity;
    }

    public void setVacpackSlimeCapacity(int vacpackSlimeCapacity) {
        this.vacpackSlimeCapacity = Math.max(1, vacpackSlimeCapacity);
    }

    public int getVacpackFoodCapacity() {
        return this.vacpackFoodCapacity;
    }

    public void setVacpackFoodCapacity(int vacpackFoodCapacity) {
        this.vacpackFoodCapacity = Math.max(1, vacpackFoodCapacity);
    }

    public int getVacpackSeedCapacity() {
        return this.vacpackSeedCapacity;
    }

    public void setVacpackSeedCapacity(int vacpackSeedCapacity) {
        this.vacpackSeedCapacity = Math.max(1, vacpackSeedCapacity);
    }

    public int getVacpackResourceCapacity() {
        return this.vacpackResourceCapacity;
    }

    public void setVacpackResourceCapacity(int vacpackResourceCapacity) {
        this.vacpackResourceCapacity = Math.max(1, vacpackResourceCapacity);
    }

    public int getVacpackOtherCapacity() {
        return this.vacpackOtherCapacity;
    }

    public void setVacpackOtherCapacity(int vacpackOtherCapacity) {
        this.vacpackOtherCapacity = Math.max(1, vacpackOtherCapacity);
    }

    public List<String> getCapturedSlimeIds() {
        return Collections.unmodifiableList(this.capturedSlimeIds);
    }

    public void setCapturedSlimeIds(List<String> slimeIds) {
        this.capturedSlimeIds.clear();
        if (slimeIds != null) {
            this.capturedSlimeIds.addAll(slimeIds);
        }
    }

    public void addCapturedSlime(String slimeId) {
        this.capturedSlimeIds.add(slimeId);
    }

    public boolean canCaptureSlime() {
        return this.capturedSlimeIds.size() < this.vacpackSlimeCapacity;
    }

    public int clearCapturedSlimes() {
        int removed = this.capturedSlimeIds.size();
        this.capturedSlimeIds.clear();
        return removed;
    }

    public boolean removeCapturedSlime(String slimeId) {
        return this.capturedSlimeIds.remove(slimeId);
    }

    public List<String> getPenSlimeIds() {
        List<String> slimeIds = new ArrayList<>();
        for (PenSlime slime : this.penSlimes) {
            slimeIds.add(slime.getSlimeId());
        }
        return Collections.unmodifiableList(slimeIds);
    }

    public List<PenSlime> getPenSlimes() {
        return Collections.unmodifiableList(this.penSlimes);
    }

    public void setPenSlimeIds(List<String> slimeIds) {
        this.penSlimes.clear();
        if (slimeIds != null) {
            for (String slimeId : slimeIds) {
                this.penSlimes.add(new PenSlime(slimeId));
            }
        }
    }

    public void setPenSlimes(List<PenSlime> slimes) {
        this.penSlimes.clear();
        if (slimes != null) {
            this.penSlimes.addAll(slimes);
        }
    }

    public int clearPenSlimes() {
        int removed = this.penSlimes.size();
        this.penSlimes.clear();
        return removed;
    }

    public boolean isPenFull() {
        return this.penSlimes.size() >= this.penCapacity;
    }

    public boolean isPenFull(int effectiveCapacity) {
        return this.penSlimes.size() >= Math.max(1, effectiveCapacity);
    }

    public boolean addPenSlime(String slimeId) {
        if (isPenFull()) {
            return false;
        }
        this.penSlimes.add(new PenSlime(slimeId));
        return true;
    }

    public boolean addPenSlime(String slimeId, int effectiveCapacity) {
        if (isPenFull(effectiveCapacity)) {
            return false;
        }
        this.penSlimes.add(new PenSlime(slimeId));
        return true;
    }

    public int getPenCaseKeys() {
        return this.penCaseKeys;
    }

    public void setPenCaseKeys(int penCaseKeys) {
        this.penCaseKeys = Math.max(0, penCaseKeys);
    }

    public void addPenCaseKeys(int amount) {
        setPenCaseKeys(this.penCaseKeys + Math.max(0, amount));
    }

    public boolean usePenCaseKey() {
        if (this.penCaseKeys <= 0) {
            return false;
        }
        this.penCaseKeys--;
        return true;
    }

    public Map<String, Integer> getPenBlueprints() {
        return Collections.unmodifiableMap(this.penBlueprints);
    }

    public void setPenBlueprints(Map<String, Integer> blueprints) {
        this.penBlueprints.clear();
        if (blueprints != null) {
            blueprints.forEach((id, amount) -> {
                if (id != null && amount != null && amount > 0) {
                    this.penBlueprints.put(id, amount);
                }
            });
        }
    }

    public int getPenBlueprintCount(String styleId) {
        return this.penBlueprints.getOrDefault(styleId, 0);
    }

    public void addPenBlueprint(String styleId) {
        this.penBlueprints.merge(styleId, 1, Integer::sum);
    }

    public String getActivePenStyleId() {
        return this.activePenStyleId;
    }

    public void setActivePenStyleId(String activePenStyleId) {
        this.activePenStyleId = activePenStyleId == null || activePenStyleId.trim().isEmpty()
                ? "pen_basic" : activePenStyleId;
    }

    public String removePenSlime(int index) {
        if (index < 0 || index >= this.penSlimes.size()) {
            return null;
        }
        return this.penSlimes.remove(index).getSlimeId();
    }
}
