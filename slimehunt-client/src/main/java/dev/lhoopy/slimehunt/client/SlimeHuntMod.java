package dev.lhoopy.slimehunt.client;

import gg.cristalix.enginex.Enginex;
import gg.cristalix.enginex.JavaMod;
import gg.cristalix.enginex.element.screen.type.GuiScreen;
import gg.cristalix.enginex.transfer.ModTransfer;

public final class SlimeHuntMod extends JavaMod {
    /** Đ”ĐµŃ€Đ¶Đ°Ń‚ŃŚ ŃĐ¸Đ˝Ń…Ń€ĐľĐ˝Đ˝Đľ Ń bundle.modVersion Đ˛ slimehunt-client/build.gradle. */
    static final String CLIENT_VERSION = "1.0.66";
    static final String START_CHANNEL = "slimehunt:start";
    static final String RESULT_CHANNEL = "slimehunt:result";
    static final String LOADED_CHANNEL = "slimehunt:loaded";
    static final String LOADING_OPEN_CHANNEL = "slimehunt:loading_open";
    static final String LOADING_STATUS_CHANNEL = "slimehunt:loading_status";
    static final String LOADING_CLOSE_CHANNEL = "slimehunt:loading_close";
    static final String PEN_OPEN_CHANNEL = "slimehunt:pen_open";
    static final String PEN_MOVE_CHANNEL = "slimehunt:pen_move";
    static final String PEN_REMOVE_CHANNEL = "slimehunt:pen_remove";
    static final String PEN_VISUAL_CHANNEL = "slimehunt:pen_visual";
    static final String FARMER_OPEN_CHANNEL = "slimehunt:farmer";
    static final String FARMER_CRAFT_CHANNEL = "slimehunt:fcraft";
    static final String PLOT_OPEN_CHANNEL = "slimehunt:plot";
    static final String PLOT_ACTION_CHANNEL = "slimehunt:plotact";
    static final String PEN_CASE_SCREEN_CHANNEL = "slimehunt:pcase";
    static final String PEN_CASE_OPEN_CHANNEL = "slimehunt:pcopen";
    static final String PEN_CASE_REQUEST_CHANNEL = "slimehunt:pcshow";
    static final String PEN_STYLE_SCREEN_CHANNEL = "slimehunt:pstyles";
    static final String PEN_STYLE_REQUEST_CHANNEL = "slimehunt:pshow";
    static final String PEN_STYLE_SELECT_CHANNEL = "slimehunt:pstyle";
    static final String SELL_TERMINAL_OPEN_CHANNEL = "slimehunt:sellmenu";
    static final String SELL_TERMINAL_ACTION_CHANNEL = "slimehunt:sellact";
    private static boolean initialized;

