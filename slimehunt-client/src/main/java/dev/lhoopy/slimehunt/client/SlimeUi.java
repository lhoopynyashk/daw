package dev.lhoopy.slimehunt.client;

import dev.xdark.clientapi.render.font.Font;
import dev.xdark.clientapi.resource.ResourceLocation;
import gg.cristalix.enginex.Enginex;
import gg.cristalix.enginex.color.Color;
import gg.cristalix.enginex.color.palette.ButtonColor;
import gg.cristalix.enginex.color.palette.Palette;
import gg.cristalix.enginex.element.AbstractElement;
import gg.cristalix.enginex.element.Image;
import gg.cristalix.enginex.element.Text;
import gg.cristalix.enginex.element.button.Button;
import gg.cristalix.enginex.element.carved.CarvedRectangle;
import gg.cristalix.enginex.element.layout.LayoutPriority;
import gg.cristalix.enginex.element.layout.type.GridLayout;
import gg.cristalix.enginex.element.layout.type.HorizontalLayout;
import gg.cristalix.enginex.element.layout.type.VerticalLayout;
import gg.cristalix.enginex.element.scrollview.type.VerticalScrollView;
import gg.cristalix.enginex.event.HoverEvent;
import gg.cristalix.enginex.event.element.ButtonLeftActionEvent;
import gg.cristalix.enginex.event.input.MouseLeftClickEvent;
import gg.cristalix.enginex.math.Relative;

import java.util.HashMap;
import java.util.Map;

/**
 * Дизайн-система SlimeHunt: токены, каркас окна и фабрики элементов.
 * Позиции внутри панелей считает движок через layout-контейнеры,
 * руками задаются только размеры блоков.
 */
final class SlimeUi {

    // --- палитра ---------------------------------------------------------

    static final Color OVERLAY = new Color(0, 0, 0, 0.54);
    static final float BLUR = 3.4F;
    /** Длительность перехода подсветки, секунды. */
    static final double HOVER_FADE = 0.13;

    static final Color SURFACE = new Color(31, 32, 35, 0.86);
    static final Color CARD = new Color(42, 43, 46, 0.62);
    static final Color CARD_HOVER = new Color(58, 61, 67, 0.92);
    static final Color CARD_SELECTED = new Color(34, 58, 77, 0.96);
    static final Color BORDER = new Color(67, 69, 74, 1.0);
    static final Color BORDER_SOFT = new Color(67, 69, 74, 0.45);
    static final Color ACCENT = new Color(47, 108, 195, 1.0);
    static final Color ACCENT_HOVER = new Color(61, 132, 221, 1.0);
    static final Color DISABLED = new Color(72, 75, 81, 1.0);
    static final Color GREEN = new Color(77, 205, 116, 1.0);
    static final Color GREEN_HOVER = new Color(92, 224, 132, 1.0);
    static final Color GOLD = new Color(244, 184, 62, 1.0);
    static final Color MUTED = new Color(180, 184, 194, 0.96);
    static final Color RED = new Color(194, 49, 64, 1.0);
    static final Color RED_HOVER = new Color(232, 62, 78, 1.0);
    static final Color WHITE = Palette.WHITE;
    /** Прозрачный: для layout-контейнеров, которые только раскладывают. */
    static final Color CLEAR = new Color(0, 0, 0, 0.0);

    // --- типографика -----------------------------------------------------

    /**
     * Шрифт задаётся здесь один раз. Доступны также MINECRAFT_TEN, MINECRAFT_TEN_V2,
     * MINECRAFT_FIVE и COMFORTAA — менять только тут, чтобы не разъехалось по экранам.
     */
    static final String FONT = Font.DEFAULT;

    /**
     * Пять ступеней, каждая кратна пиксельной сетке шрифта (высота глифа 8 px):
     * 10 / 8 / 7 / 6 / 5 px. Соседние ступени отличаются примерно в 1.2 раза,
     * поэтому размеры читаются как одна система, а не как случайные числа.
     * Больше ступеней не заводить — именно от этого текст выглядел разным.
     */
    static final double TITLE = 1.25;
    static final double HEADER = 1.00;
    static final double LEAD = 0.875;
    static final double BODY = 0.75;
    static final double CAPTION = 0.625;

