package dev.lhoopy.slimehunt.client;

import gg.cristalix.enginex.color.Color;
import gg.cristalix.enginex.color.palette.ButtonColor;
import gg.cristalix.enginex.element.carved.CarvedRectangle;
import gg.cristalix.enginex.element.carved.ProgressBar;
import gg.cristalix.enginex.element.layout.type.GridLayout;
import gg.cristalix.enginex.element.layout.type.HorizontalLayout;
import gg.cristalix.enginex.element.layout.type.VerticalLayout;
import gg.cristalix.enginex.element.scrollview.type.VerticalScrollView;
import gg.cristalix.enginex.element.screen.type.GuiScreen;
import gg.cristalix.enginex.math.Relative;
import gg.cristalix.enginex.transfer.ModTransfer;

import java.util.ArrayList;
import java.util.List;

final class FarmPlotScreen extends GuiScreen {
    private static final double STATE_WIDTH = 430.0;
    private static final double CONTENT_WIDTH = 965.0;
    private static final double LIST_HEIGHT = 430.0;

    private final MenuData data;

    FarmPlotScreen(MenuData data) {
        this.data = data;

        setSize(1920, 1080, 0);
        setColor(SlimeUi.OVERLAY);
        setBlur(SlimeUi.BLUR);

        HorizontalLayout body = SlimeUi.bodyRow();
        body.addChild(buildState(), buildContent());

        VerticalLayout window = SlimeUi.window();
        window.addChild(
                SlimeUi.header(data.displayName, typeTitle(data.plotTypeId), SlimeUi.MUTED, this::close),
                body,
                buildFooter()
        );
        addChild(window);
    }

    // --- состояние грядки ------------------------------------------------

    private CarvedRectangle buildState() {
        CarvedRectangle panel = SlimeUi.panel(STATE_WIDTH, SlimeUi.BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Состояние",
                data.planted ? "Растение посажено" : "Грядка свободна");
        column.setChildOrigin(Relative.LEFT);

        double content = STATE_WIDTH - SlimeUi.INSET * 2;
        VerticalLayout stack = SlimeUi.column(SlimeUi.GAP);
        stack.setChildOrigin(Relative.LEFT);

        if (data.planted) {
            CarvedRectangle icon = SlimeUi.card(content, 132.0);
            VerticalLayout iconColumn = SlimeUi.column(8.0);
            iconColumn.addChild(
                    SlimeUi.text(data.plantName, SlimeUi.LEAD, SlimeUi.WHITE),
                    SlimeUi.text(data.ready ? "Готово к сбору"
                                    : "До сбора: " + SlimeUi.formatTime(data.remainingSeconds),
                            SlimeUi.BODY, data.ready ? SlimeUi.GREEN : SlimeUi.MUTED)
            );
            icon.addChild(iconColumn);
            stack.addChild(icon);

            ProgressBar bar = new ProgressBar();
            bar.setSize(content, 34.0, 0.0);
            bar.setOriginAndAlign(Relative.CENTER);
            bar.setColor(SlimeUi.CARD);
            bar.setOutlineColor(SlimeUi.BORDER);
            bar.setShadowSize(0.0F);
            bar.setInset(3.0);
            bar.setProgress(Math.max(0.0, Math.min(1.0, data.growthPercent / 100.0)));
            bar.setText(data.growthPercent + "%");
            bar.getBar().setColor(data.ready ? SlimeUi.GREEN : SlimeUi.ACCENT);
            stack.addChild(bar);

            stack.addChild(infoCard(content, "Влажность",
                    data.waterSeconds > 0 ? SlimeUi.formatTime(data.waterSeconds) : "Сухая",
                    data.waterSeconds > 0 ? SlimeUi.GREEN : SlimeUi.GOLD));
            stack.addChild(infoCard(content, "Урожай", "x" + data.harvestAmount, SlimeUi.GREEN));
        } else {
            CarvedRectangle icon = SlimeUi.card(content, 152.0);
            VerticalLayout iconColumn = SlimeUi.column(8.0);
            iconColumn.addChild(
                    SlimeUi.text("Выберите семена", SlimeUi.LEAD, SlimeUi.WHITE),
                    SlimeUi.text("Одна грядка выращивает одно растение", SlimeUi.BODY, SlimeUi.MUTED)
            );
            icon.addChild(iconColumn);
            stack.addChild(icon);
            stack.addChild(infoCard(content, "Текущий стиль", typeTitle(data.plotTypeId), SlimeUi.ACCENT));
            stack.addChild(infoCard(content, "Полив",
                    data.waterSeconds > 0 ? SlimeUi.formatTime(data.waterSeconds) : "Не полита",
                    data.waterSeconds > 0 ? SlimeUi.GREEN : SlimeUi.MUTED));
        }

        column.addChild(stack);
        return panel;
    }

