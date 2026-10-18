package com.newuniverse.nusmp.client.elite.forge;

import com.newuniverse.nusmp.elite.forge.rules.MagiculeFlow;
import com.newuniverse.nusmp.elite.forge.rules.MinigameType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Why: the Magicule Flow minigame, "steady a flow meter with the mouse". A vertical tube holds the magicule flow as a needle that drifts on its own
 * ({@link MagiculeFlow#drift}); the player pushes it back by moving the mouse above (push up) or below (push down) the middle of the tube, and keeps it in the
 * bright middle zone. One mouse sample is taken per client tick and fed to the very same {@link MagiculeFlow.Run} the server uses, so the needle shown is the needle
 * scored. A scrolling trace to the right shows the last few seconds. The step is over when every sample has been taken.
 */
final class MagiculeFlowView implements MinigameView {
    private long seed;
    private int difficulty;
    private MagiculeFlow.Run run;
    private int[] samples;
    private int n;
    private double[] trace;
    private int lastControl = 500;
    private int tubeTop, tubeH;

    @Override public MinigameType type() { return MinigameType.MAGICULE_FLOW; }

    @Override
    public void begin(long seed, int difficulty) {
        this.seed = seed;
        this.difficulty = difficulty;
        this.run = new MagiculeFlow.Run(seed, difficulty);
        this.samples = new int[MagiculeFlow.samples(difficulty)];
        this.trace = new double[samples.length];
        this.n = 0;
    }

    @Override
    public void tick(int mouseX, int mouseY, boolean mouseDown) {
        if (n >= samples.length) return;
        int c = tubeH <= 0 ? 500 : (int) Math.round(500 + (tubeTop + tubeH / 2.0 - mouseY) / (tubeH / 2.0) * 500);
        c = Math.max(0, Math.min(1000, c));
        lastControl = c;
        samples[n] = c;
        trace[n] = run.step(c);
        n++;
    }

    @Override public boolean click(int x, int y, int w, int h, double mx, double my, int button) { return false; }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY, boolean mouseDown) {
        tubeTop = y + 8;
        tubeH = h - 30;
        int tw = 30, tx = x + w / 2 - tw / 2;
        g.fill(tx, tubeTop, tx + tw, tubeTop + tubeH, 0xCC060912);
        // the fill follows the needle, hotter when it is far from the middle
        double needle = run == null ? 500 : run.needle();
        int fy = tubeTop + tubeH - (int) Math.round(tubeH * needle / 1000.0);
        float off = (float) Math.min(1.0, Math.abs(needle - 500) / MagiculeFlow.band(difficulty));
        int col = ForgeDraw.mix(ForgeDraw.CYAN, ForgeDraw.RED, off);
        g.fill(tx + 2, fy, tx + tw - 2, tubeTop + tubeH - 1, ForgeDraw.alpha(col, 0.55f));
        // the perfect zone and the edge of the band
        int zTop = tubeTop + tubeH - (int) Math.round(tubeH * (500 + MagiculeFlow.ZONE) / 1000.0), zBot = tubeTop + tubeH - (int) Math.round(tubeH * (500 - MagiculeFlow.ZONE) / 1000.0);
        g.fill(tx, zTop, tx + tw, zBot, ForgeDraw.alpha(ForgeDraw.GREEN, 0.22f));
        g.renderOutline(tx, zTop, tw, zBot - zTop, ForgeDraw.alpha(ForgeDraw.GREEN, 0.8f));
        double band = MagiculeFlow.band(difficulty);
        int bTop = tubeTop + tubeH - (int) Math.round(tubeH * (500 + band) / 1000.0), bBot = tubeTop + tubeH - (int) Math.round(tubeH * (500 - band) / 1000.0);
        g.fill(tx - 4, bTop, tx, bTop + 1, ForgeDraw.DIM);
        g.fill(tx - 4, bBot, tx, bBot + 1, ForgeDraw.DIM);
        g.renderOutline(tx - 1, tubeTop - 1, tw + 2, tubeH + 2, ForgeDraw.alpha(ForgeDraw.CYAN, 0.6f));
        g.fill(tx - 3, fy - 1, tx + tw + 3, fy + 1, ForgeDraw.TEXT);
        // the hand: where the mouse is pushing, drawn on the left of the tube
        int hy = tubeTop + tubeH - (int) Math.round(tubeH * lastControl / 1000.0);
        int hx = tx - 26;
        for (int i = 0; i < 6; i++) g.fill(hx + i, hy - 5 + i, hx + i + 1, hy + 6 - i, ForgeDraw.GOLD);
        g.drawString(font, Component.translatable("nusmp.forge.mg.flow.push"), hx - 40, hy - 4, ForgeDraw.DIM, false);
        // the trace of the last 4 seconds on the right
        int trx = tx + tw + 14, trw = Math.min(110, x + w - trx - 8), len = Math.min(n, 80);
        if (trw > 20 && len > 1) {
            g.fill(trx, tubeTop, trx + trw, tubeTop + tubeH, 0x88060912);
            int mid = tubeTop + tubeH / 2;
            g.fill(trx, mid, trx + trw, mid + 1, ForgeDraw.alpha(ForgeDraw.GREEN, 0.5f));
            for (int i = 0; i < len; i++) {
                double v = trace[n - len + i];
                int py = tubeTop + tubeH - (int) Math.round(tubeH * v / 1000.0);
                ForgeDraw.dot(g, trx + trw - 2 - (len - 1 - i) * (double) trw / 80.0, py, 2, Math.abs(v - 500) <= MagiculeFlow.ZONE ? ForgeDraw.GREEN : ForgeDraw.ORANGE);
            }
        }
        float done = samples == null ? 0f : n / (float) samples.length;
        ForgeDraw.bar(g, x + 20, y + h - 14, w - 40, 5, done, ForgeDraw.CYAN);
        g.drawCenteredString(font, Component.translatable("nusmp.forge.mg.score", liveScore() / 10), x + w - 40, y + 4, ForgeDraw.TEXT);
    }

    @Override public boolean finished() { return samples != null && n >= samples.length; }

    @Override public int[] input() { return samples == null ? new int[0] : samples.clone(); }

    @Override public String hintKey() { return "nusmp.forge.mg.flow.hint"; }

    @Override public int liveScore() { return run == null || n == 0 ? 0 : run.score(n); }
}
