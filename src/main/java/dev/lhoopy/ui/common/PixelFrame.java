package dev.lhoopy.ui.common;

public final class PixelFrame {
    private final int border;
    private final int innerBorder;
    private final int cornerCut;

    public PixelFrame(int border, int innerBorder, int cornerCut) {
        this.border = border;
        this.innerBorder = innerBorder;
        this.cornerCut = cornerCut;
    }

    public static PixelFrame standard() {
        return new PixelFrame(2, 1, 3);
    }

    public int border() {
        return this.border;
    }

    public int innerBorder() {
        return this.innerBorder;
    }

    public int cornerCut() {
        return this.cornerCut;
    }
}
