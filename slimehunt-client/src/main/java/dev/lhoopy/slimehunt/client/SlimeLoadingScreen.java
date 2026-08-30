package dev.lhoopy.slimehunt.client;

import dev.xdark.clientapi.resource.ResourceLocation;
import gg.cristalix.enginex.Enginex;
import gg.cristalix.enginex.color.Color;
import gg.cristalix.enginex.color.palette.Palette;
import gg.cristalix.enginex.element.Container;
import gg.cristalix.enginex.element.Image;
import gg.cristalix.enginex.element.Rectangle;
import gg.cristalix.enginex.element.Text;
import gg.cristalix.enginex.render.context.type.PostOverlay;
import gg.cristalix.enginex.timer.task.ScheduledTask;

final class SlimeLoadingScreen {
    private static final long MIN_VISIBLE_MILLIS = 3000L;

    private static SlimeLoadingScreen current;
    private static boolean keepOpen;
    private static boolean closeRequested;
    private static String lastStatusText = "Загрузка профиля";
    private static int lastProgress = 10;
    private static ScheduledTask keepAliveTask;
    private static ScheduledTask delayedCloseTask;

    /** Фон приезжает по URL, поэтому кэшируется между открытиями экрана. */
    private static ResourceLocation art;
    private static String artUrl;
    private static boolean artRequested;

    private final Container root = new Container();
    private final Image background = new Image();
    private final Text loadingText = new Text();
    private final Rectangle progressFill = new Rectangle();
    private ScheduledTask progressTask;
    private boolean open;
    private long openedAtMillis;
    private int tick;
    private String statusText = "Загрузка профиля";
    private int targetProgress = 10;
    private double visibleProgress;

    SlimeLoadingScreen() {
        root.setSize(1920, 1080, 0);

        Rectangle base = new Rectangle();
        base.setSize(1920, 1080, 0);
        base.setColor(new Color(5, 8, 14, 1.0));

        background.setSize(1920, 1080, 0);
        background.setColor(Palette.WHITE);
        background.setSkipRenderIfTextureNotLoaded(true);
        if (art != null) {
            background.setTexture(art);
        }

        Rectangle shade = new Rectangle();
        shade.setSize(1920, 1080, 0);
        shade.setColor(new Color(0, 0, 0, 0.18));

        Rectangle bottomShade = new Rectangle();
        bottomShade.setSize(1920, 260, 0);
        bottomShade.setPos(0, 820, 2);
        bottomShade.setColor(new Color(0, 0, 0, 0.58));

        Text title = new Text("SlimeRancher");
        title.setColor(Palette.PINK_LIGHT);
        title.setShadow(true);
        title.setScale(3.0D);
        title.setOrigin(0.0D, 0.5D, 0.0D);
        title.setPos(90, 890, 4);

        Text subtitle = new Text("Загрузка ранчо слаймов");
        subtitle.setColor(Palette.WHITE_86);
        subtitle.setShadow(true);
        subtitle.setScale(1.45D);
        subtitle.setOrigin(0.0D, 0.5D, 0.0D);
        subtitle.setPos(92, 946, 4);

        loadingText.setValue(statusText + "...");
        loadingText.setColor(Palette.WHITE_62);
        loadingText.setShadow(true);
        loadingText.setScale(1.0D);
        loadingText.setOrigin(1.0D, 0.5D, 0.0D);
        loadingText.setPos(1818, 946, 4);

        Container progress = new Container();
        progress.setSize(520, 18, 0);
        progress.setPos(1308, 974, 4);

        Rectangle progressBack = new Rectangle();
        progressBack.setSize(520, 18, 0);
        progressBack.setColor(new Color(255, 255, 255, 0.18));

        progressFill.setSize(1, 18, 0);
        progressFill.setColor(Palette.PINK_LIGHT);

        progress.addChild(progressBack, progressFill);
        root.addChild(base, background, shade, bottomShade, title, subtitle, loadingText, progress);
    }

    public void open() {
        current = this;
        if (!open) {
            PostOverlay.get().setIgnoreHideGuiFlag(true);
            PostOverlay.add(root);
            open = true;
            openedAtMillis = System.currentTimeMillis();
        }
        startAnimation();
    }

    public void close() {
        if (current == this) {
            current = null;
        }
        cancelTasks();
        if (open) {
            PostOverlay.remove(root);
            open = false;
        }
    }

