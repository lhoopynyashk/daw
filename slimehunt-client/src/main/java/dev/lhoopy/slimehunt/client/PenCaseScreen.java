package dev.lhoopy.slimehunt.client;

import gg.cristalix.enginex.color.Color;
import gg.cristalix.enginex.color.palette.ButtonColor;
import gg.cristalix.enginex.element.Container;
import gg.cristalix.enginex.element.Text;
import gg.cristalix.enginex.element.button.Button;
import gg.cristalix.enginex.element.carved.CarvedRectangle;
import gg.cristalix.enginex.element.layout.type.HorizontalLayout;
import gg.cristalix.enginex.element.layout.type.VerticalLayout;
import gg.cristalix.enginex.element.mask.ScissorMask;
import gg.cristalix.enginex.element.screen.type.GuiScreen;
import gg.cristalix.enginex.event.tick.PreTickEvent;
import gg.cristalix.enginex.math.Relative;
import gg.cristalix.enginex.transfer.ModTransfer;

import java.util.ArrayList;
import java.util.List;

final class PenCaseScreen extends GuiScreen {
    private static final double CARD_STEP = 188.0;
    private static final long SPIN_DURATION_MILLIS = 4500L;
    private static final String[] RARITIES = {"common", "rare", "epic", "legendary", "mythic"};
    private static final String[] CHANCES = {"48%", "27%", "16%", "7%", "2%"};

    PenCaseScreen(MenuData data) {
        setSize(1920, 1080, 0);
        setColor(SlimeUi.OVERLAY);
        setBlur(SlimeUi.BLUR);

        CarvedRectangle body = SlimeUi.panel(SlimeUi.WINDOW_WIDTH, SlimeUi.BODY_HEIGHT);
        CarvedRectangle footer = SlimeUi.panel(SlimeUi.WINDOW_WIDTH, SlimeUi.FOOTER_HEIGHT);

        if (data.result == null) {
            buildLobby(body, footer, data.keys);
        } else {
            buildRoulette(body, footer, data.result);
        }

        VerticalLayout window = SlimeUi.window();
        window.addChild(
                SlimeUi.header("Гача загонов", "Ключи: " + data.keys, SlimeUi.GOLD, this::close),
                body,
                footer
        );
        addChild(window);
    }

    // --- лобби -----------------------------------------------------------

    private void buildLobby(CarvedRectangle body, CarvedRectangle footer, int keys) {
        VerticalLayout column = SlimeUi.section(body, "Кейс с чертежами загонов",
                "Один ключ запускает одну прокрутку");
        column.setChildOrigin(Relative.LEFT);

        HorizontalLayout row = SlimeUi.row(SlimeUi.GAP);
        for (int index = 0; index < RARITIES.length; index++) {
            row.addChild(rarityCard(RARITIES[index], CHANCES[index]));
        }
        column.addChild(row);

        Button open = SlimeUi.button(keys > 0 ? "Крутить за 1 ключ" : "Нет ключей",
                430.0, 56.0, keys > 0 ? ButtonColor.BLUE : ButtonColor.GRAY,
                keys > 0 ? () -> new ModTransfer().send(SlimeHuntMod.PEN_CASE_OPEN_CHANNEL) : null);
        footer.addChild(open);
    }

    private static CarvedRectangle rarityCard(String rarity, String chance) {
        Color accent = SlimeUi.rarityColor(rarity);
        CarvedRectangle card = SlimeUi.carved(248.0, 300.0, SlimeUi.raritySurface(rarity));
        card.setOutlineColor(accent);

        VerticalLayout content = SlimeUi.column(12.0);
        CarvedRectangle emblem = SlimeUi.carved(96.0, 96.0, new Color(20, 22, 26, 0.72));
        emblem.setOutlineColor(accent);
        emblem.addChild(SlimeUi.text("?", SlimeUi.GLYPH, accent));
        content.addChild(
                emblem,
                SlimeUi.text(SlimeUi.rarityTitle(rarity), SlimeUi.BODY, accent),
                SlimeUi.text(chance, SlimeUi.LEAD, SlimeUi.WHITE)
        );
        card.addChild(content);
        return card;
    }

    // --- рулетка ---------------------------------------------------------