    /** Декоративные глифы вроде «?» и «◆» — не текст, своя шкала. */
    static final double GLYPH = 1.60;
    static final double GLYPH_LARGE = 2.40;

    // --- сетка окна ------------------------------------------------------

    static final double WINDOW_WIDTH = 1411.0;
    static final double HEADER_HEIGHT = 56.0;
    static final double BODY_HEIGHT = 560.0;
    static final double FOOTER_HEIGHT = 86.0;
    /** Тело окна без футера занимает ту же площадь, что тело плюс футер. */
    static final double TALL_BODY_HEIGHT = BODY_HEIGHT + 16.0 + FOOTER_HEIGHT;
    /** Отступ между крупными блоками. */
    static final double GAP = 16.0;
    /** Внутренние поля панели. */
    static final double INSET = 24.0;

    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private SlimeUi() {
    }

    // --- каркас окна -----------------------------------------------------

    /**
     * Корень окна: вертикальная колонка из шапки, тела и футера с общим ритмом.
     * Высоту считает движок, поэтому блоки всегда стоят на одинаковом расстоянии.
     */
    static VerticalLayout window() {
        VerticalLayout window = new VerticalLayout(GAP);
        window.setOriginAndAlign(Relative.CENTER);
        window.setColor(CLEAR);
        window.setOutlineColor(CLEAR);
        window.setShadowSize(0.0F);
        return window;
    }

    /** Шапка окна: заголовок слева, произвольная подпись и крестик справа. */
    static CarvedRectangle header(String title, String note, Color noteColor, Runnable onClose) {
        CarvedRectangle header = panel(WINDOW_WIDTH, HEADER_HEIGHT);

        Text caption = leftText(title, HEADER, WHITE);
        caption.setOriginAndAlign(Relative.LEFT);
        caption.setPosX(INSET);
        header.addChild(caption);

        if (note != null) {
            Text hint = text(note, BODY, noteColor);
            hint.setOrigin(1.0, 0.5, 0.0);
            hint.setAlign(1.0, 0.5, 0.0);
            hint.setPosX(-(INSET + 48.0));
            header.addChild(hint);
        }

        Image close = new Image();
        close.setSize(28.0, 28.0, 0.0);
        close.setOrigin(1.0, 0.5, 0.0);
        close.setAlign(1.0, 0.5, 0.0);
        close.setPosX(-INSET);
        close.setPosZ(5.0);
        close.setTexture(closeTexture());
        close.setSkipRenderIfTextureNotLoaded(true);
        close.setInteractive(true);
        close.registerEvent(HoverEvent.class,
                event -> close.setTexture(event.isHover() ? closeHoverTexture() : closeTexture()));
        close.registerEvent(MouseLeftClickEvent.class, event -> onClose.run());
        header.addChild(close);

        return header;
    }

    /** Ряд колонок тела окна: ширины задаются, промежутки держит движок. */
    static HorizontalLayout bodyRow() {
        HorizontalLayout row = new HorizontalLayout(GAP);
        row.setColor(CLEAR);
        row.setOutlineColor(CLEAR);
        row.setShadowSize(0.0F);
        row.setOriginAndAlign(Relative.CENTER);
        return row;
    }

    // --- панели и карточки -----------------------------------------------

    static CarvedRectangle panel(double width, double height) {
        return carved(width, height, SURFACE);
    }

    static CarvedRectangle card(double width, double height) {
        return carved(width, height, CARD);
    }

    static CarvedRectangle carved(double width, double height, Color color) {
        CarvedRectangle element = new CarvedRectangle(4.0, 2.0);
        element.setSize(width, height, 0.0);
        element.setOriginAndAlign(Relative.CENTER);
        element.setColor(color);
        element.setOutlineColor(BORDER);
        element.setShadowSize(0.0F);
        return element;
    }

    /**
     * Панель с заголовком секции и вертикальной колонкой под содержимое.
     * Возвращает колонку — в неё складывается контент, панель берётся через getLastParent().
     */
    static VerticalLayout section(CarvedRectangle panel, String title, String subtitle) {
        VerticalLayout column = new VerticalLayout(GAP);
        column.setColor(CLEAR);
        column.setOutlineColor(CLEAR);
        column.setShadowSize(0.0F);
        column.setOriginAndAlign(Relative.TOP);
        column.setPosY(INSET);

        if (title != null) {
            VerticalLayout heading = new VerticalLayout(6.0);
            heading.setColor(CLEAR);
            heading.setOutlineColor(CLEAR);
            heading.setShadowSize(0.0F);
            heading.setChildOrigin(Relative.LEFT);
            heading.addChild(leftText(title, TITLE, WHITE));
            if (subtitle != null) {
                heading.addChild(leftText(subtitle, BODY, MUTED));
            }
            column.addChild(heading);
        }

        panel.addChild(column);
        return column;
    }

