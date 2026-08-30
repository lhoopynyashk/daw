package dev.lhoopy.slimehunt.client;

import gg.cristalix.enginex.color.Color;
import gg.cristalix.enginex.color.palette.ButtonColor;
import gg.cristalix.enginex.element.carved.CarvedRectangle;
import gg.cristalix.enginex.element.layout.type.GridLayout;
import gg.cristalix.enginex.element.layout.type.HorizontalLayout;
import gg.cristalix.enginex.element.layout.type.VerticalLayout;
import gg.cristalix.enginex.element.scrollview.type.VerticalScrollView;
import gg.cristalix.enginex.element.screen.type.GuiScreen;
import gg.cristalix.enginex.math.Relative;
import gg.cristalix.enginex.transfer.ModTransfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SellTerminalScreen extends GuiScreen {
    private static final double LIST_WIDTH = 878.0;
    private static final double SUMMARY_WIDTH = 517.0;

    private final MenuData data;
    private final String selectedId;
    private final int selectedAmount;

    SellTerminalScreen(MenuData data) {
        this(data, firstPlortId(data), 1);
    }

    private SellTerminalScreen(MenuData data, String selectedId, int selectedAmount) {
        this.data = data;
        this.selectedId = selectedId;
        PlortData selected = findPlort(selectedId);
        this.selectedAmount = selected == null
                ? 1 : Math.max(1, Math.min(selectedAmount, selected.totalAmount()));

        setSize(1920, 1080, 0);
        setColor(SlimeUi.OVERLAY);
        setBlur(SlimeUi.BLUR);

        VerticalLayout window = SlimeUi.window();
        HorizontalLayout body = SlimeUi.bodyRow();
        body.addChild(buildInventory(), buildSummary());
        window.addChild(
                SlimeUi.header("Терминал продажи", "Баланс: " + data.coins, SlimeUi.GOLD, this::close),
                body
        );
        addChild(window);
    }

    // --- список плортов --------------------------------------------------

    private CarvedRectangle buildInventory() {
        CarvedRectangle panel = SlimeUi.panel(LIST_WIDTH, SlimeUi.TALL_BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Плорты",
                "Выбери плорт и количество для продажи");
        column.setChildOrigin(Relative.LEFT);

        double content = LIST_WIDTH - SlimeUi.INSET * 2;

        if (data.plorts.isEmpty()) {
            CarvedRectangle holder = SlimeUi.carved(content, 540.0, SlimeUi.CLEAR);
            holder.setOutlineColor(SlimeUi.CLEAR);
            VerticalLayout empty = SlimeUi.column(12.0);
            CarvedRectangle icon = SlimeUi.card(112.0, 112.0);
            icon.addChild(SlimeUi.text("◇", SlimeUi.GLYPH_LARGE, SlimeUi.MUTED));
            empty.addChild(
                    icon,
                    SlimeUi.text("Плортов нет", SlimeUi.LEAD, SlimeUi.WHITE),
                    SlimeUi.text("Собери их из загона или переложи на склад", SlimeUi.BODY, SlimeUi.MUTED)
            );
            holder.addChild(empty);
            column.addChild(holder);
            return panel;
        }

        VerticalScrollView<VerticalLayout> scroll = SlimeUi.scroll(content, 540.0, SlimeUi.GAP);
        int rows = Math.max(1, (data.plorts.size() + 1) / 2);
        GridLayout grid = SlimeUi.grid(2, rows, SlimeUi.GAP);
        for (PlortData plort : data.plorts) {
            grid.addChild(plortCard(plort));
        }
        scroll.getLayout().addChild(grid);
        column.addChild(scroll);
        return panel;
    }

    private CarvedRectangle plortCard(PlortData plort) {
        boolean selected = plort.id.equals(selectedId);
        CarvedRectangle card = SlimeUi.carved(400.0, 110.0, selected ? SlimeUi.CARD_SELECTED : SlimeUi.CARD);
        card.setOutlineColor(selected ? SlimeUi.ACCENT : SlimeUi.BORDER);

        HorizontalLayout line = SlimeUi.row(14.0);
        line.setOriginAndAlign(Relative.LEFT);
        line.setPosX(SlimeUi.INSET);
        line.addChild(plortIcon(plort));

        VerticalLayout labels = SlimeUi.column(6.0);
        labels.setChildOrigin(Relative.LEFT);
        labels.addChild(
                SlimeUi.cardTitle(SlimeUi.shorten(plort.name, 22)),
                SlimeUi.leftText(plort.totalAmount() + " шт. доступно", SlimeUi.BODY, SlimeUi.MUTED),
                SlimeUi.leftText(plort.price + " монет / шт.", SlimeUi.BODY, SlimeUi.GOLD)
        );
        line.addChild(labels);
        card.addChild(line);

        if (!selected) {
            SlimeUi.hover(card, SlimeUi.CARD, SlimeUi.CARD_HOVER);
        }
        SlimeUi.click(card, () -> reopen(plort.id, 1));
        return card;
    }

    private static CarvedRectangle plortIcon(PlortData plort) {
        CarvedRectangle frame = SlimeUi.carved(64.0, 64.0, new Color(24, 25, 28, 0.94));
        CarvedRectangle gem = SlimeUi.carved(42.0, 42.0, colorFor(plort.id));
        gem.addChild(SlimeUi.text("◆", SlimeUi.BODY, SlimeUi.WHITE));
        frame.addChild(gem);
        return frame;
    }

    // --- сделка ----------------------------------------------------------

    private CarvedRectangle buildSummary() {
        CarvedRectangle panel = SlimeUi.panel(SUMMARY_WIDTH, SlimeUi.TALL_BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Сделка",
                "Проверь количество и подтверди продажу");
        column.setChildOrigin(Relative.LEFT);

        double content = SUMMARY_WIDTH - SlimeUi.INSET * 2;
        PlortData selected = findPlort(selectedId);

        if (selected == null) {
            CarvedRectangle holder = SlimeUi.carved(content, 540.0, SlimeUi.CLEAR);
            holder.setOutlineColor(SlimeUi.CLEAR);
            VerticalLayout empty = SlimeUi.column(10.0);
            empty.addChild(
                    SlimeUi.text("Выбери плорт слева", SlimeUi.LEAD, SlimeUi.WHITE),
                    SlimeUi.text("Терминал продаёт только доступные плорты", SlimeUi.BODY, SlimeUi.MUTED)
            );
            holder.addChild(empty);
            column.addChild(holder);
            return panel;
        }

        VerticalLayout stack = SlimeUi.column(SlimeUi.GAP);
        stack.setChildOrigin(Relative.LEFT);

        CarvedRectangle head = SlimeUi.card(content, 120.0);
        HorizontalLayout headLine = SlimeUi.row(14.0);
        headLine.setOriginAndAlign(Relative.LEFT);
        headLine.setPosX(SlimeUi.INSET);
        headLine.addChild(plortIcon(selected));
        VerticalLayout headLabels = SlimeUi.column(6.0);
        headLabels.setChildOrigin(Relative.LEFT);
        headLabels.addChild(
                SlimeUi.cardTitle(SlimeUi.shorten(selected.name, 22)),
                SlimeUi.leftText("Вакпак: " + selected.vacpackAmount + "   Склад: " + selected.storageAmount,
                        SlimeUi.BODY, SlimeUi.MUTED),
                SlimeUi.leftText("Цена: " + selected.price, SlimeUi.BODY, SlimeUi.GOLD)
        );
        headLine.addChild(headLabels);
        head.addChild(headLine);
        stack.addChild(head);

        CarvedRectangle counter = SlimeUi.card(content, 96.0);
        VerticalLayout counterColumn = SlimeUi.column(8.0);
        counterColumn.addChild(SlimeUi.text("Количество", SlimeUi.CAPTION, SlimeUi.MUTED));
        HorizontalLayout buttons = SlimeUi.row(10.0);
        buttons.addChild(
                SlimeUi.button("-10", 62.0, 40.0, ButtonColor.GRAY, () -> reopen(selectedId, selectedAmount - 10)),
                SlimeUi.button("-", 52.0, 40.0, ButtonColor.GRAY, () -> reopen(selectedId, selectedAmount - 1)),
                amountBox(),
                SlimeUi.button("+", 52.0, 40.0, ButtonColor.GRAY, () -> reopen(selectedId, selectedAmount + 1)),
                SlimeUi.button("+10", 62.0, 40.0, ButtonColor.GRAY, () -> reopen(selectedId, selectedAmount + 10))
        );
        counterColumn.addChild(buttons);
        counter.addChild(counterColumn);
        stack.addChild(counter);

        stack.addChild(SlimeUi.button("Всё этого вида: " + selected.totalAmount(), content, 42.0,
                ButtonColor.GRAY, () -> reopen(selectedId, selected.totalAmount())));

        long value = (long) selected.price * selectedAmount;
        CarvedRectangle total = SlimeUi.card(content, 70.0);
        HorizontalLayout totalLine = SlimeUi.row(0.0);
        totalLine.setOriginAndAlign(Relative.LEFT);
        totalLine.setPosX(SlimeUi.INSET);
        VerticalLayout totalLabels = SlimeUi.column(6.0);
        totalLabels.setChildOrigin(Relative.LEFT);
        totalLabels.addChild(
                SlimeUi.leftText("К продаже", SlimeUi.CAPTION, SlimeUi.MUTED),
                SlimeUi.leftText(selectedAmount + " шт.", SlimeUi.BODY, SlimeUi.WHITE)
        );
        totalLine.addChild(totalLabels);
        total.addChild(totalLine);
        total.addChild(rightValue("+" + value));
        stack.addChild(total);

        stack.addChild(SlimeUi.button("Продать выбранное", content, 54.0, ButtonColor.BLUE,
                () -> sell(selected.id, selectedAmount)));
        stack.addChild(SlimeUi.button("Продать всё  +" + data.totalValue, content, 48.0, ButtonColor.GREEN,
                () -> sell("__all__", Integer.MAX_VALUE)));

        if (data.lastAmount > 0) {
            stack.addChild(SlimeUi.leftText(
                    "Прошлая сделка: " + data.lastAmount + " шт. / +" + data.lastCoins,
                    SlimeUi.BODY, SlimeUi.MUTED));
        }

        column.addChild(stack);
        return panel;
    }

    private CarvedRectangle amountBox() {
        CarvedRectangle box = SlimeUi.carved(86.0, 40.0, SlimeUi.SURFACE);
        box.addChild(SlimeUi.text(Integer.toString(selectedAmount), SlimeUi.BODY, SlimeUi.WHITE));
        return box;
    }

    private static CarvedRectangle rightValue(String value) {
        CarvedRectangle holder = SlimeUi.carved(120.0, 40.0, SlimeUi.CLEAR);
        holder.setOutlineColor(SlimeUi.CLEAR);
        holder.setOrigin(1.0, 0.5, 0.0);
        holder.setAlign(1.0, 0.5, 0.0);
        holder.setPosX(-SlimeUi.INSET);
        holder.addChild(SlimeUi.text(value, SlimeUi.LEAD, SlimeUi.GOLD));
        return holder;
    }

    // --- действия --------------------------------------------------------

    private void sell(String plortId, int amount) {
        new ModTransfer().writeString(plortId).writeInt(amount).send(SlimeHuntMod.SELL_TERMINAL_ACTION_CHANNEL);
        close();
    }

    private void reopen(String plortId, int amount) {
        new SellTerminalScreen(data, plortId, amount).open();
    }

    private PlortData findPlort(String id) {
        if (id == null) {
            return null;
        }
        for (PlortData plort : data.plorts) {
            if (id.equals(plort.id)) {
                return plort;
            }
        }
        return null;
    }

    private static String firstPlortId(MenuData data) {
        return data.plorts.isEmpty() ? null : data.plorts.get(0).id;
    }

    private static Color colorFor(String id) {
        int hash = id == null ? 0 : id.hashCode();
        return new Color(92 + Math.abs(hash) % 120,
                92 + Math.abs(hash / 13) % 120,
                92 + Math.abs(hash / 31) % 120, 1.0);
    }

    /** Читает пакет slimehunt:sellmenu. */
    static MenuData read(ModTransfer transfer) {
        long coins = Long.parseLong(transfer.readString());
        long totalValue = Long.parseLong(transfer.readString());
        int lastAmount = transfer.readInt();
        long lastCoins = Long.parseLong(transfer.readString());
        int count = transfer.readInt();
        List<PlortData> plorts = new ArrayList<>(Math.max(0, count));
        for (int index = 0; index < count; index++) {
            plorts.add(new PlortData(transfer.readString(), transfer.readString(),
                    transfer.readInt(), transfer.readInt(), transfer.readInt()));
        }
        return new MenuData(coins, totalValue, lastAmount, lastCoins, plorts);
    }

    static final class MenuData {
        private final long coins;
        private final long totalValue;
        private final int lastAmount;
        private final long lastCoins;
        private final List<PlortData> plorts;

        MenuData(long coins, long totalValue, int lastAmount, long lastCoins, List<PlortData> plorts) {
            this.coins = coins;
            this.totalValue = totalValue;
            this.lastAmount = lastAmount;
            this.lastCoins = lastCoins;
            this.plorts = plorts == null ? Collections.emptyList() : plorts;
        }
    }

    static final class PlortData {
        private final String id;
        private final String name;
        private final int price;
        private final int vacpackAmount;
        private final int storageAmount;

        PlortData(String id, String name, int price, int vacpackAmount, int storageAmount) {
            this.id = id;
            this.name = name;
            this.price = Math.max(0, price);
            this.vacpackAmount = Math.max(0, vacpackAmount);
            this.storageAmount = Math.max(0, storageAmount);
        }

        private int totalAmount() {
            return this.vacpackAmount + this.storageAmount;
        }
    }
}
