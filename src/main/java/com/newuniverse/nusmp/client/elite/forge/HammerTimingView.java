package com.newuniverse.nusmp.client.elite.forge;

import com.newuniverse.nusmp.elite.forge.rules.HammerTiming;
import com.newuniverse.nusmp.elite.forge.rules.MinigameType;
import com.newuniverse.nusmp.elite.forge.rules.Minigames;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Hammer Timing minigame as a timing bar. A cursor sweeps along the bar (its place is {@link HammerTiming#cursor} of the time since the step began, so it
 * is exactly what the server will compute), a gold zone shows where the next strike should land, and a click (or the space key) is a hammer blow at the time of the
 * click. The blow is ignored when it comes before the earliest time or too soon after the last one, the same limits the server enforces, so the view never sends
 * something the server would refuse. Blows already struck stay on the bar as small marks coloured by how good they were. The step ends after the last blow or at the
 * time limit.
 */
final class HammerTimingView implements MinigameView {
    private long seed;
    private int difficulty;
    private long startNs;
    private final List<Integer> strikes = new ArrayList<>();
    private final List<Double> hitPos = new ArrayList<>();
    private final List<Double> quality = new ArrayList<>();
    private int flashTicks;
    private String flash = "";
    private int flashColor = ForgeDraw.TEXT;
    private boolean over;

    @Override public MinigameType type() { return MinigameType.HAMMER_TIMING; }

    @Override
    public void begin(long seed, int difficulty) {
        this.seed = seed;
        this.difficulty = difficulty;
        this.startNs = System.nanoTime();
    }

    private long elapsed() { return (System.nanoTime() - startNs) / 1_000_000L; }

    @Override
    public void tick(int mouseX, int mouseY, boolean mouseDown) {
        if (flashTicks > 0) flashTicks--;
        if (!over && elapsed() > Minigames.HAMMER.limitMillis(difficulty)) over = true;
    }

    private boolean strike() {
        if (over) return false;
        long t = elapsed();
        int n = strikes.size();
        if (t < HammerTiming.FIRST_MS || (n > 0 && t - strikes.get(n - 1) < HammerTiming.GAP_MS) || t > Minigames.HAMMER.limitMillis(difficulty)) return false;
        double pos = HammerTiming.cursor(seed, difficulty, t);
        double acc = HammerTiming.accuracy(pos - HammerTiming.target(seed, n), HammerTiming.halfWidth(difficulty));
        strikes.add((int) t);
        hitPos.add(pos);
        quality.add(acc);
        flashTicks = 14;
        if (acc >= 0.99) {
            flash = "nusmp.eforge.mg.perfect";
            flashColor = ForgeDraw.GOLD;
        } else if (acc >= 0.6) {
            flash = "nusmp.eforge.mg.good";
            flashColor = ForgeDraw.GREEN;
        } else if (acc > 0) {
            flash = "nusmp.eforge.mg.weak";
            flashColor = ForgeDraw.ORANGE;
        } else {
            flash = "nusmp.eforge.mg.miss";
            flashColor = ForgeDraw.RED;
        }
        if (strikes.size() >= HammerTiming.strikes(difficulty)) over = true;
        return true;
    }

    @Override
    public boolean click(int x, int y, int w, int h, double mx, double my, int button) {
        if (button != 0) return false;
        strike();
        return true;
    }

    @Override
    public boolean key(int keyCode) {
        if (keyCode == 32) {
            strike();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY, boolean mouseDown) {
        int n = strikes.size(), total = HammerTiming.strikes(difficulty);
        int bx = x + 20, bw = w - 40, by = y + h / 2 - 12, bh = 24;
        g.fill(bx, by, bx + bw, by + bh, 0xCC060912);
        g.renderOutline(bx - 1, by - 1, bw + 2, bh + 2, ForgeDraw.alpha(ForgeDraw.CYAN, 0.55f));
        if (n < total) {
            double c = HammerTiming.target(seed, n), hw = HammerTiming.halfWidth(difficulty);
            int zx0 = bx + (int) Math.round((c - hw) * bw), zx1 = bx + (int) Math.round((c + hw) * bw);
            g.fill(zx0, by + 1, zx1, by + bh - 1, ForgeDraw.alpha(ForgeDraw.GOLD, 0.35f));
            int cx0 = bx + (int) Math.round((c - hw * 0.25) * bw), cx1 = bx + (int) Math.round((c + hw * 0.25) * bw);
            g.fill(cx0, by + 1, cx1, by + bh - 1, ForgeDraw.alpha(ForgeDraw.GOLD, 0.75f));
            g.renderOutline(zx0, by + 1, zx1 - zx0, bh - 2, ForgeDraw.GOLD);
        }
        for (int i = 0; i < n; i++) {
            int mx = bx + (int) Math.round(hitPos.get(i) * bw);
            int col = quality.get(i) >= 0.99 ? ForgeDraw.GOLD : quality.get(i) >= 0.6 ? ForgeDraw.GREEN : quality.get(i) > 0 ? ForgeDraw.ORANGE : ForgeDraw.RED;
            g.fill(mx - 1, by + bh + 3, mx + 2, by + bh + 9, col);
        }
        if (!over) {
            int cx = bx + (int) Math.round(HammerTiming.cursor(seed, difficulty, elapsed()) * bw);
            g.fill(cx - 1, by - 6, cx + 2, by + bh + 6, ForgeDraw.TEXT);
            g.fill(cx - 3, by - 8, cx + 4, by - 5, ForgeDraw.CYAN);
        }
        g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.strikes", n, total), x + w / 2, by - 28, ForgeDraw.TEXT);
        if (flashTicks > 0) g.drawCenteredString(font, Component.translatable(flash), x + w / 2, by + bh + 16, ForgeDraw.alpha(flashColor, Math.min(1f, flashTicks / 6f)));
        ForgeDraw.bar(g, x + 20, y + h - 14, w - 40, 5, liveScore() / 1000f, ForgeDraw.GOLD);
    }

    @Override public boolean finished() { return over; }

    @Override
    public int[] input() {
        int[] out = new int[strikes.size()];
        for (int i = 0; i < out.length; i++) out[i] = strikes.get(i);
        return out;
    }

    @Override public String hintKey() { return "nusmp.eforge.mg.hammer.hint"; }

    @Override
    public int liveScore() {
        if (quality.isEmpty()) return 0;
        double sum = 0;
        for (double q : quality) sum += q;
        return (int) Math.round(1000 * sum / HammerTiming.strikes(difficulty));
    }
}
