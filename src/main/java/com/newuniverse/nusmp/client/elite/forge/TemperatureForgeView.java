package com.newuniverse.nusmp.client.elite.forge;

import com.newuniverse.nusmp.elite.forge.rules.MinigameType;
import com.newuniverse.nusmp.elite.forge.rules.TemperatureForge;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Why: the Temperature Forge minigame, "keep a heat gauge in the band". A tall gauge shows the heat of the metal; a window (the band) rises and falls on its own
 * ({@link TemperatureForge#center}) and the player holds the left mouse button to work the bellows (heat rises) and releases it to let the metal cool, keeping the heat
 * inside the window. One sample of the button is taken per client tick and fed to the same {@link TemperatureForge.Run} the server re-scores with (which also applies
 * the rule that the button cannot flip faster than every two ticks), so what the player sees is what is scored. The first second is a warm-up that scores nothing. A
 * trace on the right shows the heat and the window of the last few seconds.
 */
final class TemperatureForgeView implements MinigameView {
    private long seed;
    private int difficulty;
    private TemperatureForge.Run run;
    private int[] samples;
    private double[] heat, center;
    private int n;

    @Override public MinigameType type() { return MinigameType.TEMPERATURE_FORGE; }

    @Override
    public void begin(long seed, int difficulty) {
        this.seed = seed;
        this.difficulty = difficulty;
        this.run = new TemperatureForge.Run(seed, difficulty);
        this.samples = new int[TemperatureForge.samples(difficulty)];
        this.heat = new double[samples.length];
        this.center = new double[samples.length];
        this.n = 0;
    }

    @Override
    public void tick(int mouseX, int mouseY, boolean mouseDown) {
        if (n >= samples.length) return;
        samples[n] = mouseDown ? 1 : 0;
        center[n] = TemperatureForge.center(seed, n);
        heat[n] = run.step(samples[n]);
        n++;
    }

    @Override public boolean click(int x, int y, int w, int h, double mx, double my, int button) { return button == 0; }

    private static int heatColor(double v) {
        float t = (float) (v / 1000.0);
        return t < 0.5f ? ForgeDraw.mix(0xFF3A4A66, 0xFFFF8A2A, t * 2f) : ForgeDraw.mix(0xFFFF8A2A, 0xFFFFF2C0, (t - 0.5f) * 2f);
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY, boolean mouseDown) {
        int top = y + 8, gh = h - 30, gw = 30, gx = x + w / 2 - gw / 2;
        g.fill(gx, top, gx + gw, top + gh, 0xCC060912);
        double heatNow = run == null ? TemperatureForge.START : run.gauge();
        int fy = top + gh - (int) Math.round(gh * heatNow / 1000.0);
        g.fillGradient(gx + 2, fy, gx + gw - 2, top + gh - 1, heatColor(heatNow), heatColor(heatNow * 0.35));
        double c = n == 0 ? TemperatureForge.center(seed, 0) : TemperatureForge.center(seed, Math.min(n, samples.length - 1));
        double hb = TemperatureForge.halfBand(difficulty);
        boolean warm = n < TemperatureForge.WARMUP;
        int bt = top + gh - (int) Math.round(gh * Math.min(1000, c + hb) / 1000.0), bb = top + gh - (int) Math.round(gh * Math.max(0, c - hb) / 1000.0);
        boolean inside = Math.abs(heatNow - c) <= hb;
        int bandCol = warm ? ForgeDraw.DIM : inside ? ForgeDraw.GREEN : ForgeDraw.GOLD;
        g.fill(gx, bt, gx + gw, bb, ForgeDraw.alpha(bandCol, 0.22f));
        g.renderOutline(gx - 1, bt, gw + 2, bb - bt, ForgeDraw.alpha(bandCol, 0.95f));
        g.renderOutline(gx - 1, top - 1, gw + 2, gh + 2, ForgeDraw.alpha(ForgeDraw.ORANGE, 0.6f));
        g.fill(gx - 3, fy - 1, gx + gw + 3, fy + 1, ForgeDraw.TEXT);
        // the bellows indicator
        boolean heating = run != null && run.heating();
        g.drawString(font, Component.translatable(heating ? "nusmp.forge.mg.temp.heating" : "nusmp.forge.mg.temp.cooling"), gx - 78, top + gh / 2 - 4, heating ? ForgeDraw.ORANGE : ForgeDraw.CYAN, false);
        if (warm) g.drawCenteredString(font, Component.translatable("nusmp.forge.mg.temp.warmup"), x + w / 2, top + gh + 3, ForgeDraw.DIM);
        // the trace
        int trx = gx + gw + 14, trw = Math.min(120, x + w - trx - 8), len = Math.min(n, 80);
        if (trw > 20 && len > 1) {
            g.fill(trx, top, trx + trw, top + gh, 0x88060912);
            for (int i = 0; i < len; i++) {
                double px = trx + trw - 2 - (len - 1 - i) * (double) trw / 80.0;
                int idx = n - len + i;
                int cy = top + gh - (int) Math.round(gh * center[idx] / 1000.0);
                g.fill((int) px, cy - (int) Math.round(gh * hb / 1000.0), (int) px + 2, cy + (int) Math.round(gh * hb / 1000.0), ForgeDraw.alpha(ForgeDraw.GREEN, 0.2f));
                ForgeDraw.dot(g, px, top + gh - gh * heat[idx] / 1000.0, 2, heatColor(heat[idx]));
            }
        }
        float done = samples == null ? 0f : n / (float) samples.length;
        ForgeDraw.bar(g, x + 20, y + h - 14, w - 40, 5, done, ForgeDraw.ORANGE);
        g.drawCenteredString(font, Component.translatable("nusmp.forge.mg.score", liveScore() / 10), x + w - 40, y + 4, ForgeDraw.TEXT);
    }

    @Override public boolean finished() { return samples != null && n >= samples.length; }

    @Override public int[] input() { return samples == null ? new int[0] : samples.clone(); }

    @Override public String hintKey() { return "nusmp.forge.mg.temp.hint"; }

    @Override public int liveScore() { return run == null || n <= TemperatureForge.WARMUP ? 0 : (int) Math.round(run.score()); }
}
