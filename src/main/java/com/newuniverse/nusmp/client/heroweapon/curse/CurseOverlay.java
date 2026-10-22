package com.newuniverse.nusmp.client.heroweapon.curse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.curse.CurseText;
import com.newuniverse.nusmp.heroweapon.curse.CurseView;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Why: this is what the curse looks like to the person living it. As corruption rises the edges of the view darken in the colour of the sin and beat with the
 * heart; a bar at the bottom left shows the corruption (and, under it, the despair that makes the weapon whisper); while the weapon offers a curse a panel under the
 * crosshair names the sin and shows the three-second hold that accepts it; whispers in the weapon's voice fade in and out in the middle of the screen; and in an
 * Overrun the screen floods and the bar counts the seconds the weapon holds the wielder. It uses plain {@code fill} calls only (no textures, no shaders), is drawn as
 * a GUI layer like the System window, stays out of the way of open screens and of the hidden GUI, and can never take a frame down: the whole draw is inside a try,
 * and a failure switches the overlay off with one log line. The numbers (strength, bands, pulses, fades) are {@link VignetteMath}'s, tested.
 */
final class CurseOverlay {
    private CurseOverlay() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String MOVE_ID = "curse_bar";
    private static boolean broken;

    /** The GUI layer: called every frame by the game. */
    static void render(GuiGraphics g, DeltaTracker delta) {
        if (broken) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null || !CurseClientConfig.overlay()) return;
        long now = mc.level.getGameTime();
        if (!CurseView.anything(now)) return;
        try {
            draw(g, mc, now, delta.getGameTimeDeltaPartialTick(false));
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            LOG.warn("[nusmp] The Curse Series overlay failed to draw and is switched off until the game is restarted", e);
        }
    }

    private static void draw(GuiGraphics g, Minecraft mc, long now, float partial) {
        Sin tint = CurseClient.tint();
        boolean overrun = CurseView.locked(now), offer = CurseView.offerOpen(now);
        double seconds = (now + partial) / 20.0;
        int w = g.guiWidth(), h = g.guiHeight();
        Font font = mc.font;
        if (CurseClientConfig.vignette()) vignette(g, w, h, tint, overrun, offer, seconds);
        if (CurseClientConfig.whispers()) whisper(g, font, w, h, tint, now, offer);
        if (offer) offerPanel(g, font, w, h, tint, now);
        if (CurseClientConfig.bar()) bar(g, font, h, tint, now, overrun);
    }

    // ------------------------------------------------------------------ the tint at the edges
    private static void vignette(GuiGraphics g, int w, int h, Sin tint, boolean overrun, boolean offer, double seconds) {
        int corruption = overrun ? 1000 : CurseView.corruption();
        float cap = CurseClientConfig.vignetteMax();
        float strength = VignetteMath.strength(corruption, offer, overrun, seconds, cap);
        boolean narrow = tint == Sin.PRIDE && corruption > 200;
        if (narrow) strength = Math.min(Math.min(cap, VignetteMath.HARD_CAP), strength * 1.6f);       // Pride's sight narrows: a heavier tint, reaching further in, from the start
        if (strength < 0.004f) return;
        VignetteMath.Layout layout = VignetteMath.layout(w, h, strength, narrow);
        int rgb = shade(tint.rgb(), overrun ? 1.0 : 0.7);
        float finalStrength = strength;
        g.drawManaged(() -> frames(g, w, h, rgb, finalStrength, layout));
    }

    /** The nested frames, outermost first, each in its own opacity: a hundred thin rectangles or so, drawn as one batch. */
    private static void frames(GuiGraphics g, int w, int h, int rgb, float strength, VignetteMath.Layout layout) {
        int rings = layout.rings(), band = layout.band();
        for (int i = 0; i < rings; i++) {
            float a = VignetteMath.ringAlpha(i, rings, strength);
            if (a < 0.004f) break;
            int x1 = i * band, y1 = i * band, x2 = w - i * band, y2 = h - i * band;
            if (x2 - x1 <= band * 2 || y2 - y1 <= band * 2) break;
            int col = VignetteMath.argb(rgb, a);
            g.fill(x1, y1, x2, y1 + band, col);
            g.fill(x1, y2 - band, x2, y2, col);
            g.fill(x1, y1 + band, x1 + band, y2 - band, col);
            g.fill(x2 - band, y1 + band, x2, y2 - band, col);
        }
    }

    // ------------------------------------------------------------------ the weapon's voice
    private static void whisper(GuiGraphics g, Font font, int w, int h, Sin tint, long now, boolean offer) {
        if (!offer && CurseView.corruption() < 500) return;
        Sin sin = offer && CurseView.offered() != null ? CurseView.offered() : tint;
        int period = 160, visible = 120;
        if (!VignetteMath.whisperVisible(now, period, visible)) return;
        float a = VignetteMath.whisperAlpha(VignetteMath.whisperAge(now, period), visible);
        if (a < 0.06f) return;
        int n = CurseText.whisperIndex(Math.floorDiv(now, (long) period));
        Component line = Component.translatableWithFallback(CurseText.whisperKey(sin, n), CurseText.whispers(sin).get(n)).withStyle(ChatFormatting.ITALIC);
        g.drawCenteredString(font, line, w / 2, h / 3, VignetteMath.argb(lighten(sin.rgb(), 0.45), a));
    }

    // ------------------------------------------------------------------ the offer and its hold
    private static void offerPanel(GuiGraphics g, Font font, int w, int h, Sin tint, long now) {
        Sin sin = CurseView.offered() != null ? CurseView.offered() : tint;
        int cx = w / 2, y = h / 2 + 34;
        Component title = Component.translatableWithFallback(CurseText.HUD_OFFER, CurseText.english(CurseText.HUD_OFFER), sin.label());
        g.drawCenteredString(font, title, cx, y, 0xFF000000 | lighten(sin.rgb(), 0.25));
        Component hint = Component.translatableWithFallback(CurseText.HUD_HOLD, CurseText.english(CurseText.HUD_HOLD), CurseClient.EMBRACE.getTranslatedKeyMessage());
        g.drawCenteredString(font, hint, cx, y + 11, 0xFFE0E0E0);
        int bw = 90, x0 = cx - bw / 2, y0 = y + 24;
        g.fill(x0 - 1, y0 - 1, x0 + bw + 1, y0 + 5, 0xAA000000);
        g.fill(x0, y0, x0 + bw, y0 + 4, 0xFF202020);
        g.fill(x0, y0, x0 + Math.round(bw * VignetteMath.holdProgress(CurseClient.held(), CurseClient.HOLD_TICKS)), y0 + 4, 0xFF000000 | sin.rgb());
        g.drawCenteredString(font, (CurseView.offerTicksLeft(now) / 20 + 1) + " s", cx, y0 + 8, 0xFF909090);
    }

    // ------------------------------------------------------------------ the corruption bar
    private static void bar(GuiGraphics g, Font font, int h, Sin tint, long now, boolean overrun) {
        int corruption = overrun ? 1000 : CurseView.corruption(), despair = CurseView.despair();
        boolean cursed = corruption > 0 || CurseView.active() != null;
        if (!cursed && despair < 150) return;
        if (GuiMove.hidden(MOVE_ID)) return;                                           // GUI Move: the player switched the bar off
        int x = CurseClientConfig.barX(), y = h - CurseClientConfig.barBottom(), bw = 92;
        int bottom = despair > 0 ? (cursed ? y + 13 : y + 4) : y + 7;
        boolean moved = GuiMove.apply(g, MOVE_ID, x - 1, y - 11, bw + 2, bottom - (y - 11));       // GUI Move + GUI Size: where the config puts it is the default
        try {
            drawBar(g, font, tint, now, overrun, corruption, despair, cursed, x, y, bw);
        } finally {
            GuiMove.end(g, moved);
        }
    }

    private static void drawBar(GuiGraphics g, Font font, Sin tint, long now, boolean overrun, int corruption, int despair, boolean cursed, int x, int y, int bw) {
        if (cursed) {
            String label;
            int color;
            if (overrun) {
                label = Component.translatableWithFallback(CurseText.HUD_OVERRUN, CurseText.english(CurseText.HUD_OVERRUN)).getString() + " " + (CurseView.overrunTicksLeft(now) / 20 + 1) + " s";
                color = (now / 5L) % 2L == 0L ? 0xFFFF5555 : 0xFFAA2222;
            } else {
                label = Component.translatableWithFallback(CurseText.HUD_CORRUPTION, CurseText.english(CurseText.HUD_CORRUPTION)).getString() + " " + corruption;
                color = 0xFF000000 | lighten(tint.rgb(), 0.3);
            }
            g.drawString(font, label, x, y - 10, color, true);
            g.fill(x - 1, y - 1, x + bw + 1, y + 7, 0xAA000000);
            g.fill(x, y, x + bw, y + 6, 0xFF202020);
            g.fill(x, y, x + Math.round(bw * Math.min(1f, corruption / 1000f)), y + 6, 0xFF000000 | tint.rgb());
            g.fill(x + Math.round(bw * 0.6f), y, x + Math.round(bw * 0.6f) + 1, y + 6, 0x99FFFFFF);
        }
        if (despair > 0) {
            int dy = cursed ? y + 9 : y;
            if (!cursed) g.drawString(font, Component.translatableWithFallback(CurseText.HUD_DESPAIR, CurseText.english(CurseText.HUD_DESPAIR)).getString() + " " + despair, x, dy - 10, 0xFFA898C8, true);
            g.fill(x - 1, dy - 1, x + bw + 1, dy + 4, 0xAA000000);
            g.fill(x, dy, x + bw, dy + 3, 0xFF202020);
            g.fill(x, dy, x + Math.round(bw * Math.min(1f, despair / 1000f)), dy + 3, 0xFF8A78B8);
        }
    }

    // ------------------------------------------------------------------ colour helpers
    /** The colour with each channel multiplied by {@code k} (0 to 1). */
    private static int shade(int rgb, double k) {
        int r = (int) (((rgb >> 16) & 0xFF) * k), gr = (int) (((rgb >> 8) & 0xFF) * k), b = (int) ((rgb & 0xFF) * k);
        return (r << 16) | (gr << 8) | b;
    }

    /** The colour moved {@code k} (0 to 1) of the way to white. */
    private static int lighten(int rgb, double k) {
        int r = ((rgb >> 16) & 0xFF), gr = ((rgb >> 8) & 0xFF), b = (rgb & 0xFF);
        r += (int) ((255 - r) * k);
        gr += (int) ((255 - gr) * k);
        b += (int) ((255 - b) * k);
        return (r << 16) | (gr << 8) | b;
    }
}
