package com.newuniverse.nusmp.client.ui.theme;

import net.minecraft.client.gui.GuiGraphics;

/**
 * An animated progress / mastery bar: the shown value eases toward the target, the fill is a bright-to-deep gradient with ten faint ticks, a
 * shimmer streak travels along it and the leading edge glows. One instance per bar (it holds the eased value); drawing allocates nothing.
 */
public final class UiBar {
    private float shown;
    private boolean started;
    private float lastNow = -1f;

    /** Moves the shown value toward 'target' (0..1) using the clock; the first call jumps straight to it unless 'fromZero'. */
    public void update(float target, float now, boolean fromZero) {
        target = UiMath.clamp01(target);
        if (!UiTheme.motion()) { started = true; shown = target; return; }                // motion off: no easing, no start-from-zero
        if (!started) {
            started = true;
            shown = fromZero ? 0f : target;
        }
        float dt = lastNow < 0f ? 0f : Math.min(6f, now - lastNow);
        lastNow = now;
        shown = UiMath.approach(shown, target, 0.12f, dt, 0.002f);
    }

    public float shown() { return shown; }

    public void draw(GuiGraphics g, int x, int y, int w, int h, int rgb, float now, float alpha) {
        draw(g, x, y, w, h, shown, rgb, now, alpha);
    }

    /** Draws a bar at 'frac' (0..1) without using an instance's easing. */
    public static void draw(GuiGraphics g, int x, int y, int w, int h, float frac, int rgb, float now, float alpha) {
        int a = rgb & 0xFFFFFF;
        g.fill(x, y, x + w, y + h, UiMath.withAlpha(0x03050A, 0.72f * alpha));
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, UiMath.withAlpha(a, 0.42f * alpha));
        int fw = Math.round(w * UiMath.clamp01(frac));
        if (fw > 0) {
            g.fillGradient(x, y, x + fw, y + h, UiMath.withAlpha(UiMath.mix(0xFF000000 | a, 0xFFFFFFFF, 0.38f), 0.95f * alpha),
                    UiMath.withAlpha(UiMath.mix(0xFF000000 | a, 0xFF000000, 0.45f), 0.95f * alpha));
            int sx = x + (int) ((now * 1.3f) % (fw + 36)) - 18;                         // the travelling shimmer
            int s0 = Math.max(x, sx), s1 = Math.min(x + fw, sx + 14);
            if (s1 > s0) g.fill(s0, y, s1, y + h, UiMath.withAlpha(0xFFFFFF, 0.26f * alpha));
            g.fill(x, y, x + fw, y + 1, UiMath.withAlpha(0xFFFFFF, 0.30f * alpha));     // the top highlight
            g.fill(x + fw - 1, y - 1, x + fw, y + h + 1, UiMath.withAlpha(0xFFFFFF, 0.9f * alpha));
            g.fill(x + fw, y, x + fw + 1, y + h, UiMath.withAlpha(a, 0.5f * alpha));
        }
        for (int k = 1; k < 10; k++) {                                                    // ten faint ticks
            int tx = x + w * k / 10;
            g.fill(tx, y, tx + 1, y + h, tx <= x + fw ? UiMath.withAlpha(0x000000, 0.28f * alpha) : UiMath.withAlpha(0xFFFFFF, 0.10f * alpha));
        }
    }
}
