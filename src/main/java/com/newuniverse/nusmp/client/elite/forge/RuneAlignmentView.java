package com.newuniverse.nusmp.client.elite.forge;

import com.newuniverse.nusmp.elite.forge.rules.MinigameType;
import com.newuniverse.nusmp.elite.forge.rules.Minigames;
import com.newuniverse.nusmp.elite.forge.rules.RuneAlignment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Why: the Rune Alignment minigame, "rotate and align rune rings". Concentric rings, innermost first: each carries a bright rune marker and has a gold notch on its rim
 * where the marker must end. A ring turns by itself ({@link RuneAlignment#omega}) and the player turns it further by DRAGGING the mouse around the centre; a right click
 * (or space) locks the ring at the angle it has at that moment, so the player has to lead the ring and lock as the marker passes the notch. The marker drawn is
 * {@code player angle + drift}, exactly the angle the server scores. A lock too early (the first before 0.6 s, the next before 0.5 s after the last) is ignored, the same
 * limits the server enforces. The step ends when every ring is locked or the time is up.
 */
final class RuneAlignmentView implements MinigameView {
    private long seed;
    private int difficulty;
    private long startNs;
    private int rings;
    private int active;
    private final int[] player = new int[4], lockMs = new int[4];
    private final double[] acc = new double[4];
    private boolean dragging;
    private int lastMouseAngle;
    private int flashTicks;
    private int cx, cy;
    private boolean over;

    @Override public MinigameType type() { return MinigameType.RUNE_ALIGNMENT; }

    @Override
    public void begin(long seed, int difficulty) {
        this.seed = seed;
        this.difficulty = difficulty;
        this.rings = RuneAlignment.rings(difficulty);
        this.startNs = System.nanoTime();
        for (int i = 0; i < 4; i++) {
            player[i] = (int) (RuneAlignment.FULL * ((i * 0.37 + 0.11) % 1.0));
            lockMs[i] = 0;
        }
    }

    private long elapsed() { return (System.nanoTime() - startNs) / 1_000_000L; }

    @Override
    public void tick(int mouseX, int mouseY, boolean mouseDown) {
        if (flashTicks > 0) flashTicks--;
        if (!over && elapsed() > Minigames.RUNES.limitMillis(difficulty)) over = true;
    }

    private int mouseAngle(double mx, double my) {
        double a = Math.toDegrees(Math.atan2(mx - cx, cy - my));
        return Math.floorMod((int) Math.round(a * 10), 3600);
    }

    private static int norm(int d) {
        int x = Math.floorMod(d, 3600);
        return x > 1800 ? x - 3600 : x;
    }

    private void lock() {
        if (over || active >= rings) return;
        long t = elapsed();
        long min = active == 0 ? RuneAlignment.FIRST_MS : lockMs[active - 1] + RuneAlignment.GAP_MS;
        if (t < min || t > Minigames.RUNES.limitMillis(difficulty)) {
            flashTicks = 12;
            return;
        }
        lockMs[active] = (int) t;
        int eff = RuneAlignment.effective(seed, difficulty, active, player[active], (int) t);
        acc[active] = RuneAlignment.accuracy(RuneAlignment.distance(eff, RuneAlignment.target(seed, active)), difficulty);
        active++;
        dragging = false;
        if (active >= rings) over = true;
    }

    @Override
    public boolean click(int x, int y, int w, int h, double mx, double my, int button) {
        if (button == 1) {
            lock();
            return true;
        }
        if (button == 0 && !over) {
            dragging = true;
            lastMouseAngle = mouseAngle(mx, my);
            return true;
        }
        return false;
    }

    @Override
    public void drag(int x, int y, int w, int h, double mx, double my) {
        if (!dragging || over || active >= rings) return;
        int a = mouseAngle(mx, my);
        player[active] = Math.floorMod(player[active] + norm(a - lastMouseAngle), 3600);
        lastMouseAngle = a;
    }

    @Override public void release(double mx, double my, int button) { if (button == 0) dragging = false; }

    @Override
    public boolean key(int keyCode) {
        if (keyCode == 32) {
            lock();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY, boolean mouseDown) {
        cx = x + w / 2;
        cy = y + h / 2 - 4;
        double rMax = Math.min(w, h) / 2.0 - 16;
        double gap = rings > 1 ? Math.min(16.0, (rMax - 18) / (rings - 1)) : 16.0;
        long t = over && active > 0 ? lockMs[active - 1] : elapsed();
        for (int i = 0; i < rings; i++) {
            double r = 18 + gap * i;
            boolean locked = lockMs[i] != 0, isActive = i == active && !over;
            ForgeDraw.ring(g, cx, cy, r, 1, ForgeDraw.alpha(isActive ? ForgeDraw.CYAN : ForgeDraw.FAINT, isActive ? 0.9f : 0.5f));
            int target = RuneAlignment.target(seed, i);
            double nx = ForgeDraw.px(cx, r + 5, target), ny = ForgeDraw.py(cy, r + 5, target);
            ForgeDraw.line(g, ForgeDraw.px(cx, r - 1, target), ForgeDraw.py(cy, r - 1, target), nx, ny, 1, ForgeDraw.alpha(ForgeDraw.GOLD, locked || isActive ? 1f : 0.5f));
            ForgeDraw.dot(g, nx, ny, 3, ForgeDraw.GOLD);
            int eff = locked ? RuneAlignment.effective(seed, difficulty, i, player[i], lockMs[i]) : RuneAlignment.effective(seed, difficulty, i, player[i], (int) Math.min(t, Integer.MAX_VALUE));
            int col = locked ? (acc[i] >= 0.99 ? ForgeDraw.GOLD : acc[i] >= 0.5 ? ForgeDraw.GREEN : acc[i] > 0 ? ForgeDraw.ORANGE : ForgeDraw.RED) : isActive ? ForgeDraw.TEXT : ForgeDraw.DIM;
            ForgeDraw.dot(g, ForgeDraw.px(cx, r, eff), ForgeDraw.py(cy, r, eff), isActive ? 6 : 4, col);
            if (locked) ForgeDraw.dot(g, ForgeDraw.px(cx, r, eff), ForgeDraw.py(cy, r, eff), 2, 0xFF060912);
        }
        ForgeDraw.dot(g, cx, cy, 5, ForgeDraw.alpha(ForgeDraw.VIOLET, 0.9f));
        g.drawCenteredString(font, Component.translatable("nusmp.forge.mg.rune.ring", Math.min(active + 1, rings), rings), x + w / 2, y + 2, ForgeDraw.TEXT);
        if (flashTicks > 0) g.drawCenteredString(font, Component.translatable("nusmp.forge.mg.rune.wait"), x + w / 2, y + h - 26, ForgeDraw.RED);
        float used = (float) Math.min(1.0, elapsed() / (double) Minigames.RUNES.limitMillis(difficulty));
        ForgeDraw.bar(g, x + 20, y + h - 14, w - 40, 5, over ? 1f : used, ForgeDraw.alpha(ForgeDraw.CYAN, 1f));
        g.drawCenteredString(font, Component.translatable("nusmp.forge.mg.score", liveScore() / 10), x + w - 40, y + 4, ForgeDraw.TEXT);
    }

    @Override public boolean finished() { return over; }

    @Override
    public int[] input() {
        int[] out = new int[2 * rings];
        for (int i = 0; i < rings; i++) {
            out[2 * i] = lockMs[i] == 0 ? 0 : Math.floorMod(player[i], 3600);
            out[2 * i + 1] = lockMs[i];
        }
        return out;
    }

    @Override public String hintKey() { return "nusmp.forge.mg.rune.hint"; }

    @Override
    public int liveScore() {
        if (rings == 0) return 0;
        double sum = 0;
        for (int i = 0; i < rings; i++) sum += acc[i];
        return (int) Math.round(1000 * sum / rings);
    }
}
