package dev.lhoopy.slimehunt.client;

import gg.cristalix.enginex.Enginex;
import gg.cristalix.enginex.color.Color;
import gg.cristalix.enginex.color.palette.Palette;
import gg.cristalix.enginex.element.Container;
import gg.cristalix.enginex.element.Rectangle;
import gg.cristalix.enginex.element.Text;
import gg.cristalix.enginex.element.screen.type.GuiScreen;
import gg.cristalix.enginex.event.element.GuiScreenCloseEvent;
import gg.cristalix.enginex.event.input.MouseLeftClickEvent;
import gg.cristalix.enginex.timer.task.ScheduledTask;
import gg.cristalix.enginex.transfer.ModTransfer;

import java.util.Random;

final class SlimeHuntScreen extends GuiScreen {
    private final Random random;
    private final int totalRounds;
    private final int requiredHits;
    private final double timeoutSeconds;

    private final Container playfield = new Container();
    private final Text status = new Text();

    private ScheduledTask timeoutTask;
    private int round;
    private int hits;
    private boolean finished;
    private boolean acceptingClick;

    SlimeHuntScreen(int seed, int totalRounds, int requiredHits, int timeoutMillis) {
        this.random = new Random(seed);
        this.totalRounds = totalRounds;
        this.requiredHits = requiredHits;
        this.timeoutSeconds = Math.max(0.35, timeoutMillis / 1000.0D);

        setColor(new Color(6, 9, 16, 0.76));
        setBlur(3.0F);
        setSize(1920, 1080, 0);

        Text title = new Text("Охота на слайма");
        title.setColor(Palette.PINK_LIGHT);
        title.setShadow(true);
        title.setScale(2.0);
        title.setOrigin(0.5, 0.5, 0);
        title.setAlign(0.5, 0.16, 0);

        status.setColor(Palette.WHITE_86);
        status.setShadow(true);
        status.setScale(1.2);
        status.setOrigin(0.5, 0.5, 0);
        status.setAlign(0.5, 0.22, 0);

        playfield.setSize(760, 430, 0);
        playfield.setOrigin(0.5, 0.5, 0);
        playfield.setAlign(0.5, 0.56, 0);

        addChild(title, status, playfield);
        registerEvent(GuiScreenCloseEvent.class, event -> finish(false));
        nextRound();
    }

    private void nextRound() {
        cancelTimeout();
        playfield.clearChildren();
        acceptingClick = true;

        if (round >= totalRounds) {
            finish(hits >= requiredHits);
            return;
        }

        round++;
        status.setValue("Попадания: " + hits + "/" + requiredHits + "   Круг: " + round + "/" + totalRounds);

        double size = Math.max(42, 86 - round * 4);
        double x = 50 + random.nextInt(660);
        double y = 35 + random.nextInt(340);

        Container target = new Container();
        target.setSize(size + 22, size + 22, 0);
        target.setPos(x - 11, y - 11, 0);
        target.setInteractive(true);
        target.registerEvent(MouseLeftClickEvent.class, event -> hit());

        Rectangle hitbox = new Rectangle();
        hitbox.setSize(size + 22, size + 22, 0);
        hitbox.setColor(new Color(255, 255, 255, 0.08));
        hitbox.setInteractive(true);
        hitbox.registerEvent(MouseLeftClickEvent.class, event -> hit());

        Text circle = new Text("\u25cf");
        circle.setColor(Palette.PINK_LIGHT);
        circle.setShadow(true);
        circle.setScale(size / 12.0D);
        circle.setOrigin(0.5, 0.5, 0);
        circle.setPos((size + 22) / 2.0D, (size + 22) / 2.0D - 2, 2);
        circle.setSize(size + 22, size + 22, 0);
        circle.setInteractive(true);
        circle.registerEvent(MouseLeftClickEvent.class, event -> hit());

        Text number = new Text(String.valueOf(round));
        number.setColor(Palette.WHITE);
        number.setShadow(true);
        number.setScale(1.35);
        number.setOrigin(0.5, 0.5, 0);
        number.setPos((size + 22) / 2.0D, (size + 22) / 2.0D - 4, 3);
        number.setSize(size + 22, size + 22, 0);
        number.setInteractive(true);
        number.registerEvent(MouseLeftClickEvent.class, event -> hit());

        target.addChild(hitbox, circle, number);
        playfield.addChild(target);
        timeoutTask = Enginex.getTimerManager().after(timeoutSeconds, task -> nextRound());
    }

    private void hit() {
        if (finished || !acceptingClick) {
            return;
        }
        acceptingClick = false;
        hits++;
        if (hits >= requiredHits) {
            finish(true);
            return;
        }
        nextRound();
    }

    private void finish(boolean success) {
        if (finished) {
            return;
        }
        finished = true;
        acceptingClick = false;
        cancelTimeout();
        new ModTransfer()
            .writeInt(hits)
            .writeInt(totalRounds)
            .writeBoolean(success)
            .send(SlimeHuntMod.RESULT_CHANNEL);
        close();
    }

    private void cancelTimeout() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
    }
}