    private static CarvedRectangle infoCard(double width, String title, String value, Color valueColor) {
        CarvedRectangle card = SlimeUi.card(width, 66.0);
        VerticalLayout column = SlimeUi.column(6.0);
        column.setChildOrigin(Relative.LEFT);
        column.setOriginAndAlign(Relative.LEFT);
        column.setPosX(SlimeUi.INSET);
        column.addChild(
                SlimeUi.leftText(title, SlimeUi.CAPTION, SlimeUi.MUTED),
                SlimeUi.leftText(value, SlimeUi.BODY, valueColor)
        );
        card.addChild(column);
        return card;
    }

    // --- правая колонка --------------------------------------------------

    private CarvedRectangle buildContent() {
        return data.planted ? buildCare() : buildSeeds();
    }

    private CarvedRectangle buildSeeds() {
        CarvedRectangle panel = SlimeUi.panel(CONTENT_WIDTH, SlimeUi.BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Семена в хранилище",
                "Нажмите на карточку, чтобы посадить");
        column.setChildOrigin(Relative.LEFT);

        double content = CONTENT_WIDTH - SlimeUi.INSET * 2;

        if (data.seeds.isEmpty()) {
            CarvedRectangle holder = SlimeUi.carved(content, LIST_HEIGHT, SlimeUi.CLEAR);
            holder.setOutlineColor(SlimeUi.CLEAR);
            VerticalLayout empty = SlimeUi.column(10.0);
            empty.addChild(
                    SlimeUi.text("Семян пока нет", SlimeUi.LEAD, SlimeUi.WHITE),
                    SlimeUi.text("Создайте их на столе фермера", SlimeUi.BODY, SlimeUi.MUTED)
            );
            holder.addChild(empty);
            column.addChild(holder);
            return panel;
        }

        VerticalScrollView<VerticalLayout> scroll = SlimeUi.scroll(content, LIST_HEIGHT, SlimeUi.GAP);
        int rows = Math.max(1, (data.seeds.size() + 2) / 3);
        GridLayout grid = SlimeUi.grid(3, rows, SlimeUi.GAP);
        for (SeedData seed : data.seeds) {
            grid.addChild(seedCard(seed));
        }
        scroll.getLayout().addChild(grid);
        column.addChild(scroll);
        return panel;
    }

    private CarvedRectangle seedCard(SeedData seed) {
        boolean fits = seed.plotTypeId.equals(data.plotTypeId);
        CarvedRectangle card = SlimeUi.card(288.0, 132.0);
        card.setOutlineColor(fits ? SlimeUi.GREEN : SlimeUi.BORDER_SOFT);
        card.setTooltip(fits ? "Подходит этой грядке" : "На этой грядке растёт медленнее");

        VerticalLayout column = SlimeUi.column(6.0);
        column.setChildOrigin(Relative.LEFT);
        column.setOriginAndAlign(Relative.LEFT);
        column.setPosX(SlimeUi.INSET);
        column.addChild(
                SlimeUi.cardTitle(SlimeUi.shorten(seed.name, 20)),
                SlimeUi.leftText("В наличии: " + seed.amount, SlimeUi.BODY, SlimeUi.GREEN),
                SlimeUi.leftText(SlimeUi.formatTime(seed.growthSeconds), SlimeUi.BODY, SlimeUi.MUTED),
                SlimeUi.leftText(fits ? "Подходит грядке" : "Рост медленнее", SlimeUi.CAPTION,
                        fits ? SlimeUi.GREEN : SlimeUi.GOLD)
        );
        card.addChild(column);
        SlimeUi.interactive(card, () -> action("plant", seed.seedId));
        return card;
    }