    // --- раскладка -------------------------------------------------------

    static VerticalLayout column(double spacing) {
        VerticalLayout layout = new VerticalLayout(spacing);
        layout.setColor(CLEAR);
        layout.setOutlineColor(CLEAR);
        layout.setShadowSize(0.0F);
        layout.setOriginAndAlign(Relative.CENTER);
        return layout;
    }

    static HorizontalLayout row(double spacing) {
        HorizontalLayout layout = new HorizontalLayout(spacing);
        layout.setColor(CLEAR);
        layout.setOutlineColor(CLEAR);
        layout.setShadowSize(0.0F);
        layout.setOriginAndAlign(Relative.CENTER);
        return layout;
    }

    static GridLayout grid(int columns, int rows, double spacing) {
        GridLayout layout = new GridLayout(LayoutPriority.HORIZONTAL, rows, columns, spacing, spacing);
        layout.setColor(CLEAR);
        layout.setOutlineColor(CLEAR);
        layout.setShadowSize(0.0F);
        layout.setOriginAndAlign(Relative.CENTER);
        return layout;
    }

    /** Прокручиваемый список: заменяет ручную пагинацию. */
    static VerticalScrollView<VerticalLayout> scroll(double width, double height, double spacing) {
        VerticalScrollView<VerticalLayout> view = new VerticalScrollView<>(VerticalLayout.class);
        view.setSize(width, height, 0.0);
        view.setOriginAndAlign(Relative.CENTER);
        view.setColor(CLEAR);
        view.setEnableScissor(true);
        view.setScrollStep(40);
        view.getLayout().setSpacing(spacing);
        return view;
    }

    // --- кнопки ----------------------------------------------------------

    /** Кнопка Enginex: состояния, ripple и звук клика уже внутри. */
    static Button button(String title, double width, double height, ButtonColor color, Runnable action) {
        Button button = new Button(title, color);
        button.setSize(width, height, 0.0);
        button.setOriginAndAlign(Relative.CENTER);
        if (action == null) {
            button.setActiveInstant(false);
        } else {
            button.registerEvent(ButtonLeftActionEvent.class, event -> action.run());
        }
        return button;
    }

    static Button accentButton(String title, double width, double height, Runnable action) {
        return button(title, width, height, ButtonColor.BLUE, action);
    }

    // --- текст -----------------------------------------------------------

    static Text text(String value, double scale, Color color) {
        Text text = new Text(value);
        text.setFont(FONT);
        text.setPosZ(3.0);
        text.setOriginAndAlign(Relative.CENTER);
        text.setScale(scale);
        text.setColor(color);
        text.setShadow(false);
        return text;
    }

    static Text leftText(String value, double scale, Color color) {
        Text text = text(value, scale, color);
        text.setOriginAndAlign(Relative.LEFT);
        return text;
    }

    // --- текст по ролям --------------------------------------------------
    // Экран не выбирает кегль сам: он называет роль, а размер решает система.

    /** Имя сущности в карточке: слайм, плорт, рецепт, стиль, семя. */
    static Text cardTitle(String value) {
        return leftText(value, LEAD, WHITE);
    }

    /** Вторичная строка карточки: количество, цена, время. */
    static Text cardLine(String value, Color color) {
        return leftText(value, BODY, color);
    }

    /** Мелкая подпись: редкость, лейбл над значением. */
    static Text caption(String value, Color color) {
        return leftText(value, CAPTION, color);
    }

    /** Крупное число, на которое смотрят в первую очередь. */
    static Text value(String text, Color color) {
        return text(text, HEADER, color);
    }

    static Image image(double width, double height, ResourceLocation texture) {
        Image image = new Image();
        image.setSize(width, height, 0.0);
        image.setOriginAndAlign(Relative.CENTER);
        image.setPosZ(2.0);
        image.setTexture(texture);
        image.setColor(WHITE);
        image.setSkipRenderIfTextureNotLoaded(true);
        return image;
    }

