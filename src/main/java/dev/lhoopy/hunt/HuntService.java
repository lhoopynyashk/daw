package dev.lhoopy.hunt;

import dev.lhoopy.core.SlimesPlugin;
import gg.cristalix.wada.Wada;
import gg.cristalix.wada.color.Color;
import gg.cristalix.wada.color.palette.Palette;
import gg.cristalix.wada.common.menu.icon.ItemIcon;
import gg.cristalix.wada.common.menu.tooltip.Tooltip;
import gg.cristalix.wada.component.alert.animation.CursorAlertAnimation;
import gg.cristalix.wada.component.alert.data.CursorMessage;
import gg.cristalix.wada.component.menu.choice.common.ChoiceButton;
import gg.cristalix.wada.component.menu.choice.common.ChoiceMenu;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class HuntService {
    private static final int TOTAL_ROUNDS = 7;
    private static final int REQUIRED_HITS = 5;
    private static final int CHOICES_PER_ROUND = 5;
    private static final long ROUND_TIMEOUT_TICKS = 70L;

    private final SlimesPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, HuntSession> sessions = new HashMap<>();

    public HuntService(SlimesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cТолько игрок может ловить слаймов.");
            return;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        if (this.sessions.containsKey(playerId)) {
            player.sendMessage("\u00a7eТы уже охотишься на слайма.");
            return;
        }

        HuntSession session = new HuntSession(playerId);
        this.sessions.put(playerId, session);
        player.sendMessage("\u00a7aСлайм заинтересовался. Кликай по ярким кругам.");
        sendCursor(player, "\u00a7dОхота началась", "\u00a7fЛКМ по яркому кругу");
        openNextRound(player, session);
    }

    public void shutdown() {
        for (HuntSession session : this.sessions.values()) {
            if (session.timeoutTask != null) {
                session.timeoutTask.cancel();
            }
        }
        this.sessions.clear();
    }

    private void openNextRound(Player player, HuntSession session) {
        session.round++;
        session.roundToken++;

        if (session.round > TOTAL_ROUNDS) {
            finish(player, session);
            return;
        }

        int targetIndex = this.random.nextInt(CHOICES_PER_ROUND);
        List<ChoiceButton> buttons = new ArrayList<>();
        for (int i = 0; i < CHOICES_PER_ROUND; i++) {
            boolean target = i == targetIndex;
            buttons.add(createCircleButton(player, session, target, i));
        }

        ChoiceMenu menu = ChoiceMenu.builder()
                .title("\u00a7dОхота на слайма")
                .description("\u00a7fРаунд \u00a7e" + session.round + "\u00a77/\u00a7e" + TOTAL_ROUNDS
                        + "   \u00a7fПопаданий: \u00a7a" + session.hits + "\u00a77/\u00a7a" + REQUIRED_HITS
                        + "\n\u00a77Наведи курсор и нажми ЛКМ по яркому кругу до таймера.")
                .buttons(buttons)
                .build();

        session.activeMenu = menu;
        int token = session.roundToken;
        if (session.timeoutTask != null) {
            session.timeoutTask.cancel();
        }
        session.timeoutTask = this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            HuntSession current = this.sessions.get(player.getUniqueId());
            if (current == session && current.roundToken == token) {
                current.misses++;
                player.playSound(player.getLocation(), "note.bass", 0.8f, 0.7f);
                sendCursor(player, "\u00a7cПромах", "\u00a77Круг исчез");
                openNextRound(player, current);
            }
        }, ROUND_TIMEOUT_TICKS);

        Wada.get().getMenuManager().open(menu, player);
    }

    private ChoiceButton createCircleButton(Player player, HuntSession session, boolean target, int slot) {
        Material material = target ? Material.SLIME_BALL : Material.STAINED_GLASS_PANE;
        Color color = target ? Palette.PINK : decoyColor(slot);
        String title = target ? "\u00a7dЦЕЛЬ" : "\u00a78МИМО";
        String description = target
                ? "\u00a7fЛКМ, пока круг активен\n\u00a77Слайм почти в контейнере"
                : "\u00a78Ложная точка\n\u00a77Не трать клик";
        String overlay = target ? "КЛИК" : "МИМО";

        return ChoiceButton.builder()
                .title(title)
                .description(description)
                .overlayLabel(overlay)
                .backgroundColor(color)
                .icon(ItemIcon.builder()
                        .itemStack(new ItemStack(material))
                        .scale(target ? 1.35f : 0.85f)
                        .build())
                .tooltip(Tooltip.builder()
                        .title(target ? "Цель" : "Ложная точка")
                        .description(target ? "Попади по кругу до исчезновения." : "Этот круг не интересует слайма.")
                        .cornerColor(color)
                        .build())
                .onPlayerLeftClick((clicker, button) -> handleCircleClick(clicker, session, target))
                .build();
    }

    private Color decoyColor(int slot) {
        switch (slot % 4) {
            case 0:
                return Palette.GRAY_DARK_62;
            case 1:
                return Palette.BLUE_DARK_62;
            case 2:
                return Palette.PURPLE_DARK_62;
            default:
                return Palette.CYAN_DARK_62;
        }
    }

    private void handleCircleClick(Player player, HuntSession session, boolean hit) {
        HuntSession current = this.sessions.get(player.getUniqueId());
        if (current != session) {
            return;
        }

        if (session.timeoutTask != null) {
            session.timeoutTask.cancel();
            session.timeoutTask = null;
        }

        if (hit) {
            session.hits++;
            player.playSound(player.getLocation(), "random.orb", 0.8f, 1.4f);
            sendCursor(player, "\u00a7aПопадание", "\u00a7f" + session.hits + "/" + REQUIRED_HITS);
        } else {
            session.misses++;
            player.playSound(player.getLocation(), "note.bass", 0.8f, 0.6f);
            sendCursor(player, "\u00a7cПромах", "\u00a77Слайм нервничает");
        }

        openNextRound(player, session);
    }

    private void finish(Player player, HuntSession session) {
        if (session.timeoutTask != null) {
            session.timeoutTask.cancel();
            session.timeoutTask = null;
        }
        this.sessions.remove(player.getUniqueId());
        Wada.get().getMenuManager().close(player);

        if (session.hits >= REQUIRED_HITS) {
            player.getInventory().addItem(new ItemStack(Material.SLIME_BALL));
            player.playSound(player.getLocation(), "random.levelup", 0.9f, 1.2f);
            player.sendMessage("\u00a7aСлайм пойман и отправлен в контейнер.");
            sendCursor(player, "\u00a7aСлайм пойман", "\u00a7fТочность: " + session.hits + "/" + TOTAL_ROUNDS);
            return;
        }

        player.playSound(player.getLocation(), "mob.villager.no", 0.9f, 0.8f);
        player.sendMessage("\u00a7cСлайм потерял интерес. Попробуй другую приманку.");
        sendCursor(player, "\u00a7cОхота провалена", "\u00a77Попаданий: " + session.hits + "/" + REQUIRED_HITS);
    }

    private void sendCursor(Player player, String firstLine, String secondLine) {
        try {
            CursorMessage message = CursorMessage.builder()
                    .messages(firstLine, secondLine)
                    .animation(CursorAlertAnimation.CENTERED_MOVEMENT)
                    .duration(1.2)
                    .build();
            Wada.get().getAlertManager().sendCursorMessage(message, player);
        } catch (RuntimeException ignored) {
            // Cursor alerts are visual sugar; the hunt menu itself is the important part.
        }
    }
}
