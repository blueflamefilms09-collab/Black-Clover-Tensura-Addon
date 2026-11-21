package com.newuniverse.nusmp.client.doublesui;

import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.doublesui.DoublesUiMath;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the owner's Real Doubles as a tidy vertical stack on the screen (docs/doubles_ui.md), where before there was nothing at all: a header, then one card per double (the
 * selected one on top, taller, with a gold edge; the others compact below it, never over each other), each with a name, the distance, the time left, a health bar and a time bar,
 * and a "+N more" line when more than {@code maxRows} are out. At most 16 doubles, so at most 16 cards; the cost of a frame is a few dozen filled rectangles and a handful of text
 * draws, nothing is allocated (labels are cached Components, the distance and time texts are rebuilt only when their number changes). The position is moved by GUI Move / sized by
 * GUI Size like the other HUDs (layer id {@code doubles_stack}). With {@code doublesStackedUi = false} nothing is drawn, which is exactly the old screen.
 */
final class DoublesOverlay {
    private DoublesOverlay() {}

    private static final int BG = 0xB0100818, EDGE = 0xFFC8B0FF, GOLD = 0xFFE8C468, TEXT = 0xFFE6F2FF, DIM = 0xFF8FA3BF, TRACK = 0xFF2A2140, TIME = 0xFF9A7CFF;
    private static final int HP_GOOD = 0xFF7CF0B0, HP_MID = 0xFFE8C468, HP_LOW = 0xFFFF6A6A;
    private static final Component HEADER = Component.translatable("anime_multiverse.doubles.header");
    private static final Component[] LABEL = new Component[DoublesUiMath.MAX_ROWS + 1], MORE = new Component[DoublesUiMath.MAX_ROWS + 1];

    static void render(GuiGraphics g, DeltaTracker delta) {
        int n = DoublesView.n;
        if (n <= 0 || !DoublesClientConfig.stackedUi()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;
        Font font = mc.font;
        int maxShown = DoublesClientConfig.maxRows(), shown = DoublesUiMath.shown(n, maxShown), hidden = DoublesUiMath.hidden(n, maxShown);
        boolean hasActive = DoublesView.active >= 0 && DoublesView.active < n;
        int x = 6, y = 130, w = DoublesUiMath.W;
        GuiMove.report("doubles_stack", x, y, w, DoublesUiMath.totalHeight(n, maxShown, hasActive));    // GUI Move: where the stack is drawn (the layer hook moves it)

        g.fill(x, y, x + w, y + DoublesUiMath.HEADER_H, BG);
        g.drawString(font, HEADER, x + 4, y + 1, EDGE, false);
        long now = mc.level.getGameTime();
        for (int slot = 0; slot < shown; slot++) {
            int i = DoublesUiMath.source(slot, DoublesView.active, n);
            card(g, font, mc, i, x, y + DoublesUiMath.cardTop(slot, hasActive), w, DoublesUiMath.cardHeight(slot, hasActive), hasActive && slot == 0, now);
        }
        if (hidden > 0) {
            int top = y + DoublesUiMath.cardTop(shown, hasActive);
            g.fill(x, top, x + w, top + DoublesUiMath.MORE_H, BG);
            g.drawString(font, more(hidden), x + 4, top + 1, DIM, false);
        }
    }

    private static void card(GuiGraphics g, Font font, Minecraft mc, int i, int x, int top, int w, int h, boolean active, long now) {
        float hp = DoublesView.hp[i], max = DoublesView.maxHp[i];
        double dist = -1;
        Entity e = DoublesView.entityId[i] > 0 ? mc.level.getEntity(DoublesView.entityId[i]) : null;
        if (e instanceof LivingEntity le && le.isAlive()) { hp = le.getHealth(); max = le.getMaxHealth(); }
        if (e != null) dist = e.distanceTo(mc.player);
        int left = DoublesUiMath.lifeLeft(DoublesView.life[i], DoublesView.syncTick, now);

        g.fill(x, top, x + w, top + h, BG);
        if (active) {                                                                                    // the selected one: a gold edge
            g.fill(x, top, x + w, top + 1, GOLD); g.fill(x, top + h - 1, x + w, top + h, GOLD);
            g.fill(x, top, x + 1, top + h, GOLD); g.fill(x + w - 1, top, x + w, top + h, GOLD);
        }
        int ty = top + (active ? 3 : 2);
        g.drawString(font, label(i + 1), x + 4, ty, active ? GOLD : TEXT, false);
        String t = timeText(i, left);
        int tx = x + w - 4 - font.width(t);
        g.drawString(font, t, tx, ty, DIM, false);
        if (dist >= 0 && DoublesClientConfig.showDistance()) {
            String d = distText(i, (int) dist);
            g.drawString(font, d, tx - 6 - font.width(d), ty, DIM, false);
        }
        int bx = x + 4, bw = w - 8, hpY = active ? top + 14 : top + 11, hpH = active ? 5 : 2, tY = active ? top + 21 : top + 14, tH = active ? 3 : 1;
        float f = DoublesUiMath.frac(hp, max);
        g.fill(bx, hpY, bx + bw, hpY + hpH, TRACK);
        g.fill(bx, hpY, bx + Math.round(bw * f), hpY + hpH, f > 0.5f ? HP_GOOD : f > 0.25f ? HP_MID : HP_LOW);
        g.fill(bx, tY, bx + bw, tY + tH, TRACK);
        g.fill(bx, tY, bx + Math.round(bw * DoublesUiMath.frac(left, DoublesView.lifeMax[i])), tY + tH, TIME);
    }

    private static Component label(int number) {
        Component c = LABEL[number];
        return c != null ? c : (LABEL[number] = Component.translatable("anime_multiverse.doubles.label", number));
    }

    private static Component more(int hidden) {
        Component c = MORE[hidden];
        return c != null ? c : (MORE[hidden] = Component.translatable("anime_multiverse.doubles.more", hidden));
    }

    private static String timeText(int i, int ticks) {
        int sec = (ticks + 19) / 20;
        if (DoublesView.timeShown[i] != sec) {
            DoublesView.timeShown[i] = sec;
            DoublesView.timeTxt[i] = (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
        }
        return DoublesView.timeTxt[i];
    }

    private static String distText(int i, int blocks) {
        if (DoublesView.distShown[i] != blocks) {
            DoublesView.distShown[i] = blocks;
            DoublesView.distTxt[i] = blocks + "m";
        }
        return DoublesView.distTxt[i];
    }
}