    // --- события ---------------------------------------------------------

    /**
     * Клик вешается только на сам элемент. Подписи и иконки внутри неинтерактивны,
     * поэтому движок отбрасывает их при поиске цели и нажатие всё равно доходит сюда.
     * Делать детей интерактивными нельзя: в пределах одного hoverLayer побеждает
     * самый глубокий элемент, и при наведении на подпись карточка теряет подсветку.
     */
    static void click(AbstractElement<?> element, Runnable action) {
        element.setInteractive(true);
        element.registerEvent(MouseLeftClickEvent.class, event -> action.run());
    }

    /** Подсветка меняется анимацией, а не мгновенной сменой цвета — иначе дёргается. */
    static void hover(CarvedRectangle element, Color normal, Color hovered) {
        element.setInteractive(true);
        element.registerEvent(HoverEvent.class,
                event -> element.smoothChangeColor(event.isHover() ? hovered : normal, HOVER_FADE));
    }

    /** Кликабельная карточка: подсветка плюс действие. */
    static void interactive(CarvedRectangle card, Runnable action) {
        hover(card, CARD, CARD_HOVER);
        click(card, action);
    }

    // --- текстуры --------------------------------------------------------

    static ResourceLocation texture(String name, String path) {
        ResourceLocation cached = TEXTURES.get(path);
        if (cached != null) {
            return cached;
        }
        ResourceLocation loaded = Enginex.getTextureManager().loadTextureFromJar("slimehunt", name, path);
        TEXTURES.put(path, loaded);
        return loaded;
    }

    static ResourceLocation closeTexture() {
        return texture("textures/gui/close", "assets/slimehunt/textures/gui/close.png");
    }

    static ResourceLocation closeHoverTexture() {
        return texture("textures/gui/close_hover", "assets/slimehunt/textures/gui/close_hover.png");
    }

    static ResourceLocation slimeTexture() {
        return texture("textures/slimes/pink", "assets/slimehunt/textures/slimes/pink.png");
    }

    // --- редкости --------------------------------------------------------

    static Color rarityColor(String rarity) {
        if ("mythic".equals(rarity)) return new Color(242, 70, 112, 1.0);
        if ("legendary".equals(rarity)) return GOLD;
        if ("epic".equals(rarity)) return new Color(192, 93, 240, 1.0);
        if ("rare".equals(rarity)) return new Color(70, 150, 245, 1.0);
        return new Color(205, 208, 214, 1.0);
    }

    static Color raritySurface(String rarity) {
        if ("mythic".equals(rarity)) return new Color(76, 28, 48, 0.96);
        if ("legendary".equals(rarity)) return new Color(70, 53, 23, 0.96);
        if ("epic".equals(rarity)) return new Color(54, 31, 70, 0.96);
        if ("rare".equals(rarity)) return new Color(27, 46, 72, 0.96);
        return CARD;
    }

    static String rarityTitle(String rarity) {
        if ("mythic".equals(rarity)) return "МИФИЧЕСКИЙ";
        if ("legendary".equals(rarity)) return "ЛЕГЕНДАРНЫЙ";
        if ("epic".equals(rarity)) return "ЭПИЧЕСКИЙ";
        if ("rare".equals(rarity)) return "РЕДКИЙ";
        return "ОБЫЧНЫЙ";
    }

    // --- форматирование --------------------------------------------------

    static String shorten(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 3)) + "...";
    }

    static String formatTime(int seconds) {
        if (seconds < 60) return seconds + " сек";
        int minutes = seconds / 60;
        return minutes < 60 ? minutes + " мин" : (minutes / 60) + " ч " + (minutes % 60) + " мин";
    }

    /** Переводит значение, переданное в сотых долях, в строку вида «1.25». */
    static String formatHundredths(int hundredths) {
        int whole = hundredths / 100;
        int fraction = Math.abs(hundredths % 100);
        if (fraction == 0) {
            return Integer.toString(whole);
        }
        if (fraction % 10 == 0) {
            return whole + "." + (fraction / 10);
        }
        return whole + "." + (fraction < 10 ? "0" + fraction : Integer.toString(fraction));
    }
}
