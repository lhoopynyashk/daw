package dev.lhoopy.crafting;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.FoodDef;
import dev.lhoopy.content.PlantDef;
import dev.lhoopy.content.PlortDef;
import dev.lhoopy.content.RecipeDef;
import dev.lhoopy.content.ResourceDef;
import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import gg.cristalix.wada.transfer.ModTransfer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class FarmerTableMenuService implements PluginService {
    public static final String OPEN_CHANNEL = "slimehunt:farmer";
    public static final String CRAFT_CHANNEL = "slimehunt:fcraft";

    private final SlimesPlugin plugin;
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final FarmerTableService farmerTableService;
    private final EnginexHuntBridge clientBridge;

    public FarmerTableMenuService(SlimesPlugin plugin, ContentRegistry contentRegistry, ProfileService profileService,
                                  FarmerTableService farmerTableService, EnginexHuntBridge clientBridge) {
        this.plugin = plugin;
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.farmerTableService = farmerTableService;
        this.clientBridge = clientBridge;
    }

    @Override
    public void enable() {
        ModTransfer.registerChannel(CRAFT_CHANNEL, (player, transfer) -> {
            String recipeId = transfer.readString();
            int amount = transfer.readInt();
            Bukkit.getScheduler().runTask(this.plugin, () -> craft(player, recipeId, amount));
        });
    }

    @Override
    public void shutdown() {
    }

    public void open(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can open the farmer table.");
            return;
        }
        open((Player) sender);
    }

    public void open(Player player) {
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль ещё загружается.");
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            return;
        }
        if (!this.clientBridge.isClientModLoaded(player)) {
            if (!this.clientBridge.sendClientModTo(player)) {
                player.sendMessage(ChatColor.RED + "Не удалось загрузить мод меню.");
                return;
            }
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline()) {
                    open(player);
                }
            }, 60L);
            return;
        }
        sendMenu(player, profile);
    }

    private void craft(Player player, String recipeId, int amount) {
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Профиль не загружен.");
            return;
        }
        CraftingResult result = this.farmerTableService.craftDetailed(profile, recipeId, amount);
        if (result.isSuccess()) {
            RecipeDef recipe = result.getRecipe();
            int produced = recipe.getResultAmount() * result.getCraftedAmount();
            this.profileService.saveLoaded(player.getUniqueId());
            player.playSound(player.getLocation(), "random.orb", 0.8F, 1.25F);
            player.sendMessage(ChatColor.GREEN + "Создано: " + ChatColor.WHITE
                    + resolveName(recipe.getResultId()) + " x" + produced);
        } else {
            player.playSound(player.getLocation(), "note.bass", 0.7F, 0.8F);
            player.sendMessage(ChatColor.RED + failureMessage(result));
        }
        open(player);
    }

    private void sendMenu(Player player, PlayerProfile profile) {
        ModTransfer transfer = new ModTransfer().writeString(Long.toString(profile.getCoins()));
        List<PlotSeedCategory> categories = this.farmerTableService.getCategories();
        transfer.writeInt(categories.size());
        for (PlotSeedCategory category : categories) {
            transfer.writeString(category.getId()).writeString(category.getTitle());
        }

        List<SeedRecipeEntry> recipes = this.farmerTableService.listSeedRecipes(profile);
        transfer.writeInt(recipes.size());
        for (SeedRecipeEntry entry : recipes) {
            RecipeDef recipe = entry.getRecipe();
            PlantDef plant = entry.getPlant();
            transfer.writeString(recipe.getId())
                    .writeString(entry.getCategory().getId())
                    .writeString(plant.getDisplayName())
                    .writeString(recipe.getResultId())
                    .writeInt(recipe.getResultAmount())
                    .writeInt(plant.getGrowthSeconds())
                    .writeInt(recipe.getMaxCraftsPerAction())
                    .writeString(Long.toString(recipe.getCoinCost()))
                    .writeInt(entry.getSeedsInStorage())
                    .writeInt(recipe.getIngredients().size());
            for (Map.Entry<String, Integer> ingredient : recipe.getIngredients().entrySet()) {
                transfer.writeString(ingredient.getKey())
                        .writeString(resolveName(ingredient.getKey()))
                        .writeInt(ingredient.getValue())
                        .writeInt(profile.getStorage().getAmount(ingredient.getKey()));
            }
        }
        transfer.send(OPEN_CHANNEL, player);
    }

    private String resolveName(String id) {
        ResourceDef resource = this.contentRegistry.getResource(id);
        if (resource != null) return resource.getDisplayName();
        FoodDef food = this.contentRegistry.getFood(id);
        if (food != null) return food.getDisplayName();
        PlortDef plort = this.contentRegistry.getPlort(id);
        if (plort != null) return plort.getDisplayName();
        PlantDef plant = this.contentRegistry.getPlantOrSeed(id);
        if (plant != null) return plant.getDisplayName();
        return id.replace('_', ' ');
    }

    private static String failureMessage(CraftingResult result) {
        switch (result.getFailureReason()) {
            case MISSING_INGREDIENTS:
                return "Не хватает ресурсов для крафта.";
            case MISSING_COINS:
                return "Не хватает монет.";
            case LOCKED:
                return "Рецепт ещё не открыт.";
            case AMOUNT_LIMIT_EXCEEDED:
                return "Выбрано слишком много крафтов.";
            default:
                return "Не удалось создать предмет.";
        }
    }
}
