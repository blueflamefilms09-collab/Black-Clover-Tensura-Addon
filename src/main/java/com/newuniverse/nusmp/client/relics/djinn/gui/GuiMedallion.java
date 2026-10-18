package com.newuniverse.nusmp.client.relics.djinn.gui;

import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_CX;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_CY;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_X;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_Y;

/**
 * The medallion: the disc, its breathing glow, the ring of turning runes, the live tail and halo, the emblem replayed from its runs (wings beating,
 * a shimmer sweeping across, the layers that belong to the phase fading in), the eyes, and the timer ring round the rim. Allocation free.
 */
final class GuiMedallion {
    private GuiMedallion() {}

    private static final float TAU = GuiAnim.TAU;
    private static final float DISC_R = 29.5f;
    /** Half width of the disc at each of its 60 rows. */
    private static final int[] HW = new int[MED];
    /** 3x3 rune glyphs (bit r*3+c), six per set, three sets. */
    private static final int[][] GLYPH = {
            {0b010111010, 0b111010010, 0b100100111, 0b101010101, 0b111101101, 0b010010111},
            {0b100010001, 0b011010110, 0b110011001, 0b010101010, 0b101111101, 0b001010100},
            {0b111100100, 0b010111101, 0b011110011, 0b110010011, 0b101010010, 0b111010111}};

    static {
        for (int r = 0; r < MED; r++) {
            float dy = r + 0.5f - MED / 2f;
            HW[r] = (int) Math.floor(Math.sqrt(Math.max(0f, DISC_R * DISC_R - dy * dy)));
        }
    }

    /** The disc with its rim and the breathing glow. */
    static void disc(Ink k, GuiTheme th, GuiState st, float at, float en) {
        GuiPalette p = th.pal;
        for (int r = 0; r < MED; r++) {
            int hw = HW[r], y = MED_Y + r;
            float d = Math.abs(r + 0.5f - MED / 2f) / (MED / 2f);
            k.fill(MED_CX - hw, y, MED_CX + hw, y + 1, GuiColor.argb(GuiColor.mix(p.mid(), p.deep(), d * d), 0.96f));
            int nb = r < MED / 2 ? (r == 0 ? 0 : HW[r - 1]) : (r == MED - 1 ? 0 : HW[r + 1]);
            int rimW = Math.max(1, hw - nb);
            int rc = GuiColor.argb(p.rim(), st.phase >= 3 ? 0.95f : 0.6f);
            k.fill(MED_CX - hw, y, MED_CX - hw + rimW, y + 1, rc);
            k.fill(MED_CX + hw - rimW, y, MED_CX + hw, y + 1, rc);
        }
        float br = GuiAnim.breathe(at, 46f, 0.04f, 0.12f) * (0.6f + 0.6f * en);
        k.disc(MED_CX, MED_CY - 2, 24, GuiColor.argb(p.primary(), br * 0.5f));
        k.disc(MED_CX, MED_CY - 2, 17, GuiColor.argb(p.primary(), br * 0.7f));
        k.disc(MED_CX, MED_CY - 2, 10, GuiColor.argb(p.glow(), br * 0.7f));
    }

    /** The ring of runes (and in a Full Equip a second one turning the other way). */
    static void runes(Ink k, GuiTheme th, GuiState st, float at, float en) {
        GuiPalette p = th.pal;
        int[] set = GLYPH[Math.floorMod(th.tune.rune(), 3)];
        float rot = at * 0.012f * th.tune.spin();
        float al = 0.30f + 0.30f * Math.min(1f, en);
        for (int i = 0; i < 12; i++) {
            float ang = rot + i * TAU / 12f;
            glyph(k, Math.round(MED_CX + GuiAnim.cos(ang) * 26.5f), Math.round(MED_CY + GuiAnim.sin(ang) * 26.5f), set[(i + th.tune.corner()) % 6], GuiColor.argb(p.rim(), al));
        }
        if (st.phase >= 3) {
            k.arc(MED_CX, MED_CY, 22.5f, 22.5f, -rot * 1.6f, TAU / 36f, 36, 1, GuiColor.argb(p.accent(), 0.35f));
            for (int i = 0; i < 6; i++) {
                float ang = -rot * 1.6f + i * TAU / 6f;
                k.box(Math.round(MED_CX + GuiAnim.cos(ang) * 22.5f) - 1, Math.round(MED_CY + GuiAnim.sin(ang) * 22.5f) - 1, 2, 2, GuiColor.argb(p.glow(), 0.8f));
            }
        }
    }

