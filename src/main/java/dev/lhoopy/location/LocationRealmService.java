package dev.lhoopy.location;

import dev.lhoopy.content.ContentRegistry;
import dev.lhoopy.content.SlimeDef;
import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.core.lifecycle.PluginService;
import dev.lhoopy.hunt.EnginexHuntBridge;
import dev.lhoopy.profile.PlayerProfile;
import dev.lhoopy.profile.ProfileService;
import dev.lhoopy.slime.SlimeService;
import dev.lhoopy.storage.VacpackLimits;
import gg.cristalix.wada.Wada;
import gg.cristalix.wada.color.Color;
import gg.cristalix.wada.color.palette.Palette;
import gg.cristalix.wada.common.menu.icon.ItemIcon;
import gg.cristalix.wada.common.menu.tooltip.Tooltip;
import gg.cristalix.wada.component.menu.choice.common.ChoiceButton;
import gg.cristalix.wada.component.menu.choice.common.ChoiceMenu;
import gg.cristalix.wada.component.structureoutline.data.StructureOutline;
import gg.cristalix.wada.component.worldtext.data.WorldText;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.network.ISocketClient;
import ru.cristalix.core.realm.IRealmService;
import ru.cristalix.core.realm.RealmId;
import ru.cristalix.core.realm.RealmInfo;
import ru.cristalix.core.transfer.ITransferService;
import ru.cristalix.core.transfer.TransferService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LocationRealmService implements PluginService, Listener {
    private static final String NPC_TAG = "slimes:location_npc";
    private static final String NODE_KEY_SEPARATOR = "__";

    private final SlimesPlugin plugin;
    private final ContentRegistry contentRegistry;
    private final ProfileService profileService;
    private final EnginexHuntBridge enginexHuntBridge;
    private final SlimeService slimeService;
    private final Map<UUID, Set<String>> hiddenNodes = new HashMap<>();
    private final Map<UUID, Set<String>> visibleNodes = new HashMap<>();
    private final Map<UUID, StructureOutline> hoverOutlines = new HashMap<>();
    private final Map<UUID, WorldText> hoverTexts = new HashMap<>();
    private final Set<UUID> transferringPlayers = new HashSet<>();
    private final Map<UUID, Long> nextSlimeRespawns = new HashMap<>();
    private LocationRealmConfig config;
    private LocationRealmConfig.RealmLocation currentLocation;
    private World locationWorld;
    private int displayTaskId = -1;
    private int slimeTaskId = -1;

    public LocationRealmService(
            SlimesPlugin plugin,
            ContentRegistry contentRegistry,
            ProfileService profileService,
            EnginexHuntBridge enginexHuntBridge,
            SlimeService slimeService
    ) {
        this.plugin = plugin;
        this.contentRegistry = contentRegistry;
        this.profileService = profileService;
        this.enginexHuntBridge = enginexHuntBridge;
        this.slimeService = slimeService;
    }

    @Override
    public void enable() {
        this.config = LocationRealmConfig.load(this.plugin, this.contentRegistry);
        this.currentLocation = resolveCurrentLocation();
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (this.currentLocation != null) {
                prepareLocationRealm();
            } else {
                ensureNpcsInLoadedRanches();
            }
        });
        this.displayTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, this::updateResourceDisplays, 10L, 2L).getTaskId();
        this.slimeTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, this::updateLocationSlimes, 20L, 20L).getTaskId();

        if (this.currentLocation == null) {
            this.plugin.getLogger().info("Location selector enabled for " + this.config.getLocations().size() + " THUT realms");
        } else {
            this.plugin.getLogger().info("Current hunting location: " + this.currentLocation.getContent().getDisplayName()
                    + " (" + this.config.getRealmType() + "-" + this.currentLocation.getRealmId() + ")");
        }
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        if (this.displayTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.displayTaskId);
            this.displayTaskId = -1;
        }
        if (this.slimeTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.slimeTaskId);
            this.slimeTaskId = -1;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearHoverOutline(player);
            this.slimeService.clearLocationSlimes(player);
        }
        this.hiddenNodes.clear();
        this.visibleNodes.clear();
        this.hoverOutlines.clear();
        this.hoverTexts.clear();
        this.transferringPlayers.clear();
        this.nextSlimeRespawns.clear();
        this.locationWorld = null;
    }

    public void openMenu(Player player, String[] ignored) {
        openMenu((Player) player);
    }

    public boolean returnToRanchIfInLocation(CommandSender sender) {
        if (this.currentLocation == null || !(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
        String type = this.plugin.getConfig().getString("ranch-home.realm-type", "TEST");
        int id = this.plugin.getConfig().getInt("ranch-home.realm-id", 89);
        transfer(player, RealmId.of(type, id), "ranch");
        return true;
    }

    public boolean isCurrentLocationRealm() {
        return this.currentLocation != null;
    }

    public String currentLocationId() {
        return this.currentLocation == null ? null : this.currentLocation.getContent().getId();
    }

    public String currentLocationDisplayName() {
        return this.currentLocation == null ? null : this.currentLocation.getContent().getDisplayName();
    }

    public int currentRealmMaxPlayers() {
        return this.config == null ? 25 : this.config.getMaxPlayers();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.hiddenNodes.remove(player.getUniqueId());
        this.visibleNodes.remove(player.getUniqueId());
        if (this.currentLocation != null) {
            this.enginexHuntBridge.beginOrUpdateLoading(
                    player,
                    "Загрузка локации",
                    55
            );
            this.profileService.load(player).whenComplete((profile, error) ->
                    Bukkit.getScheduler().runTask(this.plugin, () -> finishLocationJoin(player, error))
            );
            return;
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> ensureNpc(player.getWorld()), 40L);
    }

    private void finishLocationJoin(Player player, Throwable error) {
        if (!player.isOnline()) {
            return;
        }
        if (error != null) {
            this.enginexHuntBridge.closeLoading(player);
            this.plugin.getLogger().warning("Could not load location profile for "
                    + player.getName() + ": " + error.getMessage());
            player.sendMessage(ChatColor.RED + "Не удалось загрузить профиль локации. Перезайди через минуту.");
            return;
        }

        World targetWorld = this.locationWorld == null ? player.getWorld() : this.locationWorld;
        player.teleport(spawn(targetWorld));
        if (this.slimeService.ensureVacuum(player)) {
            player.sendMessage(ChatColor.GREEN + "Вакпак выдан для охоты.");
        }
        syncPlayerResourceVisibility(player, System.currentTimeMillis());
        populateLocationSlimes(player, true);
        Bukkit.getScheduler().runTaskLater(this.plugin,
                () -> syncPlayerResourceVisibility(player, System.currentTimeMillis()), 10L);
        this.enginexHuntBridge.sendLoadingStatus(player, "Готово", 100);
        this.enginexHuntBridge.closeLoading(player);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (this.currentLocation == null) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> ensureNpc(event.getPlayer().getWorld()), 10L);
            return;
        }
        clearHoverOutline(event.getPlayer());
        this.nextSlimeRespawns.remove(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTaskLater(this.plugin,
                () -> {
                    syncPlayerResourceVisibility(event.getPlayer(), System.currentTimeMillis());
                    populateLocationSlimes(event.getPlayer(), true);
                }, 10L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearHoverOutline(event.getPlayer());
        this.hiddenNodes.remove(event.getPlayer().getUniqueId());
        this.visibleNodes.remove(event.getPlayer().getUniqueId());
        this.transferringPlayers.remove(event.getPlayer().getUniqueId());
        this.nextSlimeRespawns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getScoreboardTags().contains(NPC_TAG)) {
            return;
        }
        event.setCancelled(true);
        openMenu(event.getPlayer());
    }

    @EventHandler
    public void onResourceInteract(PlayerInteractEvent event) {
        if (this.currentLocation == null
                || (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR)
                || event.getPlayer().getWorld() != this.locationWorld) {
            return;
        }
        LocationRealmConfig.ResourceNode node = event.getClickedBlock() == null ? null : findNode(event.getClickedBlock());
        if (node == null) {
            node = findNodeAlongView(event.getPlayer(), 7.0D);
        }
        if (node == null) {
            return;
        }
        event.setCancelled(true);
        collect(event.getPlayer(), node);
    }

    @EventHandler
    public void onResourceBreak(BlockBreakEvent event) {
        if (this.currentLocation == null || event.getBlock().getWorld() != this.locationWorld || findNode(event.getBlock()) == null) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.GRAY + "Этот ресурс собирается правой кнопкой мыши.");
    }

    private void openMenu(Player player) {
        if (!this.config.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Переходы между локациями временно отключены.");
            return;
        }
        List<ChoiceButton> buttons = new ArrayList<>();
        int index = 0;
        for (LocationRealmConfig.RealmLocation location : this.config.getLocations()) {
            buttons.add(locationButton(location, index++));
        }
        ChoiceMenu menu = ChoiceMenu.builder()
                .title("§bВыбор локации")
                .description("§7Каждая зона находится на отдельном реалме. Выбери место для охоты и сбора ресурсов.")
                .buttons(buttons)
                .build();
        Wada.get().getMenuManager().open(menu, player);
    }

    private ChoiceButton locationButton(LocationRealmConfig.RealmLocation location, int index) {
        Color color = locationColor(index);
        String resourceCount = location.getNodes().size() + " видов ресурсов";
        return ChoiceButton.builder()
                .title("§f" + location.getContent().getDisplayName())
                .description("§7Уровень: §f" + location.getContent().getTier() + "\n§7" + resourceCount)
                .overlayLabel("ОТПРАВИТЬСЯ")
                .backgroundColor(color)
                .icon(ItemIcon.builder().itemStack(new ItemStack(location.getIcon())).scale(1.15F).build())
                .tooltip(Tooltip.builder()
                        .title(location.getContent().getDisplayName())
                        .description("Реалм " + this.config.getRealmType() + "-" + location.getRealmId()
                                + "\nОбычных слаймов: " + location.getContent().getNormalSlimeIds().size()
                                + "\nРесурсов: " + location.getNodes().size())
                        .cornerColor(color)
                        .build())
                .onPlayerLeftClick((player, button) -> transfer(
                        player,
                        RealmId.of(this.config.getRealmType(), location.getRealmId()),
                        location.getContent().getId()
                ))
                .build();
    }

    private void transfer(Player player, RealmId target, String destination) {
        ITransferService service = ensureTransferService();
        if (service == null) {
            player.sendMessage(ChatColor.RED + "Сервис переноса в реалм недоступен.");
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!this.transferringPlayers.add(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "Перенос уже выполняется.");
            return;
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put("slimes_destination", destination);
        metadata.put("target_realm", target.toString());
        metadata.put("player_name", player.getName());

        player.sendMessage(ChatColor.YELLOW + "Отправляю в " + ChatColor.WHITE + target + ChatColor.YELLOW + "...");
        this.profileService.releaseLoaded(playerId).whenComplete((ignored, error) ->
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (error != null) {
                        this.transferringPlayers.remove(playerId);
                        this.plugin.getLogger().warning("Could not save profile before transfer: " + error.getMessage());
                        player.sendMessage(ChatColor.RED + "Не удалось сохранить профиль перед переходом.");
                        return;
                    }
                    try {
                        Wada.get().getMenuManager().close(player);
                        service.transfer(playerId, target, metadata);
                        scheduleTransferRecovery(player);
                    } catch (RuntimeException transferError) {
                        this.transferringPlayers.remove(playerId);
                        this.plugin.getLogger().warning("Could not transfer " + player.getName()
                                + " to " + target + ": " + transferError.getMessage());
                        player.sendMessage(ChatColor.RED + "Перенос не удался. Профиль будет загружен снова.");
                        this.profileService.load(player);
                    }
                })
        );
    }

    private void scheduleTransferRecovery(Player player) {
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            this.transferringPlayers.remove(playerId);
            if (!this.profileService.isLoaded(playerId)) {
                this.plugin.getLogger().warning("Transfer did not disconnect " + player.getName()
                        + "; loading the released profile again.");
                this.profileService.load(player);
                player.sendMessage(ChatColor.RED + "Перенос не завершился. Повтори позже.");
            }
        }, 100L);
    }

    private void collect(Player player, LocationRealmConfig.ResourceNode node) {
        if (this.transferringPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Дождись завершения переноса.");
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            this.profileService.load(player);
            player.sendMessage(ChatColor.YELLOW + "Подожди, профиль ещё загружается.");
            return;
        }
        long now = System.currentTimeMillis();
        String nodeKey = nodeKey(node);
        long respawnAt = profile.getResourceNodeRespawn(nodeKey);
        if (respawnAt > now) {
            long seconds = Math.max(1L, (respawnAt - now + 999L) / 1000L);
            player.sendMessage(ChatColor.GRAY + "Ресурс восстановится через " + seconds + " сек.");
            hideNode(player, node);
            return;
        }

        int accepted = VacpackLimits.add(profile, node.getResource().getId(), node.getAmount());
        if (accepted <= 0) {
            player.sendMessage(ChatColor.RED + "В разделе ресурсов вакпака нет свободного места.");
            return;
        }

        long nextRespawn = now + node.getRespawnSeconds() * 1000L;
        profile.setResourceNodeRespawn(nodeKey, nextRespawn);
        restoreResourceBlock(node);
        hideNode(player, node);
        player.playSound(player.getLocation(), "random.orb", 0.7F, 1.35F);
        this.profileService.saveLoaded(player.getUniqueId()).whenComplete((ignored, error) ->
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (error != null) {
                        this.plugin.getLogger().warning("Could not persist collected resource for "
                                + player.getName() + ": " + error.getMessage());
                        player.sendMessage(ChatColor.RED + "БД не ответила. Ресурс остался в профиле "
                                + "и будет сохранён повторно.");
                        return;
                    }
                    player.sendMessage(ChatColor.GREEN + "+" + accepted + " "
                            + node.getResource().getDisplayName()
                            + ChatColor.GRAY + " (в вакпак)");
                })
        );
    }

    private void prepareLocationRealm() {
        if (Bukkit.getWorlds().isEmpty()) {
            throw new IllegalStateException("No world is loaded for location realm");
        }
        this.locationWorld = Bukkit.getWorlds().get(0);
        if (!(this.locationWorld.getGenerator() instanceof dev.lhoopy.world.VoidChunkGenerator)) {
            this.plugin.getLogger().severe("THUT world " + this.locationWorld.getName()
                    + " is not using Slimes void generator. Configure generator: Slimes in bukkit.yml.");
        }
        tuneWorld(this.locationWorld);
        if (this.config.shouldBuildPlatform()) {
            buildPlatform(this.locationWorld);
        }
        placeResourceBlocks(this.locationWorld);
        this.locationWorld.setSpawnLocation(0, this.config.getPlatformY() + 1, 0);
        this.plugin.getLogger().info("Location world ready: " + this.locationWorld.getName() + " (void generator)");
    }

    private void tuneWorld(World world) {
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");
        world.setGameRuleValue("randomTickSpeed", "0");
        world.setTime(6000L);
        world.setStorm(false);
    }

    private void buildPlatform(World world) {
        int y = this.config.getPlatformY();
        int radius = this.config.getPlatformRadius();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                world.getBlockAt(x, y - 1, z).setType(Material.GRASS);
                world.getBlockAt(x, y, z).setType(Material.AIR);
                world.getBlockAt(x, y + 1, z).setType(Material.AIR);
                if (Math.abs(x) == radius || Math.abs(z) == radius) {
                    world.getBlockAt(x, y, z).setType(Material.FENCE);
                }
            }
        }
        world.getBlockAt(0, y, 0).setType(Material.TORCH);
    }

    @SuppressWarnings("deprecation")
    private void placeResourceBlocks(World world) {
        for (LocationRealmConfig.ResourceNode node : this.currentLocation.getNodes()) {
            Block block = world.getBlockAt(node.getX(), node.getY(), node.getZ());
            block.setType(node.getMaterial());
            block.setData(node.getData());
        }
    }

    private void updateLocationSlimes() {
        if (this.currentLocation == null || !this.config.getSlimes().isEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == this.locationWorld && this.profileService.getLoaded(player.getUniqueId()) != null) {
                populateLocationSlimes(player, false);
            }
        }
    }

    private void populateLocationSlimes(Player player, boolean immediately) {
        if (this.currentLocation == null || !this.config.getSlimes().isEnabled()
                || !player.isOnline() || player.getWorld() != this.locationWorld) {
            return;
        }

        List<LocationRealmConfig.SlimeSpawnPoint> points = this.config.getSlimes().getSpawnPoints();
        List<String> slimeIds = this.currentLocation.getContent().getNormalSlimeIds();
        int targetCount = Math.min(points.size(), slimeIds.size());
        int currentCount = this.slimeService.countLocationSlimes(player);
        if (currentCount >= targetCount) {
            this.nextSlimeRespawns.remove(player.getUniqueId());
            return;
        }

        long now = System.currentTimeMillis();
        Long respawnAt = this.nextSlimeRespawns.get(player.getUniqueId());
        if (!immediately && respawnAt == null) {
            this.nextSlimeRespawns.put(player.getUniqueId(),
                    now + this.config.getSlimes().getRespawnSeconds() * 1000L);
            return;
        }
        if (!immediately && respawnAt > now) {
            return;
        }

        for (int index = currentCount; index < targetCount; index++) {
            SlimeDef definition = this.contentRegistry.getSlime(slimeIds.get(index % slimeIds.size()));
            if (definition == null) {
                continue;
            }
            LocationRealmConfig.SlimeSpawnPoint point = points.get(index);
            Location location = new Location(this.locationWorld, point.getX(), point.getY(), point.getZ());
            this.slimeService.spawnLocationSlime(player, definition, location);
        }
        this.nextSlimeRespawns.remove(player.getUniqueId());
    }

    private void updateResourceDisplays() {
        if (this.currentLocation == null) {
            ensureNpcsInLoadedRanches();
            return;
        }
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != this.locationWorld) {
                clearHoverOutline(player);
                continue;
            }
            PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
            if (profile == null) {
                clearHoverOutline(player);
                continue;
            }
            profile.removeExpiredResourceNodeRespawns(now);
            syncPlayerResourceVisibility(player, now);
            updateHoverOutline(player, profile, now);
        }
    }

    private void syncPlayerResourceVisibility(Player player, long now) {
        if (!player.isOnline() || player.getWorld() != this.locationWorld) {
            return;
        }
        PlayerProfile profile = this.profileService.getLoaded(player.getUniqueId());
        if (profile == null) {
            return;
        }
        profile.removeExpiredResourceNodeRespawns(now);
        for (LocationRealmConfig.ResourceNode node : this.currentLocation.getNodes()) {
            boolean available = profile.getResourceNodeRespawn(nodeKey(node)) <= now;
            syncNodeVisibility(player, node, available);
        }
    }

    @SuppressWarnings("deprecation")
    private void syncNodeVisibility(Player player, LocationRealmConfig.ResourceNode node, boolean available) {
        Set<String> hidden = this.hiddenNodes.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
        Set<String> visible = this.visibleNodes.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
        String key = nodeKey(node);
        Location location = nodeLocation(player.getWorld(), node);
        if (available) {
            boolean wasHidden = hidden.remove(key);
            boolean newlyVisible = visible.add(key);
            if (wasHidden || newlyVisible) {
                restoreResourceBlock(node);
                player.sendBlockChange(location, node.getMaterial(), node.getData());
            }
        } else {
            hidden.add(key);
            visible.remove(key);
            player.sendBlockChange(location, Material.AIR, (byte) 0);
        }
    }

    @SuppressWarnings("deprecation")
    private void hideNode(Player player, LocationRealmConfig.ResourceNode node) {
        this.hiddenNodes.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).add(nodeKey(node));
        this.visibleNodes.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).remove(nodeKey(node));
        player.sendBlockChange(nodeLocation(player.getWorld(), node), Material.AIR, (byte) 0);
        clearHoverOutline(player);
    }

    @SuppressWarnings("deprecation")
    private void updateHoverOutline(Player player, PlayerProfile profile, long now) {
        Block target = player.getTargetBlock((Set<Material>) null, 7);
        LocationRealmConfig.ResourceNode node = target == null ? null : findNode(target);
        if (node == null || profile.getResourceNodeRespawn(nodeKey(node)) > now) {
            clearHoverOutline(player);
            return;
        }

        StructureOutline current = this.hoverOutlines.get(player.getUniqueId());
        String key = hoverKey(node);
        if (current != null && key.equals(current.getKey())) {
            return;
        }
        clearHoverOutline(player);

        StructureOutline outline = StructureOutline.builder()
                .key(key)
                .locations(java.util.Collections.singletonList(nodeLocation(this.locationWorld, node)))
                .color(outlineColor(node.getResource().getRarity()))
                .xray(false)
                .build();
        Wada.get().getStructureOutlineManager().show(outline, player);
        this.hoverOutlines.put(player.getUniqueId(), outline);

        WorldText text = WorldText.builder()
                .key("slimes_resource_hint_" + player.getUniqueId())
                .location(nodeLocation(this.locationWorld, node).add(0.5D, 1.45D, 0.5D))
                .text("§fНажми §eПКМ")
                .scale(1.2D)
                .backgroundColor(Palette.BLACK_62)
                .lookAtPlayer(true, true)
                .renderDistance(8.0D)
                .shadow(true)
                .hitboxVisible(false)
                .whoCanView(player)
                .build();
        Wada.get().getWorldTextManager().add(text);
        this.hoverTexts.put(player.getUniqueId(), text);
    }

    private void clearHoverOutline(Player player) {
        StructureOutline outline = this.hoverOutlines.remove(player.getUniqueId());
        if (outline != null) {
            Wada.get().getStructureOutlineManager().remove(outline, player);
        }
        WorldText text = this.hoverTexts.remove(player.getUniqueId());
        if (text != null) {
            Wada.get().getWorldTextManager().remove(text);
        }
    }

    private Color outlineColor(String rarity) {
        if ("epic".equalsIgnoreCase(rarity)) {
            return Palette.PURPLE_LIGHT;
        }
        if ("rare".equalsIgnoreCase(rarity)) {
            return Palette.CYAN_LIGHT;
        }
        return Palette.GREEN_LIGHT;
    }

    private String hoverKey(LocationRealmConfig.ResourceNode node) {
        return "slimes_resource_" + nodeKey(node);
    }

    private LocationRealmConfig.ResourceNode findNode(Block block) {
        for (LocationRealmConfig.ResourceNode node : this.currentLocation.getNodes()) {
            if (node.getX() == block.getX() && node.getY() == block.getY() && node.getZ() == block.getZ()) {
                return node;
            }
        }
        return null;
    }

    private LocationRealmConfig.ResourceNode findNodeAlongView(Player player, double maxDistance) {
        Vector origin = player.getEyeLocation().toVector();
        Vector direction = player.getEyeLocation().getDirection().normalize();
        LocationRealmConfig.ResourceNode nearest = null;
        double nearestDistance = maxDistance + 1.0D;
        for (LocationRealmConfig.ResourceNode node : this.currentLocation.getNodes()) {
            Vector center = new Vector(node.getX() + 0.5D, node.getY() + 0.5D, node.getZ() + 0.5D);
            Vector offset = center.clone().subtract(origin);
            double distanceAlongRay = offset.dot(direction);
            if (distanceAlongRay < 0.0D || distanceAlongRay > maxDistance) {
                continue;
            }
            Vector closestPoint = origin.clone().add(direction.clone().multiply(distanceAlongRay));
            if (closestPoint.distanceSquared(center) > 0.75D * 0.75D || distanceAlongRay >= nearestDistance) {
                continue;
            }
            nearest = node;
            nearestDistance = distanceAlongRay;
        }
        return nearest;
    }

    @SuppressWarnings("deprecation")
    private void restoreResourceBlock(LocationRealmConfig.ResourceNode node) {
        if (this.locationWorld == null) {
            return;
        }
        Block block = this.locationWorld.getBlockAt(node.getX(), node.getY(), node.getZ());
        if (block.getType() != node.getMaterial()) {
            block.setType(node.getMaterial(), false);
        }
        if (block.getData() != node.getData()) {
            block.setData(node.getData(), false);
        }
    }

    private void ensureNpcsInLoadedRanches() {
        if (!this.config.getNpc().isEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            ensureNpc(player.getWorld());
        }
    }

    private void ensureNpc(World world) {
        if (world == null || this.currentLocation != null || !this.config.getNpc().isEnabled()) {
            return;
        }
        LocationRealmConfig.NpcSettings settings = this.config.getNpc();
        Location location = new Location(world, settings.getX(), settings.getY(), settings.getZ(), settings.getYaw(), 0.0F);
        Villager kept = null;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Villager) || !entity.getScoreboardTags().contains(NPC_TAG)) {
                continue;
            }
            if (kept == null) {
                kept = (Villager) entity;
            } else {
                entity.remove();
            }
        }
        if (kept == null) {
            kept = (Villager) world.spawnEntity(location, EntityType.VILLAGER);
            kept.addScoreboardTag(NPC_TAG);
        } else {
            kept.teleport(location);
        }
        kept.setCustomName(settings.getName());
        kept.setCustomNameVisible(true);
        kept.setAI(false);
        kept.setInvulnerable(true);
        kept.setCollidable(false);
        kept.setProfession(Villager.Profession.LIBRARIAN);
    }

    private LocationRealmConfig.RealmLocation resolveCurrentLocation() {
        RealmId realm = currentRealmId();
        if (realm == null || !realm.getTypeName().equalsIgnoreCase(this.config.getRealmType())) {
            return null;
        }
        return this.config.getByRealmId(realm.getId());
    }

    private RealmId currentRealmId() {
        try {
            IRealmService service = IRealmService.get();
            RealmInfo info = service == null ? null : service.getCurrentRealmInfo();
            return info == null ? null : info.getRealmId();
        } catch (NoClassDefFoundError coreApiMissing) {
            return null;
        } catch (RuntimeException error) {
            this.plugin.getLogger().warning(
                    "Cristalix Core call failed: " + error);
            return null;
        }
    }

    private ITransferService ensureTransferService() {
        ITransferService service = ITransferService.get();
        if (service != null) {
            return service;
        }
        ISocketClient socketClient = ISocketClient.get();
        if (socketClient == null) {
            return null;
        }
        try {
            service = new TransferService(socketClient);
            CoreApi.get().registerService(ITransferService.class, service);
            return service;
        } catch (NoClassDefFoundError | RuntimeException error) {
            this.plugin.getLogger().warning("Could not initialize location TransferService: " + error.getMessage());
            return ITransferService.get();
        }
    }

    private Location spawn(World world) {
        return new Location(world, 0.5D, this.config.getPlatformY() + 1.0D, 0.5D, 180.0F, 0.0F);
    }

    private Location nodeLocation(World world, LocationRealmConfig.ResourceNode node) {
        return new Location(world, node.getX(), node.getY(), node.getZ());
    }

    private String nodeKey(LocationRealmConfig.ResourceNode node) {
        return this.currentLocation.getContent().getId() + NODE_KEY_SEPARATOR + node.getId();
    }

    private Color locationColor(int index) {
        switch (index) {
            case 0: return Palette.CYAN_DARK_62;
            case 1: return Palette.BLUE_DARK_62;
            case 2: return Palette.GREEN_DARK_62;
            case 3: return Palette.RED_DARK_62;
            case 4: return Palette.PURPLE_DARK_62;
            default: return Palette.YELLOW_DARK_62;
        }
    }
}
