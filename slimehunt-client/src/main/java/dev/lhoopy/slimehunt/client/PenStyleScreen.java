package dev.lhoopy.slimehunt.client;

import gg.cristalix.enginex.color.palette.ButtonColor;
import gg.cristalix.enginex.element.carved.CarvedRectangle;
import gg.cristalix.enginex.element.layout.type.GridLayout;
import gg.cristalix.enginex.element.layout.type.VerticalLayout;
import gg.cristalix.enginex.element.scrollview.type.VerticalScrollView;
import gg.cristalix.enginex.element.screen.type.GuiScreen;
import gg.cristalix.enginex.math.Relative;
import gg.cristalix.enginex.transfer.ModTransfer;

import java.util.ArrayList;
import java.util.List;

final class PenStyleScreen extends GuiScreen {
    private static final double CARD_WIDTH = 440.0;
    private static final double CARD_HEIGHT = 250.0;

    private final String activeId;
    private final List<StyleData> styles;

    PenStyleScreen(MenuData data) {
        this.activeId = data.activeId;
        this.styles = data.styles;

        setSize(1920, 1080, 0);
        setColor(SlimeUi.OVERLAY);
        setBlur(SlimeUi.BLUR);

        VerticalLayout window = SlimeUi.window();
        window.addChild(
                SlimeUi.header("Стили загона", "Получено: " + styles.size(), SlimeUi.MUTED, this::close),
                buildBody()
        );
        addChild(window);
    }

    private CarvedRectangle buildBody() {
        CarvedRectangle panel = SlimeUi.panel(SlimeUi.WINDOW_WIDTH, SlimeUi.TALL_BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Чертежи загона",
                "Стиль меняет вместимость загона, скорость производства и расход корма");
        column.setChildOrigin(Relative.LEFT);

        double content = SlimeUi.WINDOW_WIDTH - SlimeUi.INSET * 2;
        VerticalScrollView<VerticalLayout> scroll = SlimeUi.scroll(content, 550.0, SlimeUi.GAP);

        int rows = Math.max(1, (styles.size() + 2) / 3);
        GridLayout grid = SlimeUi.grid(3, rows, SlimeUi.GAP);
        for (StyleData style : styles) {
            grid.addChild(styleCard(style));
        }
        scroll.getLayout().addChild(grid);

        column.addChild(scroll);
        return panel;
    }

    private CarvedRectangle styleCard(StyleData style) {
        boolean active = style.id.equals(activeId);
        CarvedRectangle card = SlimeUi.carved(CARD_WIDTH, CARD_HEIGHT,
                active ? SlimeUi.CARD_SELECTED : SlimeUi.CARD);
        card.setOutlineColor(active ? SlimeUi.GREEN : SlimeUi.rarityColor(style.rarity));

        VerticalLayout labels = SlimeUi.column(8.0);
        labels.setChildOrigin(Relative.LEFT);
        labels.setOriginAndAlign(Relative.TOP);
        labels.setPosY(SlimeUi.INSET);
        labels.addChild(
                SlimeUi.leftText(SlimeUi.shorten(style.name, 26), SlimeUi.LEAD, SlimeUi.rarityColor(style.rarity)),
                SlimeUi.leftText(SlimeUi.rarityTitle(style.rarity), SlimeUi.CAPTION,
                        SlimeUi.rarityColor(style.rarity)),
                SlimeUi.leftText(SlimeUi.shorten(style.description, 46), SlimeUi.BODY, SlimeUi.WHITE),
                SlimeUi.leftText("Чертежи: " + style.count, SlimeUi.BODY, SlimeUi.MUTED)
        );
        card.addChild(labels);

        CarvedRectangle holder = SlimeUi.carved(CARD_WIDTH - SlimeUi.INSET * 2, 48.0, SlimeUi.CLEAR);
        holder.setOutlineColor(SlimeUi.CLEAR);
        holder.setOriginAndAlign(Relative.BOTTOM);
        holder.setPosY(-SlimeUi.INSET);
        holder.addChild(active
                ? SlimeUi.button("Выбран", CARD_WIDTH - SlimeUi.INSET * 2, 48.0, ButtonColor.GREEN, null)
                : SlimeUi.button("Использовать", CARD_WIDTH - SlimeUi.INSET * 2, 48.0, ButtonColor.BLUE,
                        () -> select(style.id)));
        card.addChild(holder);

        if (!active) {
            SlimeUi.hover(card, SlimeUi.CARD, SlimeUi.CARD_HOVER);
        }
        return card;
    }

    private void select(String styleId) {
        new ModTransfer().writeString(styleId).send(SlimeHuntMod.PEN_STYLE_SELECT_CHANNEL);
    }

    /** Читает пакет slimehunt:pstyles. */
    static MenuData read(ModTransfer transfer) {
        String activeId = transfer.readString();
        int count = transfer.readInt();
        List<StyleData> styles = new ArrayList<>(Math.max(0, count));
        for (int index = 0; index < count; index++) {
            styles.add(new StyleData(transfer.readString(), transfer.readString(),
                    transfer.readString(), transfer.readString(), transfer.readInt()));
        }
        return new MenuData(activeId, styles);
    }

    static final class MenuData {
        final String activeId;
        final List<StyleData> styles;

        MenuData(String activeId, List<StyleData> styles) {
            this.activeId = activeId;
            this.styles = styles;
        }
    }

    static final class StyleData {
        final String id;
        final String name;
        final String rarity;
        final String description;
        final int count;

        StyleData(String id, String name, String rarity, String description, int count) {
            this.id = id;
            this.name = name;
            this.rarity = rarity;
            this.description = description;
            this.count = count;
        }
    }
}
