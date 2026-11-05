package com.newuniverse.nusmp.beastwill;

import java.util.Map;

/**
 * Why (docs/solid_cores.md): the Beast Will Core is ONE solid 3D orb (neutral grey stone, white-hot fissures; tools/beastwill/gen_beast_core_model.py) and the colour of the will is a tint the renderer
 * multiplies in: this table is that colour per will id, the same value as the {@code tint} of each {@code data/nusmp/beast_will/<id>.json} (a unit test keeps them equal). The client has no will catalog of its
 * own (the data pack is loaded by the server), so it cannot read the data file: the core's stack carries the will id, and this class turns the id into a colour. A will this table does not know (a data-pack
 * will) gets a stable colour from its id's hash, so it never looks like another will. Pure, no Minecraft type, no allocation (a map lookup and integer maths): safe on both sides and every frame.
 */
public final class BeastCoreLook {
    private BeastCoreLook() {}

    /** The violet of the old flat icon: a bare stack (no will) and an empty id. */
    public static final int DEFAULT_TINT = 0x7E54D6;
    /** How far the body skin is pulled toward the will's colour (0 = untinted stone, 1 = the colour itself), and the same for the flat inventory icon. */
    public static final float BODY_MIX = 0.6f, ICON_MIX = 0.85f;

    private static final Map<String, Integer> TINT = tints(Map.of(
            "dragon_will", 8378111, "shadow_panther_will", 4930188, "elderwood_guardian_will", 3129201, "lion_will", 14722362, "thunder_fang_chimera_will", 5945599,
            "ridge_wolf_will", 10129280, "crested_boar_will", 9132587, "wind_skimmer_will", 10477768, "flame_serpent_will", 14701594),
            // the 18 later wills (docs/beastwill_new.md), appended: fenrir .. apophis
            "fenrir_will", 14275839, "jormungandr_will", 2005672, "cerberus_will", 10108880, "minotaur_will", 11880491, "hydra_will", 9418794, "cu_sith_will", 8191819, "nidhoggr_will", 12101674, "hafgufa_will", 3039952, "kraken_will", 948838, "typhon_will", 14695584, "scylla_charybdis_will", 8164272, "sphinx_will", 15126666, "pegasus_will", 16759000, "yamata_no_orochi_will", 14163516, "kitsune_will", 16751210, "nue_will", 5660782, "ammit_will", 11563146, "apophis_will", 7220464);

    private static Map<String, Integer> tints(Map<String, Integer> first, Object... more) {
        java.util.HashMap<String, Integer> all = new java.util.HashMap<>(first);
        for (int i = 0; i + 1 < more.length; i += 2) all.put((String) more[i], (Integer) more[i + 1]);
        return Map.copyOf(all);
    }

    /** The will's colour, 0xRRGGBB. */
    public static int tint(String will) {
        if (will == null || will.isEmpty()) return DEFAULT_TINT;
        Integer t = TINT.get(will);
        return t != null ? t : hue((will.hashCode() & 0x7FFFFFFF) % 360 / 360f);
    }

    /** The vertex colour of the body (0xAARRGGBB, opaque): white stone at 0, the will's colour at 1 (the skin is grey, so this tints stone and fissures alike). */
    public static int bodyArgb(String will) { return 0xFF000000 | towardWhite(tint(will), BODY_MIX); }

    /** The additive glow colour (0xRRGGBB): the will's colour itself. */
    public static int glowRgb(String will) { return tint(will); }

    /** The item colour of the flat icon (0xAARRGGBB, opaque). */
    public static int iconArgb(String will) { return 0xFF000000 | towardWhite(tint(will), ICON_MIX); }

    static int towardWhite(int rgb, float k) {
        int r = 255 - Math.round((255 - (rgb >> 16 & 255)) * k), g = 255 - Math.round((255 - (rgb >> 8 & 255)) * k), b = 255 - Math.round((255 - (rgb & 255)) * k);
        return r << 16 | g << 8 | b;
    }

    /** A bright, fairly saturated colour of a hue 0..1 (HSV, s = 0.6, v = 1). */
    static int hue(float h) {
        float s = 0.6f, f = h * 6f - (float) Math.floor(h * 6f), p = 1f - s, q = 1f - f * s, t = 1f - (1f - f) * s;
        float[] c;
        switch ((int) Math.floor(h * 6f) % 6) {
            case 0 -> c = new float[]{1f, t, p};
            case 1 -> c = new float[]{q, 1f, p};
            case 2 -> c = new float[]{p, 1f, t};
            case 3 -> c = new float[]{p, q, 1f};
            case 4 -> c = new float[]{t, p, 1f};
            default -> c = new float[]{1f, p, q};
        }
        return Math.round(c[0] * 255) << 16 | Math.round(c[1] * 255) << 8 | Math.round(c[2] * 255);
    }
}
