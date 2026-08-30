package dev.lhoopy.pen;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PenCaseService implements PluginService {
    private static final int ROULETTE_CARD_COUNT = 31;
    private static final int ROULETTE_WINNER_INDEX = 25;
    public static final String CASE_SCREEN_CHANNEL = "slimehunt:pcase";
    public static final String CASE_OPEN_CHANNEL = "slimehunt:pcopen";
    public static final String CASE_REQUEST_CHANNEL = "slimehunt:pcshow";
    public static final String STYLE_SCREEN_CHANNEL = "slimehunt:pstyles";
    public static final String STYLE_REQUEST_CHANNEL = "slimehunt:pshow";
    public static final String STYLE_SELECT_CHANNEL = "slimehunt:pstyle";

    private final SlimesPlugin plugin;
    private final ProfileService profileService;
    private final EnginexHuntBridge clientBridge;
    private final PenStyleCatalog catalog;
    private PenService penService;

    public PenCaseService(SlimesPlugin plugin, ProfileService profileService, EnginexHuntBridge clientBridge,
                          PenStyleCatalog catalog) {
        this.plugin = plugin;
        this.profileService = profileService;
        this.clientBridge = clientBridge;
        this.catalog = catalog;
    }

    public void setPenService(PenService penService) {
        this.penService = penService;
    }

    @Override
    public void enable() {
        ModTransfer.registerChannel(CASE_OPEN_CHANNEL, (player, transfer) ->
                Bukkit.getScheduler().runTask(this.plugin, () -> openCase(player)));
        ModTransfer.registerChannel(CASE_REQUEST_CHANNEL, (player, transfer) ->
                Bukkit.getScheduler().runTask(this.plugin, () -> showCase(player)));
        ModTransfer.registerChannel(STYLE_REQUEST_CHANNEL, (player, transfer) ->
                Bukkit.getScheduler().runTask(this.plugin, () -> showStyles(player)));
        ModTransfer.registerChannel(STYLE_SELECT_CHANNEL, (player, transfer) -> {
            String styleId = transfer.readString();
            Bukkit.getScheduler().runTask(this.plugin, () -> selectStyle(player, styleId));
        });
    }

    @Override
    public void shutdown() {
    }

    public void handleCaseCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игроку.");
            return;
        }
        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("slimes.admin")) {
                player.sendMessage(ChatColor.RED + "Недостаточно прав.");
                return;
            }
            int amount = args.length > 1 ? parseAmount(args[1]) : 1;
            PlayerProfile profile = loaded(player);
            if (profile == null) {
                return;
            }
            profile.addPenCaseKeys(amount);
            this.profileService.saveLoaded(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Добавлено ключей от кейса загонов: " + amount);
        }
        showCase(player);
    }

    public void handleStyleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игроку.");
            return;
        }
        showStyles((Player) sender);
    }

    public void showCase(Player player) {
        PlayerProfile profile = loaded(player);
        if (profile == null || !ensureClient(player,
                () -> sendCase(player, profile, null, Collections.emptyList()))) {
            return;
        }
        sendCase(player, profile, null, Collections.emptyList());
    }

    private void sendCase(Player player, PlayerProfile profile, PenStyleDef result, List<PenStyleDef> roulette) {
        ModTransfer transfer = new ModTransfer()
                .writeInt(profile.getPenCaseKeys())
                .writeBoolean(result != null);
        if (result != null) {
            transfer.writeString(result.getDisplayName())
                    .writeString(result.getRarity())
                    .writeString(result.getDescription())
                    .writeInt(profile.getPenBlueprintCount(result.getId()))
                    .writeInt(ROULETTE_WINNER_INDEX)
                    .writeInt(roulette.size());
            for (PenStyleDef entry : roulette) {
                transfer.writeString(entry.getDisplayName())
                        .writeString(entry.getRarity());
            }
        }
        transfer.send(CASE_SCREEN_CHANNEL, player);
    }

    public void showStyles(Player player) {
        PlayerProfile profile = loaded(player);
        if (profile == null || !ensureClient(player, () -> sendStyles(player, profile))) {
            return;
        }
        sendStyles(player, profile);
    }

    private void sendStyles(Player player, PlayerProfile profile) {
        ModTransfer transfer = new ModTransfer().writeString(profile.getActivePenStyleId());
        int available = 1;
        for (PenStyleDef definition : this.catalog.all()) {
            if (!definition.getId().equals(PenStyleCatalog.BASIC_STYLE_ID)
                    && profile.getPenBlueprintCount(definition.getId()) > 0) {
                available++;
            }
        }
        transfer.writeInt(available);
        for (PenStyleDef definition : this.catalog.all()) {
            int count = definition.getId().equals(PenStyleCatalog.BASIC_STYLE_ID)
                    ? 1 : profile.getPenBlueprintCount(definition.getId());
            if (count <= 0) {
                continue;
            }
            transfer.writeString(definition.getId())
                    .writeString(definition.getDisplayName())
                    .writeString(definition.getRarity())
                    .writeString(definition.getDescription())
                    .writeInt(count);
        }
        transfer.send(STYLE_SCREEN_CHANNEL, player);
    }

    private void openCase(Player player) {
        PlayerProfile profile = loaded(player);
        if (profile == null) {
            return;
        }
        if (!profile.usePenCaseKey()) {
            player.sendMessage(ChatColor.RED + "Нужен ключ от кейса с загонами.");
            showCase(player);
            return;
        }
        PenStyleDef result = this.catalog.select(ThreadLocalRandom.current().nextDouble(100.0D));
        List<PenStyleDef> roulette = createRoulette(result);
        profile.addPenBlueprint(result.getId());
        this.profileService.saveLoaded(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.playSound(player.getLocation(), "random.levelup", 0.8F, 1.25F);
        player.sendMessage(ChatColor.GREEN + "Получен чертёж: " + ChatColor.WHITE + result.getDisplayName());
        }, 90L);
        sendCase(player, profile, result, roulette);
    }

    private List<PenStyleDef> createRoulette(PenStyleDef winner) {
        List<PenStyleDef> roulette = new ArrayList<>(ROULETTE_CARD_COUNT);
        for (int index = 0; index < ROULETTE_CARD_COUNT; index++) {
            PenStyleDef entry = index == ROULETTE_WINNER_INDEX
                    ? winner
                    : this.catalog.select(ThreadLocalRandom.current().nextDouble(100.0D));
            roulette.add(entry);
        }
        return roulette;
    }

    private void selectStyle(Player player, String styleId) {
        PlayerProfile profile = loaded(player);
        if (profile == null) {
            return;
        }
        PenStyleDef definition = this.catalog.get(styleId);
        if (!definition.getId().equals(styleId)) {
            player.sendMessage(ChatColor.RED + "Неизвестный стиль загона.");
            return;
        }
        if (!styleId.equals(PenStyleCatalog.BASIC_STYLE_ID) && profile.getPenBlueprintCount(styleId) <= 0) {
            player.sendMessage(ChatColor.RED + "Этот чертёж ещё не получен.");
            return;
        }
        int nextCapacity = Math.max(1, profile.getPenCapacity() + definition.getCapacityBonus());
        if (profile.getPenSlimes().size() > nextCapacity) {
            player.sendMessage(ChatColor.RED + "Сначала убери лишних слаймов. Новый стиль вмещает только " + nextCapacity + ".");
            return;
        }
        profile.setActivePenStyleId(styleId);
        this.profileService.saveLoaded(player.getUniqueId());
        player.playSound(player.getLocation(), "random.orb", 0.8F, 1.2F);
        player.sendMessage(ChatColor.GREEN + "Стиль загона изменён: " + ChatColor.WHITE + definition.getDisplayName());
        if (this.penService != null) {
            this.penService.refreshPenVisuals(player);
        }
        showStyles(player);
    }

    private PlayerProfile loaded(Player player) {
        if (!this.profileService.ensureLoaded(player)) {
            player.sendMessage(ChatColor.YELLOW + "Профиль SlimeRancher загружается.");
            return null;
        }
        return this.profileService.getLoaded(player.getUniqueId());
    }

    private boolean ensureClient(Player player, Runnable retry) {
        if (this.clientBridge.isClientModLoaded(player)) {
            return true;
        }
        if (!this.clientBridge.sendClientModTo(player)) {
            player.sendMessage(ChatColor.RED + "Клиентский мод SlimeHunt не загрузился.");
            return false;
        }
        waitForClient(player, retry, 0);
        return false;
    }

    private void waitForClient(Player player, Runnable action, int attempt) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (this.clientBridge.isClientModLoaded(player)) {
                action.run();
                return;
            }
            if (attempt < 4) {
                waitForClient(player, action, attempt + 1);
                return;
            }
            player.sendMessage(ChatColor.RED + "Клиентский мод SlimeHunt не подтвердил загрузку.");
            player.sendMessage(ChatColor.GRAY + "Проверь клиентскую консоль на ошибки SlimeHunt.");
        }, 40L);
    }

    private static int parseAmount(String raw) {
        try {
            return Math.max(1, Math.min(100, Integer.parseInt(raw)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