    private void buildRoulette(CarvedRectangle body, CarvedRectangle footer, ResultData result) {
        VerticalLayout column = SlimeUi.section(body, "Рулетка чертежей",
                "Награда определена сервером");
        column.setChildOrigin(Relative.LEFT);

        ScissorMask viewport = new ScissorMask();
        viewport.setSize(1300.0, 240.0, 0.0);
        viewport.setOriginAndAlign(Relative.CENTER);
        viewport.setPosZ(1.0);
        viewport.setColor(new Color(17, 19, 23, 0.88));
        viewport.setOutlineColor(SlimeUi.BORDER);
        viewport.setShadowSize(0.0F);

        Container reel = new Container();
        reel.setPos(0.0, 0.0, 2.0);
        reel.setSize(1.0, 1.0, 0.0);
        reel.setOriginAndAlign(Relative.CENTER);

        CarvedRectangle winner = null;
        for (int index = 0; index < result.roulette.size(); index++) {
            CarvedRectangle card = rouletteCard(result.roulette.get(index));
            card.setPosX(index * CARD_STEP);
            reel.addChild(card);
            if (index == result.winnerIndex) {
                winner = card;
            }
        }
        viewport.addChild(reel);
        column.addChild(viewport);

        CarvedRectangle marker = SlimeUi.carved(4.0, 240.0, new Color(244, 184, 62, 0.78));
        marker.setOutlineColor(SlimeUi.GOLD);
        marker.setPosZ(4.0);
        viewport.addChild(marker);

        CarvedRectangle resultPanel = SlimeUi.card(1100.0, 96.0);
        resultPanel.setOutlineColor(SlimeUi.rarityColor(result.rarity));
        VerticalLayout resultColumn = SlimeUi.column(6.0);
        resultColumn.addChild(
                SlimeUi.text("Выпал чертёж: " + result.name, SlimeUi.LEAD,
                        SlimeUi.rarityColor(result.rarity)),
                SlimeUi.text(SlimeUi.shorten(result.description, 82), SlimeUi.BODY, SlimeUi.WHITE),
                SlimeUi.text("В коллекции: " + result.count, SlimeUi.BODY, SlimeUi.MUTED)
        );
        resultPanel.addChild(resultColumn);
        resultPanel.setEnabled(false);
        column.addChild(resultPanel);

        Button styles = SlimeUi.button("Выбрать стиль", 430.0, 56.0, ButtonColor.BLUE,
                () -> new ModTransfer().send(SlimeHuntMod.PEN_STYLE_REQUEST_CHANNEL));
        styles.setEnabled(false);
        footer.addChild(styles);

        Text spinning = SlimeUi.text("Рулетка вращается...", SlimeUi.BODY, SlimeUi.GOLD);
        footer.addChild(spinning);

        animateRoulette(reel, winner, result.winnerIndex, spinning, resultPanel, styles);
    }

    private static CarvedRectangle rouletteCard(RouletteEntry entry) {
        Color accent = SlimeUi.rarityColor(entry.rarity);
        CarvedRectangle card = SlimeUi.carved(168.0, 200.0, SlimeUi.raritySurface(entry.rarity));
        card.setOutlineColor(accent);

        VerticalLayout content = SlimeUi.column(8.0);
        CarvedRectangle emblem = SlimeUi.carved(82.0, 76.0, new Color(18, 20, 24, 0.78));
        emblem.setOutlineColor(accent);
        emblem.addChild(SlimeUi.text("?", SlimeUi.GLYPH, accent));
        content.addChild(
                emblem,
                SlimeUi.text(SlimeUi.shorten(entry.name, 20), SlimeUi.CAPTION, SlimeUi.WHITE),
                SlimeUi.text(SlimeUi.rarityTitle(entry.rarity), SlimeUi.CAPTION, accent)
        );
        card.addChild(content);
        return card;
    }

    private void animateRoulette(Container reel, CarvedRectangle winnerCard, int winnerIndex,
                                 Text spinning, CarvedRectangle resultPanel, Button styles) {
        final long startedAt = System.currentTimeMillis();
        final double targetX = -winnerIndex * CARD_STEP;
        final boolean[] finished = {false};
        reel.registerEvent(PreTickEvent.class, event -> {
            if (finished[0]) {
                return;
            }
            double progress = Math.min(1.0,
                    (System.currentTimeMillis() - startedAt) / (double) SPIN_DURATION_MILLIS);
            double remaining = 1.0 - progress;
            double eased = 1.0 - remaining * remaining * remaining * remaining * remaining;
            reel.setPosX(targetX * eased);
            if (progress < 1.0) {
                return;
            }
            finished[0] = true;
            reel.setPosX(targetX);
            if (winnerCard != null) {
                winnerCard.setScale(1.05);
            }
            spinning.setEnabled(false);
            resultPanel.setEnabled(true);
            styles.setEnabled(true);
        });
    }

    /** Читает пакет slimehunt:pcase. */
    static MenuData read(ModTransfer transfer) {
        int keys = transfer.readInt();
        if (!transfer.readBoolean()) {
            return new MenuData(keys, null);
        }
        String name = transfer.readString();
        String rarity = transfer.readString();
        String description = transfer.readString();
        int count = transfer.readInt();
        int winnerIndex = transfer.readInt();
        int rouletteSize = transfer.readInt();
        List<RouletteEntry> roulette = new ArrayList<>(Math.max(0, rouletteSize));
        for (int index = 0; index < rouletteSize; index++) {
            roulette.add(new RouletteEntry(transfer.readString(), transfer.readString()));
        }
        return new MenuData(keys, new ResultData(name, rarity, description, count, winnerIndex, roulette));
    }

    static final class MenuData {
        final int keys;
        final ResultData result;

        MenuData(int keys, ResultData result) {
            this.keys = keys;
            this.result = result;
        }
    }

    static final class RouletteEntry {
        final String name;
        final String rarity;

        RouletteEntry(String name, String rarity) {
            this.name = name;
            this.rarity = rarity;
        }
    }

    static final class ResultData {
        final String name;
        final String rarity;
        final String description;
        final int count;
        final int winnerIndex;
        final List<RouletteEntry> roulette;

        ResultData(String name, String rarity, String description, int count,
                   int winnerIndex, List<RouletteEntry> roulette) {
            this.name = name;
            this.rarity = rarity;
            this.description = description;
            this.count = count;
            this.winnerIndex = winnerIndex;
            this.roulette = roulette;
        }
    }
}
