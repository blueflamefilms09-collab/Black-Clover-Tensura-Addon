package com.newuniverse.nusmp.client.relics.djinn.gui;

import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.*;

/**
 * Paints the whole active-Djinn panel onto a {@link Canvas}: themed background, medallion with the animated emblem, accent, particles, the text and
 * the restyled bars, the themed frame, and the summon intro and phase surges over everything. Pure: the game, the preview tool and the tests all call
 * this one class. One instance is reused for every frame (it keeps a few text caches and the pen).
 *
 * <p>Layers (back to front): background and haze, medallion disc, rune ring, accent, emblem, timer ring, particles, text and bars, frame, overlays.
 */
public final class DjinnGuiPainter {
    private final Ink ink = new Ink();
    private GuiLabels labels = GuiLabels.DEFAULT;

    private static final String[] TIMES = new String[1801];
    private int phaseKey = -1, footKey = -1, trustKey = -1;
    private String phaseText = "", footRight = "", trustText = "";

    public void setLabels(GuiLabels l) {
        labels = l == null ? GuiLabels.DEFAULT : l;
        phaseKey = footKey = trustKey = -1;
    }

    /** Rectangles the last {@link #paint} handed to the canvas (a cost measure). */
    public int lastFills() { return ink.fills; }

    /** "42s" up to a minute, "4:59" beyond; cached. */
    static String time(int seconds) {
        int s = Math.max(0, Math.min(1800, seconds));
        String t = TIMES[s];
        if (t == null) t = TIMES[s] = s < 60 ? s + "s" : (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
        return t;
    }

    public void paint(Canvas cv, GuiTheme th, GuiState st, int px, int py) {
        float intro = GuiAnim.intro(st.introAge);
        float master = GuiAnim.ramp(intro, 0f, 0.3f);
        if (master <= 0.01f) return;
        ink.begin(cv, px, py, master);
        if (intro < 0.4f) {                                             // the panel unfolds from its middle line
            int half = (int) (H / 2f * Math.min(1f, GuiAnim.easeOutBack(intro / 0.4f)));
            ink.clipRows(H / 2 - half, H / 2 + half);
        }
        float en = st.energy();
        float at = st.time * th.tune.tempo() * th.motif.tempo;
        float reveal = st.reveal();
        GuiPalette p = th.pal;

        GuiBackdrop.back(ink, th, st, at, en);
        GuiMedallion.disc(ink, th, st, at, en);
        GuiMedallion.runes(ink, th, st, at, en);
        GuiAccents.paint(ink, th, at, en, 0.25f + 0.75f * GuiAnim.ramp(reveal, 0.1f, 0.7f));
        GuiMedallion.emblem(ink, th, st, at, en, reveal);
        GuiMedallion.timerRing(ink, th, st, at, en);
        GuiParticles.paint(ink, th, at, en);
        content(th, st, at, en);
        GuiBackdrop.frame(ink, th, st, at, en);
        overlays(th, st, intro, at);
    }

    // ---------------------------------------------------------------- text and bars
    private void content(GuiTheme th, GuiState st, float at, float en) {
        Ink k = ink;
        GuiPalette p = th.pal;
        if (th.labelsFor != labels) {
            th.labelsFor = labels;
            th.nameLine = (th.dark ? labels.dark + " " : "") + th.spec.name();
            String full = th.spec.title() == null ? "" : th.spec.title(), t = full;
            int room = COL_W - k.textWidth(th.nameLine) - 8;
            while (!t.isEmpty() && k.textWidth(t) > room) t = t.substring(0, t.length() - 1);
            th.subLine = t.length() == full.length() ? t : "";
        }
        k.text(th.nameLine, COL_X, TITLE_Y, GuiColor.argb(p.text(), 1f));
        k.text(th.subLine, COL_X + k.textWidth(th.nameLine) + 8, TITLE_Y, GuiColor.argb(p.textDim(), 1f));

        // phase pips and the phase text
        for (int i = 1; i <= 4; i++) pip(k, th, st, COL_X + (i - 1) * 10, PIPS_Y + 1, i, at);
        int secs = (st.phase == 1 || st.phase >= 3) ? (int) ((st.timeLeft + 19f) / 20f) : -1;
        int key = Math.max(0, Math.min(4, st.phase)) * 10000 + secs + 1;
        if (key != phaseKey) {
            phaseKey = key;
            phaseText = secs >= 0 && st.phase != 4 ? labels.phase[Math.max(0, Math.min(4, st.phase))] + "  " + time(secs) : labels.phase[Math.max(0, Math.min(4, st.phase))];
        }
        k.text(phaseText, COL_X + 44, PIPS_Y, GuiColor.argb(st.phase > 0 ? p.glow() : p.textDim(), 1f));

        // bars
        GuiBars.draw(k, th, COL_X, MAGOI_Y, COL_W, MAGOI_H, st.magoi / 100f, p.barA(), p.barB(), at, en, true);
        GuiBars.draw(k, th, COL_X, ASSIM_Y, COL_W, ASSIM_H, st.assim / 1000f, GuiColor.mix(0x8A48E0, p.accent(), 0.25f), GuiColor.mix(0xC890FF, p.glow(), 0.25f), at, en, false);
        int ta = st.phase >= 4 ? 0xFF7060 : p.accent(), tb = st.phase >= 4 ? 0xFFF0E8 : p.glow();
        GuiBars.draw(k, th, COL_X, TIMER_Y, COL_W, TIMER_H, st.timerFrac(), ta, tb, at, en, true);
        GuiBars.draw(k, th, COL_X, LOCK_Y, COL_W, THIN_H, st.lockLeft / Math.max(1f, st.lockMax), 0xB84040, 0xFF8070, at, en, false);
        GuiBars.draw(k, th, COL_X, COOL_Y, COL_W, THIN_H, st.coolLeft / Math.max(1f, st.coolMax), GuiColor.mix(p.primary(), 0x808898, 0.5f), 0xE8ECF8, at, en, false);

        // footer: trust, then whatever is recovering
        if (st.trust != trustKey) { trustKey = st.trust; trustText = labels.trust + " " + st.trust; }
        k.text(trustText, COL_X, FOOT_Y, GuiColor.argb(p.textDim(), 1f));
        int ls = st.lockLeft > 0f ? (int) ((st.lockLeft + 19f) / 20f) : -1, cs = st.coolLeft > 0f ? (int) ((st.coolLeft + 19f) / 20f) : -1;
        int fk = (ls + 1) * 4000 + (cs + 1);
        if (fk != footKey) {
            footKey = fk;
            String lk = ls >= 0 ? labels.maso + " " + time(ls) : "", ck = cs >= 0 ? labels.extreme + " " + time(cs) : "";
            String both = lk.isEmpty() ? ck : ck.isEmpty() ? lk : lk + "  " + ck;
            footRight = k.textWidth(trustText) + k.textWidth(both) + 12 <= COL_W ? both : lk.isEmpty() ? ck : lk;
        }
        if (!footRight.isEmpty()) k.text(footRight, COL_X + COL_W - k.textWidth(footRight), FOOT_Y, GuiColor.argb(st.lockLeft > 0f ? 0xFF9080 : p.text(), 1f));
    }

    /** A 7 x 7 diamond: lit up to the current phase, the current one pulsing, in a Full Equip sparkling. */
    private static void pip(Ink k, GuiTheme th, GuiState st, int x, int y, int index, float at) {
        GuiPalette p = th.pal;
        boolean lit = index <= st.phase;
        float pulse = index == st.phase ? 0.65f + 0.35f * GuiAnim.sin(at * 0.25f) : 1f;
        int c = lit ? GuiColor.argb(index >= 4 ? 0xFFE0D0 : GuiColor.mix(p.primary(), p.glow(), 0.4f + 0.2f * index / 4f), pulse) : GuiColor.argb(p.track(), 1f);
        for (int r = 0; r < 7; r++) {
            int w = r < 4 ? 1 + 2 * r : 1 + 2 * (6 - r);
            k.fill(x + 3 - w / 2, y + r, x + 3 + w / 2 + 1, y + r + 1, c);
        }
        if (lit) k.dot(x + 3, y + 3, GuiColor.argb(0xFFFFFF, 0.55f * pulse));
        else k.dot(x + 3, y + 3, GuiColor.argb(p.rim(), 0.35f));
        if (lit && st.phase >= 3 && GuiAnim.rnd((int) (at / 5f) + index, 77) < 0.25f) k.star(x + 3, y + 3, 4, GuiColor.argb(p.glow(), 0.5f));
    }

    // ---------------------------------------------------------------- overlays
    private void overlays(GuiTheme th, GuiState st, float intro, float at) {
        Ink k = ink;
        GuiPalette p = th.pal;
        if (intro < 1f) {                                               // the summon: a flash on the medallion and a ring of light leaving it
            float f = GuiAnim.clamp01(1f - intro / 0.25f);
            if (f > 0f) k.disc(MED_CX, MED_CY, 28, GuiColor.argb(p.glow(), 0.5f * f));
            float r = 6f + intro * 80f, a = (1f - intro) * 0.8f;
            int n = Math.max(12, (int) (r * 0.9f));
            k.arc(MED_CX, MED_CY, r, r, 0f, GuiAnim.TAU / n, n, 2, GuiColor.argb(p.glow(), a));
            if (r > 8f) k.arc(MED_CX, MED_CY, r - 5f, r - 5f, 0.3f, GuiAnim.TAU / n, n, 1, GuiColor.argb(p.accent(), a * 0.6f));
        }
        float surge = st.prev != st.phase ? GuiAnim.clamp01(1f - st.phaseAge / 14f) : 0f;
        if (surge > 0.02f) {                                            // a phase change: a ring bursts out of the medallion
            float r = 30f + (1f - surge) * 46f;
            int n = Math.max(12, (int) (r * 0.9f));
            k.arc(MED_CX, MED_CY, r, r, 0f, GuiAnim.TAU / n, n, 2, GuiColor.argb(st.phase >= 3 ? p.glow() : p.rim(), surge * 0.8f));
        }
        if (st.pulse > 0.01f) k.fill(0, 0, W, H, GuiColor.argb(0xFFFFFF, st.pulse * 0.4f));            // an Extreme Magic strikes
    }
}