    @Override
    public void onLoad() {
        Enginex.log("SlimeHunt " + CLIENT_VERSION + " loading");
        if (initialized) {
            sendLoaded();
            return;
        }
        initialized = true;

        ModTransfer.registerChannel(LOADING_OPEN_CHANNEL, transfer -> {
            String message = transfer.readString();
            int progress = transfer.readInt();
            String artUrl = transfer.isReadable() ? transfer.readString() : "";
            onMainThread(() -> {
                SlimeLoadingScreen.requestArt(artUrl);
                SlimeLoadingScreen.openOrUpdate(message, progress);
            });
        });
        ModTransfer.registerChannel(LOADING_STATUS_CHANNEL, transfer -> {
            String message = transfer.readString();
            int progress = transfer.readInt();
            onMainThread(() -> SlimeLoadingScreen.updateCurrent(message, progress));
        });
        ModTransfer.registerChannel(LOADING_CLOSE_CHANNEL, transfer ->
                onMainThread(SlimeLoadingScreen::closeCurrent));

        ModTransfer.registerChannel(START_CHANNEL, transfer -> {
            int seed = transfer.readInt();
            int totalRounds = transfer.readInt();
            int requiredHits = transfer.readInt();
            int timeoutMillis = transfer.readInt();
            Enginex.log("SlimeHunt start received");
            onMainThread(() -> new SlimeHuntScreen(seed, totalRounds, requiredHits, timeoutMillis).open());
        });

        openScreen(PEN_OPEN_CHANNEL, PenScreen::read, PenScreen::new);
        openScreen(PEN_CASE_SCREEN_CHANNEL, PenCaseScreen::read, PenCaseScreen::new);
        openScreen(PEN_STYLE_SCREEN_CHANNEL, PenStyleScreen::read, PenStyleScreen::new);
        openScreen(FARMER_OPEN_CHANNEL, FarmerTableScreen::read, FarmerTableScreen::new);
        openScreen(PLOT_OPEN_CHANNEL, FarmPlotScreen::read, FarmPlotScreen::new);
        openScreen(SELL_TERMINAL_OPEN_CHANNEL, SellTerminalScreen::read, SellTerminalScreen::new);

        ModTransfer.registerChannel(PEN_VISUAL_CHANNEL, transfer -> {
            PenWorldSlimes.SlimeVisual[] visuals = PenWorldSlimes.read(transfer);
            onMainThread(() -> PenWorldSlimes.replace(visuals));
        });

        sendLoaded();
        Enginex.log("SlimeHunt channels registered, loaded packet sent");
    }

    /**
     * ĐźŃ€ĐľĐłŃ€ĐµĐ˛ ĐşŃŤŃĐ° Ń‚ĐµĐşŃŃ‚ŃŃ€ Đ˛ Ń„ĐľĐ˝Đµ: Đ¸Đ˝Đ°Ń‡Đµ ĐżĐµŃ€Đ˛Đ°ŃŹ Đ¶Đµ ĐľŃ‚ĐşŃ€Ń‹Ń‚Đ°ŃŹ ĐĽĐµĐ˝ŃŽŃĐşĐ° ĐłŃ€ŃĐ·Đ¸Ń‚ Đ¸Ń…
     * Đ˛ Đ¸ĐłŃ€ĐľĐ˛ĐľĐĽ ĐżĐľŃ‚ĐľĐşĐµ Đ¸ Đ´Đ°Ń‘Ń‚ Đ·Đ°ĐĽĐµŃ‚Đ˝Ń‹Đą ĐżĐľĐ´Đ˛Đ¸Ń.
     */
    @Override
    public void asyncOnLoad() {
        SlimeUi.closeTexture();
        SlimeUi.closeHoverTexture();
        SlimeUi.slimeTexture();
    }

    /**
     * ĐźĐ°ĐşĐµŃ‚ Ń€Đ°Đ·Đ±Đ¸Ń€Đ°ĐµŃ‚ŃŃŹ Đ˛ ŃĐµŃ‚ĐµĐ˛ĐľĐĽ ĐżĐľŃ‚ĐľĐşĐµ, ŃŤĐşŃ€Đ°Đ˝ ŃŃ‚Ń€ĐľĐ¸Ń‚ŃŃŹ Đ¸ ĐľŃ‚ĐşŃ€Ń‹Đ˛Đ°ĐµŃ‚ŃŃŹ Đ˛ Đ¸ĐłŃ€ĐľĐ˛ĐľĐĽ.
     */
    private static <T> void openScreen(String channel, PacketReader<T> reader, ScreenFactory<T> factory) {
        ModTransfer.registerChannel(channel, transfer -> {
            T data = reader.read(transfer);
            onMainThread(() -> factory.create(data).open());
        });
    }

    private static void onMainThread(Runnable action) {
        Enginex.getMinecraft().execute(action);
    }

    private static void sendLoaded() {
        new ModTransfer()
                .writeString(CLIENT_VERSION)
                .send(LOADED_CHANNEL);
    }

    private interface PacketReader<T> {
        T read(ModTransfer transfer);
    }

    private interface ScreenFactory<T> {
        GuiScreen create(T data);
    }
}
