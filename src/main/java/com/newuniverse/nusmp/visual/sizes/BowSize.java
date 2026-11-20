package com.newuniverse.nusmp.visual.sizes;

import java.util.Map;

/**
 * Why (docs/heroweapon/bow_size.md): owner, "the hero bow or whatever should be bigger in the player's hand as it is tiny, and that applies to all the bows in my mod". Since 1.72.0
 * every bow mesh is placed so that it is the size of vanilla's bow (tools/relics/bow_vanilla.py, 1.2 blocks tip to tip, 1.08 in a third-person hand), which is what read as tiny next to a
 * "hero" weapon. The meshes and their place lines are left as they are (the classic path); the renderer multiplies a held bow by the factor of this class about the point the hand holds.
 *
 * <p>Pure (no Minecraft class, so the unit tests and the dedicated server's self-test read it): the table of every bow of the mod with the length it should read at in a third-person hand,
 * the factor that gets a mesh there, the first-person cap that keeps the bow off the camera, and the two client switches ({@code visual.bigBows}, {@code visual.bowScale} of
 * {@code vfx/client/FlatPropsConfig}'s file, default on / 1.0). Off = factor 1 exactly = the sizes of before.
 *
 * <p>Lengths are the APPARENT length in blocks, the mesh's tip-to-tip length (the {@code place} scale times its height) times the display scale of the bow's model json
 * ({@link #TP_SCALE}, {@link #FP_SCALE}: vanilla's Bow display, which BowDisplayGuardTest enforces on every bow). A vanilla bow reads 1.08 / 0.82.
 *
 * <p>Cost: one table lookup the first time a mesh is drawn (cached in the mesh), then a multiply per frame; no allocation, no state per player, nothing is sent over the network.
 */
public final class BowSize {
    private BowSize() {}

    /** Vanilla's Bow display scale in a third-person / first-person hand (the model json of every class-bow item carries it; BowDisplayGuardTest). */
    public static final float TP_SCALE = 0.9f, FP_SCALE = 0.68f;
    /** The longest apparent first-person length a bow may reach (the number the camera allows; vanilla's bow is 0.82, a hero bow's target is 1.13). */
    public static final float FP_MAX_APPARENT = 1.2f;
    /** The most a bow is ever enlarged (a mesh far below its target is a data error, not something to blow up). */
    public static final float MAX_FACTOR = 2.5f;
    /** Range of the user multiplier {@code visual.bowScale}. */
    public static final float SCALE_MIN = 0.5f, SCALE_MAX = 2.0f;

    /** The tiers and the apparent third-person length (blocks) a bow of the tier is brought to; {@code min / max} is the band the tests hold it to. */
    public enum Tier {
        /** The Legendary Bow itself and its curse look (the hero weapon): 1.3 to 1.6. */
        HERO(1.50f, 1.30f, 1.60f),
        /** The nine bow forms of the Legendary Bow (vassal forms): 1.2 to 1.5. */
        FORM(1.40f, 1.20f, 1.50f),
        /** The relic bows (Arash's, Galewing, Aeon, Briar, Greed, God weapons): 1.2 to 1.5. */
        RELIC(1.35f, 1.20f, 1.50f);

        public final float target, min, max;

        Tier(float target, float min, float max) { this.target = target; this.min = min; this.max = max; }
    }

    /** Every bow of the mod (class "bow" of weapon_classes.json, its curse look and its forms): the rest id, never a draw stage. A bow that is missing here is not enlarged and BowSizeTest fails. */
    private static final Map<String, Tier> TABLE = Map.ofEntries(
            Map.entry("legendary_bow", Tier.HERO), Map.entry("legendary_bow_curse", Tier.HERO),
            Map.entry("legendary_bow__arrow_shower", Tier.FORM), Map.entry("legendary_bow__bind_arrow", Tier.FORM), Map.entry("legendary_bow__bird_hunting", Tier.FORM),
            Map.entry("legendary_bow__eagle_piercer", Tier.FORM), Map.entry("legendary_bow__frozen_rain", Tier.FORM), Map.entry("legendary_bow__hunters_bow", Tier.FORM),
            Map.entry("legendary_bow__meteor_bow", Tier.FORM), Map.entry("legendary_bow__sagittarius", Tier.FORM), Map.entry("legendary_bow__scouts_bow", Tier.FORM),
            Map.entry("arashs_stella_bow", Tier.RELIC), Map.entry("silver_eagles_galewing_bow", Tier.RELIC), Map.entry("starforged_aeon_bow", Tier.RELIC),
            Map.entry("thornmothers_briar_bow", Tier.RELIC), Map.entry("black_sword_greed_bow", Tier.RELIC),
            Map.entry("divine_mercy", Tier.RELIC), Map.entry("the_dragons_spirit", Tier.RELIC), Map.entry("the_reaver", Tier.RELIC),
            Map.entry("embrasure", Tier.RELIC), Map.entry("spirit_of_boreas", Tier.RELIC));

