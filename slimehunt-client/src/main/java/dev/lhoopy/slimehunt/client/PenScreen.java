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
import java.util.List;

final class PenScreen extends GuiScreen {
    private static final double PEN_WIDTH = 720.0;
    private static final double CAPTURED_WIDTH = 675.0;
    private static final double SLOT = 208.0;
    private static final double LIST_HEIGHT = 450.0;

    private final MenuData data;

    PenScreen(MenuData data) {
        this.data = data;

        setSize(1920, 1080, 0);
        setColor(SlimeUi.OVERLAY);
        setBlur(SlimeUi.BLUR);

        boolean full = data.stored.size() >= data.capacity;
        VerticalLayout window = SlimeUi.window();
        window.addChild(
                SlimeUi.header("Загон слаймов",
                        data.stored.size() + " / " + data.capacity,
                        full ? SlimeUi.GOLD : SlimeUi.GREEN,
                        this::close),
                buildBody(),
                buildFooter()
        );
        addChild(window);
    }

    // --- тело ------------------------------------------------------------

    private HorizontalLayout buildBody() {
        HorizontalLayout body = SlimeUi.bodyRow();
        body.addChild(buildPenPanel(), buildCapturedPanel());
        return body;
    }

    private CarvedRectangle buildPenPanel() {
        CarvedRectangle panel = SlimeUi.panel(PEN_WIDTH, SlimeUi.BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "В загоне", "Слаймы, которые живут в загоне");
        column.setChildOrigin(Relative.LEFT);

        double content = PEN_WIDTH - SlimeUi.INSET * 2;
        VerticalScrollView<VerticalLayout> scroll = SlimeUi.scroll(content, LIST_HEIGHT, SlimeUi.GAP);

        int rows = Math.max(1, (data.capacity + 2) / 3);
        GridLayout grid = SlimeUi.grid(3, rows, SlimeUi.GAP);
        for (int index = 0; index < data.capacity; index++) {
            grid.addChild(index < data.stored.size()
                    ? filledSlot(data.stored.get(index), index)
                    : emptySlot());
        }
        scroll.getLayout().addChild(grid);

        column.addChild(scroll);
        return panel;
    }

    private CarvedRectangle filledSlot(SlimeEntry slime, int index) {
        CarvedRectangle slot = SlimeUi.card(SLOT, 184.0);
        slot.setTooltip("Нажми, чтобы убрать слайма из загона");

        VerticalLayout content = SlimeUi.column(8.0);
        content.addChild(
                SlimeUi.image(72.0, 72.0, SlimeUi.slimeTexture()),
                SlimeUi.text(SlimeUi.shorten(slime.name, 18), SlimeUi.LEAD, SlimeUi.WHITE),
                SlimeUi.text(SlimeUi.rarityTitle(slime.rarity), SlimeUi.CAPTION, SlimeUi.rarityColor(slime.rarity))
        );
        slot.addChild(content);
        slot.setOutlineColor(SlimeUi.rarityColor(slime.rarity));
        SlimeUi.interactive(slot, () -> removeFromPen(index));
        return slot;
    }

    private static CarvedRectangle emptySlot() {
        CarvedRectangle slot = SlimeUi.carved(SLOT, 184.0, SlimeUi.CARD);
        slot.setOutlineColor(SlimeUi.BORDER_SOFT);

        VerticalLayout content = SlimeUi.column(4.0);
        content.addChild(
                SlimeUi.text("Свободное", SlimeUi.BODY, SlimeUi.MUTED),
                SlimeUi.text("место", SlimeUi.BODY, SlimeUi.MUTED)
        );
        slot.addChild(content);
        return slot;
    }

    private CarvedRectangle buildCapturedPanel() {
        CarvedRectangle panel = SlimeUi.panel(CAPTURED_WIDTH, SlimeUi.BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Поймано",
                "Слаймы в вакпаке, доступные для заселения");
        column.setChildOrigin(Relative.LEFT);

        double content = CAPTURED_WIDTH - SlimeUi.INSET * 2;

        if (data.captured.isEmpty()) {
            VerticalLayout empty = SlimeUi.column(10.0);
            empty.setPosY(LIST_HEIGHT / 2.0 - 70.0);
            empty.addChild(
                    SlimeUi.image(86.0, 86.0, SlimeUi.slimeTexture()),
                    SlimeUi.text("Инвентарь пуст", SlimeUi.LEAD, SlimeUi.WHITE),
                    SlimeUi.text("Поймайте слайма, чтобы поместить его в загон",
                            SlimeUi.BODY, SlimeUi.MUTED)
            );
            CarvedRectangle holder = SlimeUi.carved(content, LIST_HEIGHT, SlimeUi.CLEAR);
            holder.setOutlineColor(SlimeUi.CLEAR);
            holder.addChild(empty);
            column.addChild(holder);
            return panel;
        }

        boolean full = data.stored.size() >= data.capacity;
        VerticalScrollView<VerticalLayout> scroll = SlimeUi.scroll(content, LIST_HEIGHT, 12.0);
        for (SlimeEntry slime : data.captured) {
            scroll.getLayout().addChild(capturedRow(slime, content, full));
        }
        column.addChild(scroll);
        return panel;
    }