    private CarvedRectangle buildCare() {
        CarvedRectangle panel = SlimeUi.panel(CONTENT_WIDTH, SlimeUi.BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Уход за растением",
                "Поливайте грядку и собирайте созревший урожай");
        column.setChildOrigin(Relative.LEFT);

        double content = CONTENT_WIDTH - SlimeUi.INSET * 2;
        VerticalLayout stack = SlimeUi.column(SlimeUi.GAP);
        stack.setChildOrigin(Relative.LEFT);

        CarvedRectangle summary = SlimeUi.card(content, 190.0);
        VerticalLayout rows = SlimeUi.column(12.0);
        rows.setChildOrigin(Relative.LEFT);
        rows.setOriginAndAlign(Relative.LEFT);
        rows.setPosX(SlimeUi.INSET);
        rows.addChild(
                SlimeUi.leftText(data.plantName, SlimeUi.LEAD, SlimeUi.WHITE),
                SlimeUi.leftText("Рост: " + data.growthPercent + "%", SlimeUi.BODY,
                        data.ready ? SlimeUi.GREEN : SlimeUi.WHITE),
                SlimeUi.leftText(data.ready ? "Можно собирать"
                                : "Осталось: " + SlimeUi.formatTime(data.remainingSeconds),
                        SlimeUi.BODY, data.ready ? SlimeUi.GREEN : SlimeUi.WHITE),
                SlimeUi.leftText("Стиль: " + typeTitle(data.plotTypeId), SlimeUi.BODY, SlimeUi.MUTED)
        );
        summary.addChild(rows);
        stack.addChild(summary);

        HorizontalLayout actions = SlimeUi.row(SlimeUi.GAP);
        actions.addChild(
                SlimeUi.button(data.waterSeconds > 0 ? "Продлить полив" : "Полить",
                        (content - SlimeUi.GAP) / 2.0, 66.0, ButtonColor.BLUE, () -> action("water", "")),
                SlimeUi.button(data.ready ? "Собрать урожай" : "Урожай растёт",
                        (content - SlimeUi.GAP) / 2.0, 66.0,
                        data.ready ? ButtonColor.GREEN : ButtonColor.GRAY,
                        data.ready ? () -> action("harvest", "") : null)
        );
        stack.addChild(actions);

        CarvedRectangle hint = SlimeUi.card(content, 96.0);
        VerticalLayout hintColumn = SlimeUi.column(8.0);
        hintColumn.setChildOrigin(Relative.LEFT);
        hintColumn.setOriginAndAlign(Relative.LEFT);
        hintColumn.setPosX(SlimeUi.INSET);
        hintColumn.addChild(
                SlimeUi.leftText("Совет", SlimeUi.BODY, SlimeUi.GOLD),
                SlimeUi.leftText(data.waterSeconds > 0
                        ? "Политое растение растёт с полной скоростью."
                        : "Сухое растение растёт в пять раз медленнее.", SlimeUi.BODY, SlimeUi.WHITE),
                SlimeUi.leftText("Неподходящий стиль грядки дополнительно замедляет рост.",
                        SlimeUi.CAPTION, SlimeUi.MUTED)
        );
        hint.addChild(hintColumn);
        stack.addChild(hint);

        column.addChild(stack);
        return panel;
    }

    // --- футер -----------------------------------------------------------

