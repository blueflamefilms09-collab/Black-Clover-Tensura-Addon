package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.book.NoelleFormRules;

/**
 * Why: the hair of every Saiyan form and the 3D tail (docs/saiyan_hair.md) as pure data and pure maths, so the client painter ({@code SaiyanHairDraw}), the asset test and the CI self-test read one table.
 * Each form wears one of seven Blender hair shapes (entity_meshes/saiyan_hair_&lt;shape&gt;.txt, skins in textures/entity/boss_mesh/): the shape is shared where the anime's hair is the same, and the form's own
 * colour is multiplied into the neutral skin ({@link Hair#tint}) and added as a glow ({@link Hair#glow}, {@link Hair#glowK}), so Super Saiyan is gold, Super Saiyan 2 brighter and more jagged, Super Saiyan 3 a deeper
 * orange-gold with the mane to the knees, the Super Saiyan 4 forms a black-red mane, God red, Blue blue, Ultra Instinct silver and white. The tail is {@link #TAIL}: one mesh in three parts that swing about their own joints.
 * No Minecraft type here (the player model is reached only by the painter).
 *
 * <p>Cost (rule 11), per player per frame: nothing beyond {@link #FAR_BLOCKS} blocks; the full mesh up to {@link #FULL_BLOCKS} blocks, the decimated far copy (no glow pass) between; at most
 * {@link NoelleFormRules#MAX_FULL_PER_FRAME} players at full detail and {@link NoelleFormRules#MAX_FAR_PER_FRAME} at the far one ({@link NoelleFormRules#budgeted}). One hair is at most {@link #MAX_QUADS} quad lines
 * (body and glow copy together), the tail at most {@link #MAX_TAIL_QUADS}; worst case per frame is 4 x (hair + tail) at full detail plus 6 far copies. No server cost, no packet: the form code that already
 * travels in the Saiyan aura packet picks the hair.
 */
public final class SaiyanHair {
    private SaiyanHair() {}

    /** The seven hair shapes (the mesh id of one is {@code saiyan_hair_<shape>}). */
    public static final String[] SHAPES = {"base", "ss", "ss2", "ss3", "ss4", "god", "ui"};
    /** The tail mesh id (three parts: tail, wing_l, wing_r are its root, middle and tip segment: the names borrow the roles the mesh drawer routes). */
    public static final String TAIL = "saiyan_tail";
    /** The most quad lines (body and glow copy) a hair mesh or its far copy may hold, and a tail mesh. */
    public static final int MAX_QUADS = 1200, MAX_TAIL_QUADS = 700;
    /** Full detail up to this distance (blocks); the far copy beyond it; nothing beyond {@link #FAR_BLOCKS}. */
    public static final double FULL_BLOCKS = 24.0, FAR_BLOCKS = 48.0;

    /**
     * One form's hair: the mesh {@code shape}, the RGB multiplied into the skin ({@code tint}), the RGB and strength (0..1.5) of the additive glow, and whether the hair is lit by its own light ({@code lit}: the
     * golden, blue and silver hair glows in the dark; the black hair does not).
     */
    public record Hair(String id, String shape, int tint, int glow, float glowK, boolean lit) {
        public String meshId() { return SaiyanHair.meshId(shape); }
    }

    private static Hair h(String id, String shape, int tint, int glow, float glowK, boolean lit) { return new Hair(id, shape, tint, glow, glowK, lit); }

    private static final java.util.List<Hair> TABLE = new java.util.ArrayList<>();   // a plain list: a lookup is a short scan (no per-player map)

    static {
        //     form                               shape   tint      glow      k      lit
        TABLE.add(h("kaioken",                         "base", 0x1A1418, 0xE03020, 0.35f, false));
        TABLE.add(h("false_super_saiyan",              "ss",   0xD8C070, 0xE0B040, 0.25f, true));
        TABLE.add(h("super_saiyan",                    "ss",   0xFFD020, 0xFFD020, 0.55f, true));
        TABLE.add(h("super_saiyan_full_power",         "ss",   0xFFDC38, 0xFFE060, 0.65f, true));
        TABLE.add(h("super_saiyan_2",                  "ss2",  0xFFEA50, 0xFFF080, 0.75f, true));
        TABLE.add(h("super_saiyan_3",                  "ss3",  0xFFB818, 0xFFC820, 0.70f, true));
        TABLE.add(h("super_saiyan_4",                  "ss4",  0x2A1218, 0xC02828, 0.35f, false));
        TABLE.add(h("super_full_power_super_saiyan_4", "ss4",  0x2C1016, 0xE03838, 0.40f, false));
        TABLE.add(h("super_saiyan_4_limit_breaker",    "ss4",  0x301018, 0xFF4468, 0.50f, false));
        TABLE.add(h("super_saiyan_god",                "god",  0xE02828, 0xFF3030, 0.60f, true));
        TABLE.add(h("super_saiyan_blue",               "god",  0x48B8FF, 0x40B0FF, 0.60f, true));
        TABLE.add(h("perfected_super_saiyan_blue",     "god",  0x68D0FF, 0x80D8FF, 0.70f, true));
        TABLE.add(h("super_saiyan_blue_kaioken",       "god",  0x58A8FF, 0xFF4060, 0.60f, true));
        TABLE.add(h("ultra_instinct_sign",             "ui",   0xBCC8E0, 0xB0C8FF, 0.60f, true));
        TABLE.add(h("perfected_ultra_instinct",        "ui",   0xDCE6F8, 0xE8F0FF, 0.70f, true));
        TABLE.add(h("true_ultra_instinct",             "ui",   0xF4F8FF, 0xFFFFFF, 0.80f, true));
    }