    /**
     * Картинка фона живёт не в бандле, а на URL: так мод весит меньше и быстрее
     * доезжает до клиента, а сам экран показывается сразу, не дожидаясь арта.
     */
    static void requestArt(String url) {
        if (url == null || url.isEmpty() || url.equals(artUrl)) {
            return;
        }
        artUrl = url;
        if (artRequested) {
            return;
        }
        artRequested = true;
        Enginex.log("SlimeHunt loading art: requesting " + url);
        Enginex.getTextureManager()
                .loadStaticTextureFromUrl("textures/loading/art", url)
                .thenAccept(location -> Enginex.getMinecraft().execute(() -> {
                    art = location;
                    Enginex.log("SlimeHunt loading art: ready");
                    if (current != null) {
                        current.background.setTexture(location);
                    }
                }))
                .exceptionally(error -> {
                    // Не удалось скачать фон — экран просто останется тёмным.
                    artRequested = false;
                    Enginex.log("SlimeHunt loading art: FAILED " + error);
                    return null;
                });
    }

    static void openOrUpdate(String statusText, int progressPercent) {
        cancelDelayedClose();
        keepOpen = true;
        closeRequested = false;
        lastStatusText = statusText;
        lastProgress = Math.max(0, Math.min(100, progressPercent));
        if (current == null) {
            current = new SlimeLoadingScreen();
            current.updateStatus(statusText, progressPercent);
            current.open();
            startKeepAlive();
            return;
        }
        current.updateStatus(statusText, progressPercent);
        startKeepAlive();
    }

    static void updateCurrent(String statusText, int progressPercent) {
        cancelDelayedClose();
        keepOpen = true;
        closeRequested = false;
        lastStatusText = statusText;
        lastProgress = Math.max(0, Math.min(100, progressPercent));
        if (current == null) {
            openOrUpdate(statusText, progressPercent);
            return;
        }
        current.updateStatus(statusText, progressPercent);
    }

    static void closeCurrent() {
        if (current != null) {
            long visibleMillis = System.currentTimeMillis() - current.openedAtMillis;
            long remainingMillis = MIN_VISIBLE_MILLIS - visibleMillis;
            if (remainingMillis > 0L) {
                current.updateStatus("Готово", 100);
                scheduleDelayedClose(remainingMillis);
                return;
            }
        }
        closeNow();
    }

    private static void closeNow() {
        keepOpen = false;
        closeRequested = true;
        cancelDelayedClose();
        stopKeepAlive();
        if (current != null) {
            current.close();
        }
    }

    private void startAnimation() {
        cancelTasks();
        progressTask = Enginex.getTimerManager().every(0.05D, task -> {
            tick++;
            double target = Math.max(targetProgress / 100.0D, Math.min(0.94D, visibleProgress + 0.0015D));
            double step = Math.max(0.006D, (target - visibleProgress) * 0.18D);
            visibleProgress = Math.min(target, visibleProgress + step);
            progressFill.setSize(520.0D * visibleProgress, 18, 0);
            loadingText.setValue(statusText + dots(tick));
        });
    }

    private void cancelTasks() {
        if (progressTask != null) {
            progressTask.cancel();
            progressTask = null;
        }
    }

    private void updateStatus(String statusText, int progressPercent) {
        this.statusText = statusText;
        this.targetProgress = Math.max(0, Math.min(100, progressPercent));
        this.progressFill.setSize(520.0D * this.visibleProgress, 18, 0);
        this.loadingText.setValue(this.statusText + dots(tick));
    }

    private static void startKeepAlive() {
        if (keepAliveTask != null) {
            return;
        }
        keepAliveTask = Enginex.getTimerManager().every(0.03D, task -> {
            if (!keepOpen || closeRequested) {
                stopKeepAlive();
                return;
            }
            if (current == null || !current.open) {
                SlimeLoadingScreen replacement = new SlimeLoadingScreen();
                replacement.updateStatus(lastStatusText, lastProgress);
                replacement.open();
            }
        });
    }

    private static void stopKeepAlive() {
        if (keepAliveTask != null) {
            keepAliveTask.cancel();
            keepAliveTask = null;
        }
    }

    private static void scheduleDelayedClose(long remainingMillis) {
        if (delayedCloseTask != null) {
            return;
        }
        delayedCloseTask = Enginex.getTimerManager().after(Math.max(0.05D, remainingMillis / 1000.0D), task -> closeNow());
    }

    private static void cancelDelayedClose() {
        if (delayedCloseTask != null) {
            delayedCloseTask.cancel();
            delayedCloseTask = null;
        }
    }

    private static String dots(int tick) {
        int count = tick % 4;
        if (count == 0) {
            return "";
        }
        if (count == 1) {
            return ".";
        }
        if (count == 2) {
            return "..";
        }
        return "...";
    }
}
