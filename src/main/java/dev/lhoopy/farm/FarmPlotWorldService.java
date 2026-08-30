package dev.lhoopy.farm;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.PlantDef;
import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FarmPlotWorldService {
    private static final String CONFIG_PLOTS = "farm-plots.plots";

    private final SlimesPlugin plugin;
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final CropGrowthService cropGrowthService;
    private final FarmPlotMenuService menuService;
    private final List<PlotBlockArea> plotAreas = new ArrayList<>();
    private final Map<String, Map<String, String>> renderedStates = new HashMap<>();

    FarmPlotWorldService(
            SlimesPlugin plugin,
            ContentRegistry contentRegistry,
            ProfileService profileService,
            CropGrowthService cropGrowthService,
            FarmPlotMenuService menuService
    ) {
        this.plugin = plugin;
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.cropGrowthService = cropGrowthService;
        this.menuService = menuService;
    }

    void loadConfig() {
        this.plotAreas.clear();
        this.renderedStates.clear();
        if (this.plugin.getConfig().isConfigurationSection(CONFIG_PLOTS)) {
            for (String plotId : this.plugin.getConfig().getConfigurationSection(CONFIG_PLOTS).getKeys(false)) {
                String path = CONFIG_PLOTS + "." + plotId + ".";
                this.plotAreas.add(new PlotBlockArea(
                        normalize(plotId),
                        this.plugin.getConfig().getInt(path + "x"),
                        this.plugin.getConfig().getInt(path + "y"),
                        this.plugin.getConfig().getInt(path + "z"),
                        normalize(this.plugin.getConfig().getString(path + "type", "basic"))
                ));
            }
        }
        if (this.plotAreas.isEmpty()) {
            this.plotAreas.add(new PlotBlockArea("plot_1", -4, 90, -4, "basic"));
            this.plotAreas.add(new PlotBlockArea("plot_2", 0, 90, -4, "basic"));
            this.plotAreas.add(new PlotBlockArea("plot_3", 4, 90, -4, "basic"));
        }
    }

    void render(Player player, PlayerProfile profile, boolean force) {
        World world = player.getWorld();
        long now = System.currentTimeMillis();
        Map<String, String> worldStates = this.renderedStates.computeIfAbsent(world.getName(), ignored -> new HashMap<>());
        for (PlotBlockArea area : this.plotAreas) {
            FarmPlot plot = profile.getFarmData().getOrCreatePlot(area.plotId);
            if (plot.getPlotTypeId() == null || plot.getPlotTypeId().trim().isEmpty()) {
                plot.setPlotTypeId(area.defaultType);
            }
            renderPlot(player, world, area, plot, now, worldStates, force);
        }
    }

    void invalidate(World world) {
        if (world != null) {
            this.renderedStates.remove(world.getName());
        }
    }

    void clearRuntimeState() {
        this.renderedStates.clear();
    }

    boolean hasPlot(String plotId) {
        for (PlotBlockArea area : this.plotAreas) {
            if (area.plotId.equals(normalize(plotId))) {
                return true;
            }
        }
        return false;
    }

    void handleInteract(PlayerInteractEvent event, boolean canModify) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        PlotBlockArea area = findArea(event.getClickedBlock());
        if (area == null) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);
        if (!canModify) {
            player.sendMessage(ChatColor.RED + "Менять грядки может только владелец ранчо.");
            return;
        }
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher загружается.");
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            return;
        }

        profile.getFarmData().getOrCreatePlot(area.plotId);
        this.menuService.open(player, profile, area.plotId);
    }

    private void renderPlot(
            Player player,
            World world,
            PlotBlockArea area,
            FarmPlot plot,
            long now,
            Map<String, String> worldStates,
            boolean force
    ) {
        PlantDef plant = plot.isEmpty() ? null : this.contentRegistry.getPlant(plot.getPlantId());
        if (plant != null) {
            this.cropGrowthService.updateGrowth(plot, plant, now);
        }

        boolean watered = plot.getWateredUntilMillis() > now;
        int stage = plant == null ? -1 : growthStage(plot, plant);
        showStatusEffect(player, area, watered, stage);

        String state = plot.getPlotTypeId() + '|' + plot.getPlantId() + '|' + watered + '|' + stage;
        if (!force && state.equals(worldStates.get(area.plotId))) {
            return;
        }
        worldStates.put(area.plotId, state);

        Material ground = materialFor(plot.getPlotTypeId());
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                Block groundBlock = world.getBlockAt(area.x + dx, area.y, area.z + dz);
                setBlock(groundBlock, ground, hydrationData(ground, watered));

                Block cropBlock = world.getBlockAt(area.x + dx, area.y + 1, area.z + dz);
                if (plant == null) {
                    setBlock(cropBlock, Material.AIR, (byte) 0);
                } else {
                    renderPlantCell(cropBlock, plant, stage, dx, dz);
                }
            }
        }
    }

    private PlotBlockArea findArea(Block block) {
        for (PlotBlockArea area : this.plotAreas) {
            if (block.getY() != area.y && block.getY() != area.y + 1) {
                continue;
            }
            int x = block.getX();
            int z = block.getZ();
            if (x >= area.x && x <= area.x + 1 && z >= area.z && z <= area.z + 1) {
                return area;
            }
        }
        return null;
    }

    private int growthStage(FarmPlot plot, PlantDef plant) {
        return PlantVisuals.stage(this.cropGrowthService.growthPercent(plot, plant));
    }

    private static void renderPlantCell(Block block, PlantDef plant, int stage, int dx, int dz) {
        if (!PlantVisuals.occupiesCell(stage, dx, dz)) {
            setBlock(block, Material.AIR, (byte) 0);
            return;
        }
        Material material = PlantVisuals.material(plant);
        setBlock(block, material, PlantVisuals.data(material, stage));
    }

    private static void showStatusEffect(Player player, PlotBlockArea area, boolean watered, int stage) {
        Location center = new Location(player.getWorld(), area.x + 1.0D, area.y + 1.35D, area.z + 1.0D);
        if (stage == PlantVisuals.STAGE_COUNT - 1) {
            player.playEffect(center, Effect.HAPPY_VILLAGER, 0);
        } else if (watered) {
            player.playEffect(center, Effect.WATERDRIP, 0);
        }
    }

    private static void setBlock(Block block, Material material, byte data) {
        if (block.getType() != material) {
            block.setType(material, false);
        }
        if (material != Material.AIR && block.getData() != data) {
            block.setData(data, false);
        }
    }

    private static byte hydrationData(Material material, boolean watered) {
        return material == Material.SOIL && watered ? (byte) 7 : (byte) 0;
    }

    private static Material materialFor(String plotType) {
        if ("wet".equalsIgnoreCase(plotType)) {
            return Material.CLAY;
        }
        if ("mycelium".equalsIgnoreCase(plotType)) {
            return Material.MYCEL;
        }
        if ("hot".equalsIgnoreCase(plotType)) {
            return Material.NETHERRACK;
        }
        if ("crystal".equalsIgnoreCase(plotType)) {
            return Material.QUARTZ_BLOCK;
        }
        if ("sky".equalsIgnoreCase(plotType)) {
            return Material.ENDER_STONE;
        }
        return Material.SOIL;
    }

    private static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static final class PlotBlockArea {
        private final String plotId;
        private final int x;
        private final int y;
        private final int z;
        private final String defaultType;

        private PlotBlockArea(String plotId, int x, int y, int z, String defaultType) {
            this.plotId = plotId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.defaultType = defaultType;
        }
    }
}