    /** The hair of a form id, or null (Base keeps the player's own hair). */
    public static Hair of(String formId) {
        if (formId != null) for (Hair x : TABLE) if (x.id().equals(formId)) return x;
        return null;
    }

    public static java.util.List<Hair> all() { return java.util.Collections.unmodifiableList(TABLE); }

    /** The index of a shape in {@link #SHAPES}, or -1. */
    public static int shapeIndex(String shape) {
        for (int i = 0; i < SHAPES.length; i++) if (SHAPES[i].equals(shape)) return i;
        return -1;
    }

    public static String meshId(String shape) { return "saiyan_hair_" + shape; }

    public static String meshPath(String id) { return "assets/anime_multiverse/entity_meshes/" + id + ".txt"; }

    public static String lodPath(String id) { return "assets/anime_multiverse/entity_meshes/" + id + "_lod.txt"; }

    public static String skinPath(String id) { return "assets/anime_multiverse/textures/entity/boss_mesh/" + id + ".png"; }

    public static String glowPath(String id) { return "assets/anime_multiverse/textures/entity/boss_mesh/" + id + "_glow.png"; }

    /** The detail a distance asks for, before the crowd budget: nothing beyond 48 blocks, the far copy between 24 and 48, the full figure inside 24. */
    public static NoelleFormRules.Lod wanted(double dist2) {
        if (dist2 > FAR_BLOCKS * FAR_BLOCKS) return NoelleFormRules.Lod.NONE;
        return dist2 > FULL_BLOCKS * FULL_BLOCKS ? NoelleFormRules.Lod.FAR : NoelleFormRules.Lod.STANDARD;
    }

    // ------------------------------------------------------------------ the swap and the sway (t = ticks since the form began, left = ticks until it ends)
    /**
     * 0..1: how far the hair has grown. With the charge-up on, the hair changes at the flash (it snaps from the player's own hair to the form's over eight ticks, starting two ticks before the burst, like the
     * anime); with it off it is there at once. It shrinks away over the last eight ticks of the form.
     */
    public static float grow(float t, float left, boolean chargeUp) {
        float in = chargeUp ? smooth((t - (SaiyanLook.GATHER_TICKS - 2f)) / 8f) : 1f;
        return in * smooth(left / 8f);
    }

    /** The scale of the hair at growth g: it springs from 0.4 to 1, ahead of a straight line in between (by at most 0.12), exactly 0.4 at 0 and exactly 1 at 1. */
    public static float scale(float g) { return 0.4f + 0.6f * g + 0.12f * (float) Math.sin(g * Math.PI); }

    /** The slots the painter sways: the crown and the hanging locks of a hair, the three segments of the tail. */
    public static final int CROWN = 0, LOCKS = 1, TAIL_ROOT = 2, TAIL_MID = 3, TAIL_TIP = 4;

    private static final float[] AMP = {0.03f, 0.07f, 0.10f, 0.16f, 0.24f};

    /** The sway about the sideways axis (radians): a slow swing, and the hanging parts stream back when the player runs ({@code run} 0..1, the limb swing amount). */
    public static float swayX(int slot, float age, float run) {
        return AMP[slot] * (float) Math.sin(age * 0.11f - slot * 0.8f) + (slot == CROWN ? 0f : 0.35f * Math.min(run, 1f) * (slot - 0.5f) / 4f);
    }

    /** The sway about the front-back axis (radians). */
    public static float swayZ(int slot, float age) { return AMP[slot] * 0.8f * (float) Math.sin(age * 0.083f + 1.7f - slot * 0.9f); }

    /** How loosely the crown sways: 3 for the Ultra Instinct hair (in the series it floats as if weightless: ja Wikipedia, Goku), 1 for every other shape. */
    public static float loose(String shape) { return "ui".equals(shape) ? 3f : 1f; }

    private static float smooth(float x) { x = x < 0f ? 0f : (x > 1f ? 1f : x); return x * x * (3f - 2f * x); }
}
