package com.newuniverse.nusmp.beastwill;

/**
 * Why: the animal spirit a Beast Will form shows (3.57.0) is chosen on the client from the one int the aura lease already carries ({@code PlayerAuras.set(.., style)}), so no new packet and no copy of the
 * data files is needed. The old style was {@code (phase << 24) | tint}, with the phase only ever 1 or 2, so bits 26..29 were free: they now hold the look KIND (0 = unknown, 1..9 = the nine shipped wills; up
 * to 15 fit). Bits 24..25 stay the phase, bits 0..23 the tint, so a style written by the old code (kind bits 0) decodes to "unknown kind" and keeps the old look. The kind comes from the will's id, so a
 * will added by a data pack that is not listed here simply keeps the old look. Pure (no game classes): the server writes it, the client reads it, a unit test pins it.
 */
public final class BeastWillLook {
    private BeastWillLook() {}

    public static final int UNKNOWN = 0, DRAGON = 1, PANTHER = 2, ELDERWOOD = 3, LION = 4, WOLF = 5, BOAR = 6, RAPTOR = 7, SERPENT = 8, CHIMERA = 9, COUNT = 10;

    /** The texture stem of each kind ({@code particle/bwspirit_<stem>_a.png} is the body, {@code _b} the accent layer). Index = kind. */
    public static final String[] STEM = {"", "dragon", "panther", "elder", "lion", "wolf", "boar", "raptor", "serpent", "chimera"};

    /** The look kind of a will id, or {@link #UNKNOWN}. */
    public static int kindOf(String willId) {
        if (willId == null) return UNKNOWN;
        return switch (willId) {
            case "dragon_will" -> DRAGON;
            case "shadow_panther_will" -> PANTHER;
            case "elderwood_guardian_will" -> ELDERWOOD;
            case "lion_will" -> LION;
            case "ridge_wolf_will" -> WOLF;
            case "crested_boar_will" -> BOAR;
            case "wind_skimmer_will" -> RAPTOR;
            case "flame_serpent_will" -> SERPENT;
            case "thunder_fang_chimera_will" -> CHIMERA;
            default -> UNKNOWN;
        };
    }

    /** The aura style: phase (1 Acquire, 2 Integrate) in bits 24..25, the look kind in bits 26..29, the tint (0xRRGGBB) below. */
    public static int style(int phase, int kind, int tint) {
        return ((phase & 3) << 24) | ((kind & 15) << 26) | (tint & 0xFFFFFF);
    }

    public static int phase(int style) { return (style >>> 24) & 3; }

    /** 0 when the style carries no (or an unlisted) kind: the painter then keeps the old look. */
    public static int kind(int style) {
        int k = (style >>> 26) & 15;
        return k < COUNT ? k : UNKNOWN;
    }

    public static int tint(int style) { return style & 0xFFFFFF; }
}