    private static volatile boolean big = true;
    private static volatile float scale = 1f;

    /** True unless the player turned {@code visual.bigBows} off (then every bow is exactly the size of before). Default true. */
    public static boolean enabled() { return big; }

    /** The user multiplier {@code visual.bowScale} (0.5 to 2.0, default 1.0), applied on top of the table's sizes when {@link #enabled}. */
    public static float userScale() { return scale; }

    /** Written by the client config only. */
    public static void setBig(boolean on) { big = on; }

    /** Written by the client config only; clamped to {@link #SCALE_MIN}..{@link #SCALE_MAX}, a NaN is 1. */
    public static void setScale(double s) { scale = s != s ? 1f : (float) Math.max(SCALE_MIN, Math.min(SCALE_MAX, s)); }

    /** The ids of the table (for the tests and the self-test). */
    public static java.util.Set<String> ids() { return TABLE.keySet(); }

    /** The rest id of a draw stage mesh ({@code <id>_pull1..3} to {@code <id>}); any other id is returned as it is. */
    public static String restId(String id) {
        int n = id.length();
        if (n > 6 && id.charAt(n - 6) == '_' && id.startsWith("pull", n - 5) && id.charAt(n - 1) >= '1' && id.charAt(n - 1) <= '3') return id.substring(0, n - 6);
        return id;
    }

    /** The tier of a bow mesh id (rest or draw stage), null when it is not a bow of the table. */
    public static Tier tier(String id) { return TABLE.get(restId(id)); }

    /** The multiplier (at least 1, at most {@link #MAX_FACTOR}) that brings a rest mesh of {@code restLength} blocks (tip to tip, before any display scale) to the tier's apparent third-person length; 0 = not a bow of the table. */
    public static float baseFactor(String id, float restLength) {
        Tier t = tier(id);
        if (t == null) return 0f;
        if (!(restLength > 0f)) return 1f;
        return Math.min(MAX_FACTOR, Math.max(1f, t.target / (restLength * TP_SCALE)));
    }

    /**
     * The multiplier to draw a bow mesh with in one hand under the current settings ({@link #factor(boolean, float, float, float, boolean)}).
     */
    public static float factor(float base, float meshLength, boolean firstPerson) { return factor(big, scale, base, meshLength, firstPerson); }

    /**
     * The multiplier to draw a bow mesh with in one hand. {@code base} is {@link #baseFactor} of its rest mesh (0 = no bow: 1), {@code meshLength} its own length. {@code on} false: exactly 1.
     * On: base times the user's scale; in first person never beyond {@link #FP_MAX_APPARENT} apparent blocks (the bow stays off the camera and the screen).
     */
    public static float factor(boolean on, float userScale, float base, float meshLength, boolean firstPerson) {
        if (!on || !(base > 0f)) return 1f;
        float k = base * userScale;
        if (firstPerson && meshLength > 0f) k = Math.min(k, Math.max(1f, FP_MAX_APPARENT / (meshLength * FP_SCALE)));
        return k;
    }

    /** The apparent length (blocks) of a bow whose rest mesh is {@code restLength} and whose drawn mesh is {@code meshLength}, with the settings given; the number the tests and the previews use. */
    public static float apparent(String id, float restLength, float meshLength, boolean firstPerson, boolean on, float userScale) {
        return meshLength * factor(on, userScale, baseFactor(id, restLength), meshLength, firstPerson) * (firstPerson ? FP_SCALE : TP_SCALE);
    }
}