    private CarvedRectangle buildFooter() {
        CarvedRectangle footer = SlimeUi.panel(SlimeUi.WINDOW_WIDTH, SlimeUi.FOOTER_HEIGHT);

        HorizontalLayout row = SlimeUi.row(SlimeUi.GAP);
        int count = Math.min(6, data.types.size());
        for (int index = 0; index < count; index++) {
            PlotTypeData type = data.types.get(index);
            boolean selected = type.id.equals(data.plotTypeId);
            row.addChild(SlimeUi.button(type.title, 200.0, 56.0,
                    selected ? ButtonColor.GREEN : ButtonColor.BLUE,
                    selected ? null : () -> action("type", type.id)));
        }
        footer.addChild(row);
        return footer;
    }

    private void action(String action, String argument) {
        new ModTransfer().writeString(action).writeString(data.plotId).writeString(argument)
                .send(SlimeHuntMod.PLOT_ACTION_CHANNEL);
    }

    private static String typeTitle(String typeId) {
        if ("wet".equals(typeId)) return "Влажная";
        if ("mycelium".equals(typeId)) return "Грибница";
        if ("hot".equals(typeId)) return "Горячая";
        if ("crystal".equals(typeId)) return "Кристальная";
        if ("sky".equals(typeId)) return "Небесная";
        return "Обычная";
    }

    /** Читает пакет slimehunt:plot. */
    static MenuData read(ModTransfer transfer) {
        String plotId = transfer.readString();
        String displayName = transfer.readString();
        String plotTypeId = transfer.readString();
        boolean planted = transfer.readBoolean();
        String plantName = transfer.readString();
        String plantId = transfer.readString();
        int growthPercent = transfer.readInt();
        int remainingSeconds = transfer.readInt();
        boolean ready = transfer.readBoolean();
        int harvestAmount = transfer.readInt();
        int waterSeconds = transfer.readInt();
        int seedCount = transfer.readInt();
        List<SeedData> seeds = new ArrayList<>(Math.max(0, seedCount));
        for (int index = 0; index < seedCount; index++) {
            seeds.add(new SeedData(transfer.readString(), transfer.readString(), transfer.readString(),
                    transfer.readInt(), transfer.readInt()));
        }
        int typeCount = transfer.readInt();
        List<PlotTypeData> types = new ArrayList<>(Math.max(0, typeCount));
        for (int index = 0; index < typeCount; index++) {
            types.add(new PlotTypeData(transfer.readString(), transfer.readString()));
        }
        return new MenuData(plotId, displayName, plotTypeId, planted, plantName, plantId, growthPercent,
                remainingSeconds, ready, harvestAmount, waterSeconds, seeds, types);
    }

    static final class MenuData {
        final String plotId;
        final String displayName;
        final String plotTypeId;
        final boolean planted;
        final String plantName;
        final String plantId;
        final int growthPercent;
        final int remainingSeconds;
        final boolean ready;
        final int harvestAmount;
        final int waterSeconds;
        final List<SeedData> seeds;
        final List<PlotTypeData> types;

        MenuData(String plotId, String displayName, String plotTypeId, boolean planted, String plantName,
                 String plantId, int growthPercent, int remainingSeconds, boolean ready, int harvestAmount,
                 int waterSeconds, List<SeedData> seeds, List<PlotTypeData> types) {
            this.plotId = plotId;
            this.displayName = displayName;
            this.plotTypeId = plotTypeId;
            this.planted = planted;
            this.plantName = plantName;
            this.plantId = plantId;
            this.growthPercent = growthPercent;
            this.remainingSeconds = remainingSeconds;
            this.ready = ready;
            this.harvestAmount = harvestAmount;
            this.waterSeconds = waterSeconds;
            this.seeds = seeds;
            this.types = types;
        }
    }

    static final class SeedData {
        final String seedId;
        final String name;
        final String plotTypeId;
        final int amount;
        final int growthSeconds;

        SeedData(String seedId, String name, String plotTypeId, int amount, int growthSeconds) {
            this.seedId = seedId;
            this.name = name;
            this.plotTypeId = plotTypeId;
            this.amount = amount;
            this.growthSeconds = growthSeconds;
        }
    }

    static final class PlotTypeData {
        final String id;
        final String title;

        PlotTypeData(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }
}
