package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;

/**
 * How True Lightning grows with the charge (pure, tested). The owner kept the real vanilla bolt OFF and asked to "just add the charge effect": a fuller hold makes the strike and the
 * lightning of the rune bigger. Three things scale, all inside hard caps, and nothing here spawns anything (the bolt entity, its caps and its safety rules are {@link TrueLightning}'s):
 * <ul>
 *   <li><b>the strike</b> (the visual bolt that comes down at the impact): 1, 2 or 3 jagged bolts by the stage of the charge, 8 to 22 blocks tall, 0.9 to 1.8 wide (brighter), 10 to 16 ticks
 *       long, the side bolts spread 0.6 to 1.5 blocks off the centre. A cast without a measured hold (the 3.4.0 spell) gets no extra strike;</li>
 *   <li><b>the radius</b>: the bolt's hit volume and its splash are x1.0 (tap) to x1.35 (full) bigger for the LIGHTNING element, inside the bolt state's own caps
 *       ({@link RuneCommands#MAX_HIT_RADIUS}, {@link RuneCommands#MAX_SPLASH});</li>
 *   <li><b>the arcs of the written rune</b> (lightning element only, the held rune): one arc once the hand ring is written, two once the middle ring is, three once the outer ring is; the
 *       arcs start on the outermost ring written so far (so they get longer), are thicker, wilder and brighter with each ring. The count never passes {@link RuneGlyphLayout#MAX_ARCS}.</li>
 * </ul>
 */
public final class LightningCharge {
    private LightningCharge() {}

    public static final int MAX_BOLTS = 3;
    public static final double MAX_HEIGHT = 24.0, MAX_WIDTH = 2.0, MAX_RADIUS_MULT = 1.4;
    public static final double BASE_HEIGHT = 8.0, EXTRA_HEIGHT = 14.0, BASE_WIDTH = 0.9, EXTRA_WIDTH = 0.9, EXTRA_RADIUS = 0.35, BASE_SPREAD = 0.6, EXTRA_SPREAD = 0.9;
    public static final int BASE_TICKS = 10, EXTRA_TICKS = 6;

    /** The visual strike of a charge: how many bolts, how tall, how wide (the effect's power), how many ticks, how far the side bolts stand from the centre. */
    public record Strike(int bolts, double height, float width, int ticks, double spread) {}

    /** Is this rune scaled by the charge (the lightning element, a charge that was measured)? */
    public static boolean scales(RuneSpec s, Charge c) { return s != null && c != null && !c.legacy() && s.element() == RuneElement.LIGHTNING; }

    /** x1.0 for a tap up to x{@code 1 + EXTRA_RADIUS} for a full hold; x1 for anything else. Always within [1, {@link #MAX_RADIUS_MULT}]. */
    public static double radiusMult(RuneSpec s, Charge c) {
        if (!scales(s, c)) return 1.0;
        return Math.min(MAX_RADIUS_MULT, 1.0 + EXTRA_RADIUS * RuneCost.level(c));
    }

    /** The strike of a charge. The cast without a measured hold is one bolt of the base size. */
    public static Strike strike(Charge c) {
        double l = RuneCost.level(c);
        int bolts = c == null || c.legacy() ? 1 : Math.max(1, Math.min(MAX_BOLTS, RuneCharge.tier(c.stage())));
        return new Strike(bolts, Math.min(MAX_HEIGHT, BASE_HEIGHT + EXTRA_HEIGHT * l), (float) Math.min(MAX_WIDTH, BASE_WIDTH + EXTRA_WIDTH * l), BASE_TICKS + (int) Math.round(EXTRA_TICKS * l), BASE_SPREAD + EXTRA_SPREAD * l);
    }

    /** The offset (x, z) of bolt {@code k} of a strike from the impact: the first stands at the centre, the others at 120 degrees round it. Writes two numbers. */
    public static void boltOffset(Strike s, int k, double[] out) {
        if (k <= 0) { out[0] = 0.0; out[1] = 0.0; return; }
        double a = 0.5 + k * (2.0 * Math.PI / 3.0);
        out[0] = Math.cos(a) * s.spread();
        out[1] = Math.sin(a) * s.spread();
    }

    // ---------------------------------------------------------------- the rune's own arcs
    /** How many rings the hold has written far enough to feed an arc: 0..3 (the hand ring, then the middle, then the outer ring, in that order). */
    public static int arcsReached(double hand, double middle, double outer) {
        if (!(hand > 0.5)) return 0;
        if (!(middle > 0.5)) return 1;
        return outer > 0.55 ? 3 : 2;
    }

    /** The ring an arc starts from (0 hand, 1 middle, 2 outer) when {@code reached} rings are written: the outermost one, so the arcs get longer. */
    public static int startRing(int reached) { return Math.max(0, Math.min(2, reached - 1)); }

    /** The brightness, thickness and wildness of the arcs with {@code reached} rings written (1..3). */
    public static double arcLight(int reached) { return reached <= 1 ? 0.55 : reached == 2 ? 0.78 : 1.0; }

    public static double arcWidth(int reached) { return reached <= 1 ? 1.0 : reached == 2 ? 1.4 : 1.8; }

    public static double arcJag(int reached) { return reached <= 1 ? 1.0 : reached == 2 ? 1.25 : 1.5; }
}