    private static void glyph(Ink k, int cx, int cy, int bits, int argb) {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                if ((bits >> (r * 3 + c) & 1) != 0) k.dot(cx - 1 + c, cy - 1 + r, argb);
    }

    /** Tail, wings, body, horns, halo and eyes. {@code reveal} 0..1 is how much of the Djinn this phase shows. */
    static void emblem(Ink k, GuiTheme th, GuiState st, float at, float en, float reveal) {
        GuiPalette p = th.pal;
        Emblem e = th.emblem;
        float intro = GuiAnim.intro(st.introAge);
        float scan = intro >= 1f ? 1000f : intro * (Emblem.GH + 8);
        float flash = st.pulse;
        float wingA = 0.10f + 0.90f * GuiAnim.ramp(reveal, 0.55f, 1f);
        float hornA = GuiAnim.ramp(reveal, 0.12f, 0.45f);
        float bodyB = 0.35f + 0.65f * GuiAnim.ramp(reveal, 0f, 0.3f);
        // ---- tail (behind everything)
        if (th.tail != com.newuniverse.nusmp.relics.djinn.Djinn.Look.Tail.NONE) tail(k, th, at, wingA, scan);
        // ---- wings
        float beat = reveal >= 0.95f ? GuiAnim.flap(at, 0.05f) : GuiAnim.breathe(at, 60f, 0.25f, 0.45f);
        int f = GuiAnim.frame(beat, Emblem.FRAMES);
        int[] wr = e.wings[f];
        for (int i = 0; i < wr.length; i++) run(k, th, wr[i], at, en, wingA, bodyB, flash, scan);
        // ---- body, ears and horns
        for (int i = 0; i < e.body.length; i++) {
            int r = e.body[i];
            run(k, th, r, at, en, Emblem.part(r) == Emblem.P_HORN ? hornA : 1f, bodyB, flash, scan);
        }
        // ---- halo
        if (th.halo != com.newuniverse.nusmp.relics.djinn.Djinn.Look.Halo.NONE) halo(k, th, at, en, 0.15f + 0.85f * GuiAnim.ramp(reveal, 0.5f, 1f), scan);
        // ---- eyes
        if (scan > Emblem.HEAD_Y) {
            int blink = (int) (at % 90f);
            float ea = (blink < 3 ? 0.2f : 1f) * (0.25f + 0.75f * GuiAnim.ramp(reveal, 0f, 0.4f));
            int ey = MED_Y + Emblem.HEAD_Y - 1, c = th.dark ? p.accent() : p.glow();
            k.fill(MED_X + Emblem.HEAD_X - 4, ey - 1, MED_X + Emblem.HEAD_X - 1, ey + 2, GuiColor.argb(c, 0.22f * ea));
            k.fill(MED_X + Emblem.HEAD_X + 1, ey - 1, MED_X + Emblem.HEAD_X + 4, ey + 2, GuiColor.argb(c, 0.22f * ea));
            k.fill(MED_X + Emblem.HEAD_X - 3, ey, MED_X + Emblem.HEAD_X - 1, ey + 1, GuiColor.argb(0xFFFFFF, ea));
            k.fill(MED_X + Emblem.HEAD_X + 1, ey, MED_X + Emblem.HEAD_X + 3, ey + 1, GuiColor.argb(0xFFFFFF, ea));
        }
        // ---- the scan line of the intro
        if (intro < 1f) {
            int sy = MED_Y + (int) scan;
            k.fill(MED_X + 4, sy, MED_X + MED - 4, sy + 1, GuiColor.argb(p.glow(), 0.9f));
            k.fill(MED_X + 4, sy - 1, MED_X + MED - 4, sy, GuiColor.argb(p.glow(), 0.3f));
        }
    }

