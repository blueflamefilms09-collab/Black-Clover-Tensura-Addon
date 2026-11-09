package com.newuniverse.nusmp.saiyan;

import java.util.Locale;

/**
 * Why: the numbers of the Saiyan Senses (3.65.0, Stage 2) with no Minecraft type, so they are unit tested. Four tiers with the spec's radii (16, 32, 64, 128 blocks) earned by use; a creature is
 * "detectable" from a distance that its stealth shortens (Ki Suppression, invisibility, sneaking all interfere); its energy signature is a class from faint to overwhelming, read from its power against the
 * sensing Saiyan's own Battle Power. The server sends only the few nearest entries (a bounded list), and the client draws them as a radar.
 */
public final class SaiyanSenseRules {
    private SaiyanSenseRules() {}

    public enum Tier {
        BEGINNER(16.0), INTERMEDIATE(32.0), ADVANCED(64.0), MASTER(128.0);

        public final double radius;

        Tier(double radius) { this.radius = radius; }

        public String key() { return "nusmp.saiyan.sense." + name().toLowerCase(Locale.ROOT); }

        public static Tier of(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }
    }

    /** Scans that earn each tier. */
    public static final int[] USES = {0, 30, 150, 500};
    /** The most entries one ping carries, and the most creatures looked at to make it (the work is bounded by these). */
    public static final int MAX_ENTRIES = 32, MAX_CANDIDATES = 160;
    /** An invisible creature is only felt this close; a suppressed one at a quarter of the radius; a sneaking one at half. */
    public static final double INVISIBLE_RANGE = 8.0, SUPPRESSED_SHARE = 0.25, SNEAK_SHARE = 0.5;

    public static Tier tierOf(int scans, int[] thresholds) {
        Tier t = Tier.BEGINNER;
        for (int i = 0; i < Tier.values().length && i < thresholds.length; i++) if (scans >= thresholds[i]) t = Tier.of(i);
        return t;
    }

    public static Tier tierOf(int scans) { return tierOf(scans, USES); }

    /** The radius the senses reach: the tier's, cut at the server's maximum. */
    public static double radius(Tier t, double maxRadius) { return Math.min(t.radius, Math.max(1.0, maxRadius)); }

    /** The distance from which a creature can be felt, after its stealth. */
    public static double reach(double radius, boolean suppressed, boolean invisible, boolean sneaking) {
        double r = radius;
        if (suppressed) r = Math.min(r, radius * SUPPRESSED_SHARE);
        if (sneaking) r = Math.min(r, radius * SNEAK_SHARE);
        if (invisible) r = Math.min(r, INVISIBLE_RANGE);
        return r;
    }

    public static boolean detectable(double distance, double radius, boolean suppressed, boolean invisible, boolean sneaking) { return distance <= reach(radius, suppressed, invisible, sneaking); }

    /** The energy-signature class of a creature of this power felt by a Saiyan of this Battle Power: 0 faint, 1 comparable, 2 strong, 3 dangerous, 4 overwhelming. */
    public static int signature(double power, double myBp) {
        if (!(myBp > 0.0) || !(power > 0.0)) return 0;
        double r = power / myBp;
        if (r < 0.25) return 0;
        if (r < 1.0) return 1;
        if (r < 4.0) return 2;
        if (r < 16.0) return 3;
        return 4;
    }

    /** A signed offset clamped into a short for the packet (a block is 8 units, so the range is about 4000 blocks and the precision an eighth of a block). */
    public static short pack(double blocks) { return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(blocks * 8.0))); }

    public static double unpack(short v) { return v / 8.0; }
}
