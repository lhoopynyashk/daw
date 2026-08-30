package dev.lhoopy.crafting;

import dev.lhoopy.profile.PlayerProfile;

public final class SlimeLabService {
    private static final String STATION_ID = "slime_lab";
    private final CraftingService craftingService;

    public SlimeLabService(CraftingService craftingService) {
        this.craftingService = craftingService;
    }

    public boolean craft(PlayerProfile profile, String recipeId) {
        return craftDetailed(profile, recipeId).isSuccess();
    }

    public CraftingResult craftDetailed(PlayerProfile profile, String recipeId) {
        return this.craftingService.craft(profile, recipeId, STATION_ID);
    }

    public CraftingResult craftDetailed(PlayerProfile profile, String recipeId, int amount) {
        return this.craftingService.craft(profile, recipeId, STATION_ID, amount);
    }

    public String getStationId() {
        return STATION_ID;
    }
}