    private static void run(Ink k, GuiTheme th, int r, float at, float en, float partA, float bodyB, float flash, float scan) {
        int y = Emblem.y(r);
        if (y > scan || partA <= 0.02f) return;
        GuiPalette p = th.pal;
        int x = Emblem.x(r), len = Emblem.len(r), part = Emblem.part(r), edge = Emblem.edge(r);
        float yf = y / (float) Emblem.GH;
        float sh = 0.5f + 0.5f * GuiAnim.sin((x + len / 2f + y) * 0.28f - at * 0.18f);
        int c;
        float a = partA;
        switch (part) {
            case Emblem.P_HORN -> c = GuiColor.mix(GuiColor.mix(p.primary(), p.glow(), 0.35f + 0.4f * (1f - yf)), p.glow(), edge * 0.4f + 0.2f * sh * en);
            case Emblem.P_EAR -> c = GuiColor.mix(GuiColor.mix(p.deep(), p.primary(), 0.45f), p.rim(), edge * 0.5f);
            case Emblem.P_WING -> {
                c = GuiColor.mix(GuiColor.scale(p.primary(), 0.55f), p.primary(), 0.3f + 0.7f * sh * (0.4f + 0.6f * en));
                if (edge == 1) c = GuiColor.mix(c, p.glow(), 0.55f);
            }
            case Emblem.P_WING2 -> c = GuiColor.mix(GuiColor.mix(p.accent(), p.glow(), 0.45f), 0xFFFFFF, 0.1f + 0.2f * sh * en);
            default -> {
                c = GuiColor.mix(GuiColor.mix(0x141824, p.primary(), 0.20f + 0.14f * yf), p.glow(), 0.16f * sh * (0.3f + 0.7f * en));
                if (edge == 1) c = GuiColor.mix(c, p.rim(), 0.7f);
                c = GuiColor.scale(c, bodyB);
            }
        }
        if (flash > 0.01f) c = GuiColor.mix(c, 0xFFFFFF, 0.7f * flash);
        k.fill(MED_X + x, MED_Y + y, MED_X + x + len, MED_Y + y + 1, GuiColor.argb(c, a));
    }

    private static void tail(Ink k, GuiTheme th, float at, float a, float scan) {
        GuiPalette p = th.pal;
        float wave = th.tune.tempo();
        final int n = 24;
        for (int i = 0; i < n; i++) {
            float s = i / (float) (n - 1);
            int gx = 35 + Math.round(16f * s), gy = 52 - Math.round(14f * s * s + GuiAnim.sin(s * 5f - at * 0.2f * wave) * 3.2f * s);
            float dx = gx + 0.5f - 30f, dy = gy + 0.5f - 30f;
            if (dx * dx + dy * dy > 28f * 28f || gy > scan) continue;
            int size = s < 0.25f ? 3 : s < 0.65f ? 2 : 1;
            int c;
            switch (th.tail) {
                case FLAME -> c = GuiColor.mix(p.accent(), p.glow(), (1f - s) * 0.8f * (0.6f + 0.4f * GuiAnim.noise(i * 0.7f + at * 0.4f, 9)));
                case BLADE -> c = GuiColor.mix(p.primary(), p.glow(), s);
                case TENDRIL -> c = GuiColor.mix(GuiColor.scale(p.primary(), 0.8f), p.accent(), s);
                default -> c = GuiColor.mix(p.primary(), p.glow(), s * 0.8f);
            }
            k.box(MED_X + gx - size / 2, MED_Y + gy - size / 2, size, size, GuiColor.argb(c, a * (i == n - 1 ? 1f : 0.92f)));
            if (i == n - 1) {
                int tx = MED_X + gx, ty = MED_Y + gy, tc = GuiColor.argb(p.glow(), a);
                switch (th.tail) {
                    case BLADE -> { k.dot(tx + 1, ty - 1, tc); k.dot(tx + 1, ty + 1, tc); k.box(tx + 1, ty, 2, 1, tc); }
                    case TENDRIL -> k.arc(tx, ty - 2, 2.5f, 2.5f, at * 0.15f, TAU / 6f, 6, 1, tc);
                    case FLAME -> { k.dot(tx, ty - 2, tc); k.dot(tx + (at % 8 < 4 ? 1 : -1), ty - 3, GuiColor.fade(tc, 0.7f)); }
                    default -> k.dot(tx + 1, ty - 1, tc);
                }
            }
        }
    }