    private CarvedRectangle capturedRow(SlimeEntry slime, double width, boolean penFull) {
        CarvedRectangle row = SlimeUi.card(width, 94.0);
        row.setOutlineColor(SlimeUi.rarityColor(slime.rarity));

        HorizontalLayout line = SlimeUi.row(14.0);
        line.setPosX(SlimeUi.INSET);
        line.setOriginAndAlign(Relative.LEFT);
        line.addChild(SlimeUi.image(58.0, 58.0, SlimeUi.slimeTexture()));

        VerticalLayout labels = SlimeUi.column(6.0);
        labels.setChildOrigin(Relative.LEFT);
        labels.addChild(
                SlimeUi.cardTitle(SlimeUi.shorten(slime.name, 26)),
                SlimeUi.leftText(SlimeUi.rarityTitle(slime.rarity), SlimeUi.CAPTION,
                        SlimeUi.rarityColor(slime.rarity))
        );
        line.addChild(labels);
        row.addChild(line);

        if (penFull) {
            row.setTooltip("Загон заполнен");
            row.addChild(placeButton("Занято", null));
        } else {
            row.addChild(placeButton("В загон", () -> moveToPen(slime)));
            SlimeUi.hover(row, SlimeUi.CARD, SlimeUi.CARD_HOVER);
        }
        return row;
    }

    private static CarvedRectangle placeButton(String title, Runnable action) {
        CarvedRectangle holder = SlimeUi.carved(0.0, 0.0, SlimeUi.CLEAR);
        holder.setOutlineColor(SlimeUi.CLEAR);
        holder.setSize(140.0, 46.0, 0.0);
        holder.setOrigin(1.0, 0.5, 0.0);
        holder.setAlign(1.0, 0.5, 0.0);
        holder.setPosX(-SlimeUi.INSET);
        holder.addChild(SlimeUi.button(title, 140.0, 46.0,
                action == null ? ButtonColor.GRAY : ButtonColor.BLUE, action));
        return holder;
    }

    // --- футер -----------------------------------------------------------

    private CarvedRectangle buildFooter() {
        CarvedRectangle footer = SlimeUi.panel(SlimeUi.WINDOW_WIDTH, SlimeUi.FOOTER_HEIGHT);

        HorizontalLayout row = SlimeUi.row(SlimeUi.GAP);
        row.addChild(
                SlimeUi.button("Улучшение загона", 242.0, 56.0, ButtonColor.BLUE,
                        () -> new ModTransfer().send(SlimeHuntMod.PEN_CASE_REQUEST_CHANNEL)),
                SlimeUi.button("Смена стиля", 242.0, 56.0, ButtonColor.BLUE,
                        () -> new ModTransfer().send(SlimeHuntMod.PEN_STYLE_REQUEST_CHANNEL)),
                stat("Производство",
                        SlimeUi.formatHundredths(data.productionPerMinuteX100) + " в мин",
                        data.productionPerMinuteX100 > 0 ? SlimeUi.GREEN : SlimeUi.MUTED),
                stat("Корм в запасе", Integer.toString(data.foodCount),
                        data.foodCount > 0 ? SlimeUi.WHITE : SlimeUi.GOLD),
                stat("Плорты в загоне", data.plortStored + " / " + data.plortCapacity,
                        data.plortStored >= data.plortCapacity ? SlimeUi.GOLD : SlimeUi.WHITE)
        );
        footer.addChild(row);
        return footer;
    }

    private static CarvedRectangle stat(String title, String value, Color valueColor) {
        CarvedRectangle card = SlimeUi.card(242.0, 56.0);
        VerticalLayout content = SlimeUi.column(5.0);
        content.addChild(
                SlimeUi.text(title, SlimeUi.CAPTION, SlimeUi.MUTED),
                SlimeUi.text(value, SlimeUi.BODY, valueColor)
        );
        card.addChild(content);
        return card;
    }

    // --- действия --------------------------------------------------------

    private void moveToPen(SlimeEntry slime) {
        new ModTransfer().writeString(slime.id).send(SlimeHuntMod.PEN_MOVE_CHANNEL);
        close();
    }

    private void removeFromPen(int index) {
        new ModTransfer().writeInt(index).send(SlimeHuntMod.PEN_REMOVE_CHANNEL);
        close();
    }

    /** Читает пакет slimehunt:pen_open. */
    static MenuData read(ModTransfer transfer) {
        int capacity = transfer.readInt();
        List<SlimeEntry> stored = readSlimes(transfer);
        List<SlimeEntry> captured = readSlimes(transfer);
        int productionPerMinuteX100 = transfer.readInt();
        int foodCount = transfer.readInt();
        int plortStored = transfer.readInt();
        int plortCapacity = transfer.readInt();
        return new MenuData(capacity, stored, captured, productionPerMinuteX100, foodCount,
                plortStored, plortCapacity);
    }

    private static List<SlimeEntry> readSlimes(ModTransfer transfer) {
        int count = transfer.readInt();
        List<SlimeEntry> entries = new ArrayList<>(Math.max(0, count));
        for (int index = 0; index < count; index++) {
            entries.add(new SlimeEntry(transfer.readString(), transfer.readString(), transfer.readString()));
        }
        return entries;
    }

    static final class MenuData {
        final int capacity;
        final List<SlimeEntry> stored;
        final List<SlimeEntry> captured;
        final int productionPerMinuteX100;
        final int foodCount;
        final int plortStored;
        final int plortCapacity;

        MenuData(int capacity, List<SlimeEntry> stored, List<SlimeEntry> captured,
                 int productionPerMinuteX100, int foodCount, int plortStored, int plortCapacity) {
            this.capacity = Math.max(1, capacity);
            this.stored = stored;
            this.captured = captured;
            this.productionPerMinuteX100 = Math.max(0, productionPerMinuteX100);
            this.foodCount = Math.max(0, foodCount);
            this.plortStored = Math.max(0, plortStored);
            this.plortCapacity = Math.max(0, plortCapacity);
        }
    }

    static final class SlimeEntry {
        final String id;
        final String name;
        final String rarity;

        SlimeEntry(String id, String name, String rarity) {
            this.id = id;
            this.name = name;
            this.rarity = rarity;
        }
    }
}
