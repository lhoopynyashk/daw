package dev.lhoopy.hunt;

import dev.lhoopy.core.SlimesPlugin;
import dev.lhoopy.slime.SlimeCaptureTarget;
import dev.lhoopy.slime.SlimeService;
import gg.cristalix.wada.Wada;
import gg.cristalix.wada.transfer.ModTransfer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class EnginexHuntBridge implements Listener {
    static final String MOD_NAME = "SlimeHunt";
    static final String START_CHANNEL = "slimehunt:start";
    static final String RESULT_CHANNEL = "slimehunt:result";
    static final String LOADED_CHANNEL = "slimehunt:loaded";
    static final String LOADING_OPEN_CHANNEL = "slimehunt:loading_open";
    static final String LOADING_STATUS_CHANNEL = "slimehunt:loading_status";
    static final String LOADING_CLOSE_CHANNEL = "slimehunt:loading_close";
    public static final String MUSIC_CHANNEL = "slimehunt:music";
    public static final String MUSIC_STOP_CHANNEL = "slimehunt:music_stop";

    private static final int TOTAL_ROUNDS = 9;
    private static final int REQUIRED_HITS = 6;
    private static final int ROUND_TIMEOUT_MS = 1200;
    /** Фон экрана загрузки по умолчанию. Переопределяется ключом loading.art-url в config.yml. */
    private static final String DEFAULT_LOADING_ART_URL =
            "https://raw.githubusercontent.com/makaroon1977-code/dsad/c03395326dc6f80ffaaf6cceb29598daa8f9688e/SlimeRancher-Keyart_Wishlist-Landscape-2560x1440-b2f7b22e6d19dff415daf5dbe4d8e78c%20(1).jpg";
    private static final long LOAD_RETRY_TICKS = 40L;
    private static final long LOAD_TIMEOUT_TICKS = 200L;
    private static final long GAME_TIMEOUT_TICKS = 400L;
    private static final long LOADING_CLOSE_AFTER_LATE_OPEN_TICKS = 10L;
    private static final long[] LOADING_PACKET_RETRY_TICKS = {5L, 20L, 60L};
    private static final long[] MOD_SEND_RETRY_TICKS = {40L, 100L};

    private final SlimesPlugin plugin;
    private final Set<UUID> loadedPlayers = new HashSet<>();
    private final Set<UUID> sentPlayers = new HashSet<>();
    private final Set<UUID> openedLoadingPlayers = new HashSet<>();
    private final Set<UUID> completedLoadingPlayers = new HashSet<>();
    private final Set<UUID> pendingLoadingCloses = new HashSet<>();
    private final Map<UUID, LoadingStatus> pendingLoadingStatuses = new HashMap<>();
    private final Map<UUID, StartRequest> pendingStarts = new HashMap<>();
    private final Map<UUID, SlimeCaptureTarget> activeTargets = new HashMap<>();
    private SlimeService slimeWorldService;
    private boolean modLoaded;

    public EnginexHuntBridge(SlimesPlugin plugin) {
        this.plugin = plugin;
    }

    public void setSlimeService(SlimeService slimeWorldService) {
        this.slimeWorldService = slimeWorldService;
    }

    public void enable() {
        loadClientMod();
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        ModTransfer.registerChannel(LOADED_CHANNEL, (player, transfer) -> Bukkit.getScheduler().runTask(plugin, () -> {
            UUID playerId = player.getUniqueId();
            String clientVersion = readClientVersion(transfer);
            boolean firstLoad = loadedPlayers.add(playerId);
            plugin.getLogger().info("SlimeHunt client loaded for " + player.getName()
                    + " version=" + clientVersion
                    + " firstLoad=" + firstLoad
                    + " completedLoading=" + completedLoadingPlayers.contains(playerId));
            if (completedLoadingPlayers.contains(playerId)) {
                pendingLoadingCloses.remove(playerId);
                pendingLoadingStatuses.remove(playerId);
                closeLoading(player);
                return;
            }
            if (!firstLoad) {
                return;
            }
            if (pendingLoadingCloses.remove(playerId)) {
                closeLoading(player);
            } else {
                openPendingLoading(player);
            }
            StartRequest request = pendingStarts.remove(player.getUniqueId());
            if (request != null) {
                sendStart(player, request);
            }
        }));
        ModTransfer.registerChannel(RESULT_CHANNEL, (player, transfer) -> {
            int hits = transfer.readInt();
            int total = transfer.readInt();
            boolean success = transfer.readBoolean();
            Bukkit.getScheduler().runTask(plugin, () -> finish(player, hits, total, success));
        });
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        loadedPlayers.clear();
        sentPlayers.clear();
        openedLoadingPlayers.clear();
        completedLoadingPlayers.clear();
        pendingLoadingCloses.clear();
        pendingLoadingStatuses.clear();
        pendingStarts.clear();
        activeTargets.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        loadedPlayers.remove(playerId);
        sentPlayers.remove(playerId);
        openedLoadingPlayers.remove(playerId);
        completedLoadingPlayers.remove(playerId);
        pendingLoadingCloses.remove(playerId);
        pendingLoadingStatuses.remove(playerId);
        pendingStarts.remove(playerId);
        activeTargets.remove(playerId);
    }

    public boolean isClientModLoaded(Player player) {
        return loadedPlayers.contains(player.getUniqueId());
    }

    public boolean sendClientModTo(Player player) {
        UUID playerId = player.getUniqueId();
        if (loadedPlayers.contains(playerId)) {
            return true;
        }
        if (sentPlayers.contains(playerId)) {
            boolean resent = sendClientMod(player);
            if (resent) {
                scheduleClientModRetries(playerId);
            }
            return resent;
        }
        if (!modLoaded && !loadClientMod()) {
            return false;
        }
        sentPlayers.add(playerId);
        plugin.getLogger().info("Sending " + MOD_NAME + " client mod to " + player.getName());
        boolean sent = sendClientMod(player);
        if (!sent) {
            sentPlayers.remove(playerId);
        } else {
            scheduleClientModRetries(playerId);
        }
        return sent;
    }

    public boolean showLoading(Player player, String message, int progressPercent) {
        completedLoadingPlayers.remove(player.getUniqueId());
        if (!sendClientModTo(player)) {
            return false;
        }
        plugin.getLogger().fine("Opening loading overlay for " + player.getName()
                + " message='" + message + "' progress=" + clampProgress(progressPercent));
        if (loadedPlayers.contains(player.getUniqueId())) {
            sendLoadingOpen(player, message, progressPercent);
        } else {
            pendingLoadingStatuses.put(player.getUniqueId(), new LoadingStatus(message, clampProgress(progressPercent)));
        }
        return true;
    }

    public boolean beginOrUpdateLoading(Player player, String message, int progressPercent) {
        if (!loadedPlayers.contains(player.getUniqueId()) && !sentPlayers.contains(player.getUniqueId())) {
            return showLoading(player, message, progressPercent);
        }
        if (loadedPlayers.contains(player.getUniqueId()) && !openedLoadingPlayers.contains(player.getUniqueId())) {
            completedLoadingPlayers.remove(player.getUniqueId());
            sendLoadingOpen(player, message, progressPercent);
            return true;
        }
        sendLoadingStatus(player, message, progressPercent);
        return true;
    }

    public void sendLoadingStatus(Player player, String message, int progressPercent) {
        if (completedLoadingPlayers.contains(player.getUniqueId())) {
            return;
        }
        if (!loadedPlayers.contains(player.getUniqueId())) {
            pendingLoadingStatuses.put(player.getUniqueId(), new LoadingStatus(message, clampProgress(progressPercent)));
            return;
        }
        new ModTransfer()
                .writeString(message)
                .writeInt(clampProgress(progressPercent))
                .send(LOADING_STATUS_CHANNEL, player);
    }

    private void sendLoadingStatusNow(Player player, String message, int progressPercent) {
        if (!loadedPlayers.contains(player.getUniqueId())) {
            pendingLoadingStatuses.put(player.getUniqueId(), new LoadingStatus(message, clampProgress(progressPercent)));
            return;
        }
        new ModTransfer()
                .writeString(message)
                .writeInt(clampProgress(progressPercent))
                .send(LOADING_STATUS_CHANNEL, player);
    }

    public void closeLoading(Player player) {
        completedLoadingPlayers.add(player.getUniqueId());
        pendingLoadingStatuses.remove(player.getUniqueId());
        plugin.getLogger().fine("Closing loading overlay for " + player.getName());
        if (!loadedPlayers.contains(player.getUniqueId())) {
            pendingLoadingCloses.add(player.getUniqueId());
            sendLoadingClose(player);
            scheduleLoadingCloseRetries(player.getUniqueId());
            return;
        }
        if (!openedLoadingPlayers.contains(player.getUniqueId())) {
            sendLoadingOpen(player, "Готово", 100);
            scheduleLoadingCloseAfterLateOpen(player.getUniqueId());
            return;
        } else {
            sendLoadingStatusNow(player, "Готово", 100);
        }
        sendLoadingClose(player);
        scheduleLoadingCloseRetries(player.getUniqueId());
    }

    public void startCapture(Player player, SlimeCaptureTarget target) {
        if (activeTargets.containsKey(player.getUniqueId()) || pendingStarts.containsKey(player.getUniqueId())) {
            player.sendMessage("\u00a7eОхота уже запущена.");
            return;
        }
        activeTargets.put(player.getUniqueId(), target);
        start(player, false);
    }

    public void start(CommandSender sender) {
        start(sender, true);
    }

    private void start(CommandSender sender, boolean manualCommand) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return;
        }

        Player player = (Player) sender;
        if (!activeTargets.containsKey(player.getUniqueId())) {
            if (manualCommand) {
                player.sendMessage("\u00a7cСначала найди заинтересованного слайма и кликни по нему сосалкой.");
                player.sendMessage("\u00a77Возьми сосалку через /sosat и найди слайма в охотничьей зоне.");
            }
            return;
        }

        if (!modLoaded && !loadClientMod()) {
            activeTargets.remove(player.getUniqueId());
            player.sendMessage("\u00a7cКлиентский мод не найден: plugins/Slimes/SlimeHunt-bundle.jar");
            player.sendMessage("\u00a77Собери :slimehunt-client:jar и положи bundle jar в папку Slimes.");
            return;
        }

        StartRequest request = new StartRequest(ThreadLocalRandom.current().nextInt());
        if (loadedPlayers.contains(player.getUniqueId())) {
            sendStart(player, request);
            return;
        }

        pendingStarts.put(player.getUniqueId(), request);
        player.sendMessage("\u00a7eЗагружаю клиентский мод SlimeHunt...");
        if (!sendClientMod(player)) {
            pendingStarts.remove(player.getUniqueId());
            activeTargets.remove(player.getUniqueId());
            player.sendMessage("\u00a7cНе удалось отправить клиентский мод SlimeHunt.");
            player.sendMessage("\u00a77Проверь размер bundle и консоль сервера.");
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingStarts.containsKey(player.getUniqueId()) && player.isOnline()) {
                sendClientMod(player);
            }
        }, LOAD_RETRY_TICKS);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            StartRequest pending = pendingStarts.remove(player.getUniqueId());
            if (pending != null && player.isOnline() && !loadedPlayers.contains(player.getUniqueId())) {
                activeTargets.remove(player.getUniqueId());
                player.sendMessage("\u00a7cКлиентский мод SlimeHunt не загрузился.");
                player.sendMessage("\u00a77Проверь консоль клиента и сервера.");
            }
        }, LOAD_TIMEOUT_TICKS);
    }

    private boolean loadClientMod() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.isDirectory() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder " + dataFolder.getAbsolutePath());
        }

        File file = new File(dataFolder, "SlimeHunt-bundle.jar");
        if (!file.isFile()) {
            this.modLoaded = false;
            return false;
        }

        Wada.get().getModLoader().load(MOD_NAME, file);
        this.modLoaded = true;
        plugin.getLogger().info("Loaded client mod bundle " + file.getAbsolutePath());
        return true;
    }

    private boolean sendClientMod(Player player) {
        try {
            Wada.get().getModLoader().send(MOD_NAME, player);
            plugin.getLogger().info("Sent " + MOD_NAME + " client mod to " + player.getName());
            return true;
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException exception) {
            plugin.getLogger().warning("Could not send " + MOD_NAME + " client mod to " + player.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    private void scheduleClientModRetries(UUID playerId) {
        for (long delay : MOD_SEND_RETRY_TICKS) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline() && !this.loadedPlayers.contains(playerId)) {
                    this.plugin.getLogger().info("Retrying " + MOD_NAME + " client mod for " + player.getName());
                    sendClientMod(player);
                }
            }, delay);
        }
    }

    private void sendLoadingOpen(Player player, String message, int progressPercent) {
        openedLoadingPlayers.add(player.getUniqueId());
        new ModTransfer()
                .writeString(message)
                .writeInt(clampProgress(progressPercent))
                .writeString(loadingArtUrl())
                .send(LOADING_OPEN_CHANNEL, player);
    }

    /**
     * Картинка экрана загрузки лежит не в бандле, а на URL из конфига:
     * бандл легче на 160 КБ, а арт можно поменять без пересборки мода.
     */
    private String loadingArtUrl() {
        return this.plugin.getConfig().getString("loading.art-url", DEFAULT_LOADING_ART_URL);
    }

    private void sendLoadingClose(Player player) {
        new ModTransfer().send(LOADING_CLOSE_CHANNEL, player);
    }

    private void scheduleLoadingOpenRetries(UUID playerId, String message, int progressPercent) {
        for (long delay : LOADING_PACKET_RETRY_TICKS) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()
                        && !completedLoadingPlayers.contains(playerId)
                        && !openedLoadingPlayers.contains(playerId)) {
                    sendLoadingOpen(player, message, progressPercent);
                }
            }, delay);
        }
    }

    private void openPendingLoading(Player player) {
        UUID playerId = player.getUniqueId();
        LoadingStatus status = pendingLoadingStatuses.remove(playerId);
        if (status == null) {
            return;
        }
        sendLoadingOpen(player, status.message, status.progressPercent);
        scheduleLoadingOpenRetries(playerId, status.message, status.progressPercent);
    }

    private void scheduleLoadingCloseRetries(UUID playerId) {
        for (long delay : LOADING_PACKET_RETRY_TICKS) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline() && completedLoadingPlayers.contains(playerId)) {
                    sendLoadingClose(player);
                }
            }, delay);
        }
    }

    private void scheduleLoadingCloseAfterLateOpen(UUID playerId) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && completedLoadingPlayers.contains(playerId)) {
                sendLoadingClose(player);
                scheduleLoadingCloseRetries(playerId);
            }
        }, LOADING_CLOSE_AFTER_LATE_OPEN_TICKS);
    }

    private static int clampProgress(int progressPercent) {
        return Math.max(0, Math.min(100, progressPercent));
    }

    private static String readClientVersion(ModTransfer transfer) {
        try {
            return transfer.readString();
        } catch (RuntimeException exception) {
            return "unknown";
        }
    }

    private void sendStart(Player player, StartRequest request) {
        new ModTransfer()
            .writeInt(request.seed)
            .writeInt(TOTAL_ROUNDS)
            .writeInt(REQUIRED_HITS)
            .writeInt(ROUND_TIMEOUT_MS)
            .send(START_CHANNEL, player);

        player.sendMessage("\u00a7dОхота на слайма: \u00a7fлови круги мышкой!");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeTargets.remove(player.getUniqueId()) != null && player.isOnline()) {
                player.sendMessage("\u00a7cОхота сброшена: мини-игра не вернула результат.");
            }
        }, GAME_TIMEOUT_TICKS);
    }

    private void finish(Player player, int hits, int total, boolean success) {
        pendingStarts.remove(player.getUniqueId());
        SlimeCaptureTarget target = activeTargets.remove(player.getUniqueId());
        if (success) {
            if (target != null && slimeWorldService != null) {
                slimeWorldService.completeCapture(player, target, hits, total);
                return;
            }
            player.getInventory().addItem(new ItemStack(Material.SLIME_BALL, 1));
            player.sendMessage("\u00a7aСлайм пойман! \u00a77" + hits + "/" + total);
        } else {
            player.sendMessage("\u00a7cСлайм сбежал. \u00a77" + hits + "/" + total);
        }
    }

    private static final class StartRequest {
        private final int seed;

        private StartRequest(int seed) {
            this.seed = seed;
        }
    }

    private static final class LoadingStatus {
        private final String message;
        private final int progressPercent;

        private LoadingStatus(String message, int progressPercent) {
            this.message = message;
            this.progressPercent = progressPercent;
        }
    }
}