    private static void halo(Ink k, GuiTheme th, float at, float en, float a, float scan) {
        GuiPalette p = th.pal;
        float cx = MED_X + Emblem.HEAD_X, cy = MED_Y + Emblem.HEAD_Y - 15f, rx = 11.5f, ry = 3.6f;
        float spin = th.tune.spin() * at * 0.03f;
        switch (th.halo) {
            case RUNES -> {
                int[] set = GLYPH[Math.floorMod(th.tune.rune(), 3)];
                k.arc(cx, cy, rx, ry, 0f, TAU / 24f, 24, 1, GuiColor.argb(p.accent(), 0.3f * a));
                for (int i = 0; i < 8; i++) {
                    float ang = spin + i * TAU / 8f;
                    boolean front = GuiAnim.sin(ang) > 0f;
                    glyph(k, Math.round(cx + GuiAnim.cos(ang) * rx), Math.round(cy + GuiAnim.sin(ang) * ry), set[i % 6], GuiColor.argb(p.glow(), (front ? 0.95f : 0.45f) * a));
                }
            }
            case SPOKES -> {
                k.arc(cx, cy, rx, ry, 0f, TAU / 28f, 28, 1, GuiColor.argb(p.rim(), 0.6f * a));
                for (int i = 0; i < 10; i++) {
                    float ang = spin + i * TAU / 10f;
                    float ca = GuiAnim.cos(ang), sa = GuiAnim.sin(ang);
                    k.line(Math.round(cx + ca * rx), Math.round(cy + sa * ry), Math.round(cx + ca * (rx + 3.5f)), Math.round(cy + sa * (ry + 1.8f)),
                            GuiColor.argb(p.glow(), (sa > 0f ? 0.95f : 0.5f) * a));
                }
            }
            default -> {
                for (int i = 0; i < 28; i++) {
                    float ang = i * TAU / 28f;
                    float sweep = GuiAnim.tri(i / 28f - spin / TAU);
                    k.box(Math.round(cx + GuiAnim.cos(ang) * rx), Math.round(cy + GuiAnim.sin(ang) * ry), 1, 1,
                            GuiColor.argb(GuiColor.mix(p.rim(), p.glow(), sweep), (0.4f + 0.55f * sweep * sweep) * a * (GuiAnim.sin(ang) > 0f ? 1f : 0.6f)));
                }
            }
        }
    }

    /** The timer ring: 72 dots round the rim, lit for the fraction left (the Extreme Magic charge fills towards white, the untimed Half Equip chases). */
    static void timerRing(Ink k, GuiTheme th, GuiState st, float at, float en) {
        GuiPalette p = th.pal;
        final int n = 72;
        float frac = st.timerFrac();
        int lit = Math.round(frac * n);
        int onC = st.phase >= 4 ? 0xFFE8E0 : p.glow();
        for (int i = 0; i < n; i++) {
            float ang = -(float) Math.PI / 2f + i * TAU / n;
            int x = Math.round(MED_CX + GuiAnim.cos(ang) * 31f), y = Math.round(MED_CY + GuiAnim.sin(ang) * 31f);
            if (st.phase == 2) {                                            // untimed: a bright dot chases round
                float d = GuiAnim.frac(i / (float) n - at * 0.012f * th.tune.spin());
                k.dot(x, y, GuiColor.argb(d < 0.12f ? onC : p.rim(), d < 0.12f ? 0.95f : 0.22f));
            } else if (i < lit) {
                float head = i == lit - 1 ? 1f : 0.7f;
                k.dot(x, y, GuiColor.argb(onC, head));
                if (i == lit - 1 && st.phase > 0) k.box(x - 1, y - 1, 3, 3, GuiColor.argb(onC, 0.25f + 0.2f * GuiAnim.sin(at * 0.4f)));
            } else {
                k.dot(x, y, GuiColor.argb(p.rim(), 0.16f));
            }
        }
    }
}
