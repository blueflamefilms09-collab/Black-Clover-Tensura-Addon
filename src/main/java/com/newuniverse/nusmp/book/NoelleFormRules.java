package com.newuniverse.nusmp.book;

/**
 * Why: the pure rules of Noelle Silva's armour forms (docs/magic/noelle_forms.md), shared by the server (which form is put on, how long it may last, what it gives) and the client painter
 * (which look to draw, how much of it, at what distance). No game classes: everything here is unit tested (NoelleFormRulesTest).
 *
 * <p><b>The style number.</b> The Water armour is the render layer {@code Aura.WATER}; its style byte (the armour policy's page style) carries two things: the FORM in the low four bits and the
 * TIER in bit 4. Forms: 0 = Sea Dragon's Scale Mail (the plain water armour, unchanged), 1 = Valkyrie Dress (remade: the Saint Valkyrie Dress at the character tier), 2 = Mermaid Form,
 * 3 = Sea Dragon God Form, 4 = Black Dragon Form. The CHARACTER tier is the full-quality look: it is chosen by the server when the caster's bound grimoire is Noelle Silva's (the canon book); every other
 * Water mage gets the standard tier (the same silhouettes with fewer pieces and fewer effects). The old styles 0 and 1 keep their numbers, so a lease that was running before the update still draws.
 *
 * <p><b>Cost.</b> {@link #recipe} is the whole per-frame budget of a form at a level of detail: how many ribbon strips, droplets, rings, … the painter may draw. {@link #quads} turns a recipe into
 * a quad count, and the tests prove the worst case stays under {@link #QUAD_CAP}. The geo pieces are counted by the preview tool (vertices), see the doc.
 */
public final class NoelleFormRules {
    private NoelleFormRules() {}

    /** Append only: the low four bits of the style. */
    public enum Form { ARMOUR, VALKYRIE, MERMAID, SEA_DRAGON, BLACK_DRAGON }

    public static final int FORM_MASK = 15, CHARACTER_BIT = 16;

    /** The page ids of the three appended forms (WaterBook). */
    public static final String PAGE_VALKYRIE = "valkyrie_dress", PAGE_MERMAID = "mermaid_form", PAGE_DRAGON = "sea_dragon_god_form", PAGE_BLACK = "black_dragon_form";

    // ------------------------------------------------------------------ style
    /** The style byte of a form at a tier. */
    public static int style(Form f, boolean character) { return f.ordinal() | (character ? CHARACTER_BIT : 0); }

    /** The form of a style byte; anything out of range reads as the plain armour (never throws: a bad byte from the network must not crash a renderer). */
    public static Form form(int style) {
        int f = style & FORM_MASK;
        Form[] all = Form.values();
        return f < all.length ? all[f] : Form.ARMOUR;
    }

    public static boolean character(int style) { return (style & CHARACTER_BIT) != 0; }

    /** True for the styles the remade painter draws: every form but the plain Sea Dragon's Scale Mail (style 0). */
    public static boolean remade(int style) { return form(style) != Form.ARMOUR; }

    /** The form a page puts on (null: not a form page). */
    public static Form formOfPage(String pageId) {
        if (pageId == null) return null;
        return switch (pageId) {
            case PAGE_VALKYRIE -> Form.VALKYRIE;
            case PAGE_MERMAID -> Form.MERMAID;
            case PAGE_DRAGON -> Form.SEA_DRAGON;
            case PAGE_BLACK -> Form.BLACK_DRAGON;
            default -> null;
        };
    }

    /** Who may open the page: the Mermaid Form and the Valkyrie Dress are Water Magic's (the water spirit lends them to any Water mage); the two dragon forms are Noelle's grimoire only. */
    public static boolean pageOpen(String pageId, boolean noelleGrimoire) {
        Form f = formOfPage(pageId);
        return f != Form.SEA_DRAGON && f != Form.BLACK_DRAGON || noelleGrimoire;
    }

    // ------------------------------------------------------------------ the models of a form (the single list the painter, the tests and the self-test agree on)
    /** The model key of a form's pieces: assets/nusmp/geo/entity/&lt;key&gt;_arm_&lt;piece&gt;.geo.json (tools/gen_noelle_forms_geo.py). Style 0 has none. */
    public static String key(Form f) {
        return switch (f) {
            case VALKYRIE -> "nvl";
            case MERMAID -> "nml";
            case SEA_DRAGON -> "ndg";
            case BLACK_DRAGON -> "nbk";
            default -> "";
        };
    }

    private static final String[] VAL_STD = {"helm", "chest", "skirt", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "gauntlet_r", "gauntlet_l", "greave_r", "greave_l", "wings", "halo"},
            VAL_CHAR = {"wings2", "aureole", "sash", "lance"},
            MER_STD = {"crown", "hair", "bust", "train", "veil", "leg_r", "leg_l", "vambrace_r", "vambrace_l", "trident"}, MER_CHAR = {"dorsal", "scarf", "coral"},
            DRG_STD = {"helm", "chest", "tassets", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "greave_r", "greave_l", "wings", "tail", "cannon_r"},
            DRG_CHAR = {"ridge", "horns2", "wings2", "cannon_l"}, NO_PIECES = {};

    /** The pieces every player in the form wears (the standard tier). */
    public static String[] standardPieces(Form f) {
        return switch (f) {
            case VALKYRIE -> VAL_STD.clone();
            case MERMAID -> MER_STD.clone();
            case SEA_DRAGON, BLACK_DRAGON -> DRG_STD.clone();
            default -> NO_PIECES.clone();
        };
    }

    /** The extra pieces of the character tier (Noelle's grimoire). */
    public static String[] characterPieces(Form f) {
        return switch (f) {
            case VALKYRIE -> VAL_CHAR.clone();
            case MERMAID -> MER_CHAR.clone();
            case SEA_DRAGON, BLACK_DRAGON -> DRG_CHAR.clone();
            default -> NO_PIECES.clone();
        };
    }

    /** The sprite and skin textures the painter draws (assets/nusmp/textures/aura/&lt;name&gt;.png; the shells also have a _glow map). */
    public static final String[] SPRITES = {"noelle_ring", "noelle_ribbon", "noelle_feather", "noelle_ember", "noelle_eye", "noelle_bubble", "noelle_spark", "noelle_dot", "noelle_haze",
            "noelle_sigil_val", "noelle_sigil_mer", "noelle_sigil_drg", "noelle_sigil_blk", "noelle_val_shell", "noelle_mer_shell", "noelle_drg_shell", "noelle_blk_shell"};

    // ------------------------------------------------------------------ level of detail (client)
    /** What the painter draws for a player at a distance: NONE beyond {@link #FAR_BLOCKS}; FAR = only the big pieces and the glow; STANDARD = every standard piece; CHARACTER = + the character pieces. */
    public enum Lod { NONE, FAR, STANDARD, CHARACTER }

    public static final double DETAIL_BLOCKS = 24.0, FAR_BLOCKS = 64.0;

    /**
     * The level of detail from the SQUARED distance to the camera (no square root per frame). Up to {@link #DETAIL_BLOCKS}: every piece and every effect of the tier (a standard-tier player is
     * never above STANDARD); up to {@link #FAR_BLOCKS}: only the big standard pieces and the glow, no ribbons, rings, drops or skin shells; beyond: nothing.
     */
    public static Lod lod(double dist2, boolean characterTier) {
        if (dist2 > FAR_BLOCKS * FAR_BLOCKS) return Lod.NONE;
        if (dist2 > DETAIL_BLOCKS * DETAIL_BLOCKS) return Lod.FAR;
        return characterTier ? Lod.CHARACTER : Lod.STANDARD;
    }

    /** The most players per frame that may be drawn at the CHARACTER level, and at the STANDARD level or above (the nearest-first budget of the painter: the rest fall back one level). */
    public static final int MAX_CHARACTER_PER_FRAME = 2, MAX_FULL_PER_FRAME = 6;

    /** The level a player gets when {@code charDrawn} / {@code fullDrawn} players were already drawn this frame at CHARACTER / at STANDARD or above: a crowd costs a bounded number of full figures. */
    public static Lod budgeted(Lod wanted, int charDrawn, int fullDrawn) {
        Lod l = wanted;
        if (l == Lod.CHARACTER && charDrawn >= MAX_CHARACTER_PER_FRAME) l = Lod.STANDARD;
        if (l == Lod.STANDARD && fullDrawn >= MAX_FULL_PER_FRAME) l = Lod.FAR;
        return l;
    }

    /**
     * The dynamic parts of one form at one level of detail (all drawn as sprites / strips, nothing is allocated: the painter reads these numbers).
     *
     * @param ribbons  winding water / ink ribbons, {@code ribbonSegs} strip segments each
     * @param drops    orbiting droplets (or embers)
     * @param rings    halo / orbit rings drawn as {@code ringSegs} segment strips
     * @param sigil    the ground sigil and ripples (three ripple quads + the sigil quad)
     * @param backSigil the big seal standing behind the back (two quads)
     * @param eyes     glowing eye quads (two) and the brow flare
     * @param skin     the water / ink skin shell and the haze shell (two full body passes: ~1700 vertices each at the player model's 6 boxes... counted in the doc)
     */
    public record Recipe(int ribbons, int ribbonSegs, int drops, int rings, int ringSegs, boolean sigil, boolean backSigil, boolean eyes, boolean skin) {}

    private static final Recipe NONE = new Recipe(0, 0, 0, 0, 0, false, false, false, false);

    /** The dynamic budget of a form. Plain armour (style 0) is the old painter's own and is not part of this table. */
    public static Recipe recipe(Form f, Lod lod) {
        if (lod == Lod.NONE || f == Form.ARMOUR) return NONE;
        boolean c = lod == Lod.CHARACTER, near = lod != Lod.FAR;
        return switch (f) {
            case VALKYRIE -> new Recipe(c ? 4 : near ? 2 : 0, 10, c ? 14 : near ? 8 : 0, c ? 3 : near ? 1 : 0, 24, near, c, false, near);
            case MERMAID -> new Recipe(c ? 4 : near ? 2 : 0, 10, c ? 16 : near ? 8 : 0, c ? 2 : near ? 1 : 0, 24, near, c, false, near);
            case SEA_DRAGON -> new Recipe(c ? 3 : near ? 2 : 0, 12, c ? 12 : near ? 6 : 0, c ? 3 : near ? 1 : 0, 24, near, c, near, near);
            case BLACK_DRAGON -> new Recipe(c ? 5 : near ? 3 : 0, 12, c ? 16 : near ? 8 : 0, c ? 2 : near ? 1 : 0, 24, near, c, true, near);
            default -> NONE;
        };
    }

    /** The sprite quads of a recipe (strips count one quad per segment, a ring one per segment, the sigil four, the back seal two, the eyes four). Excludes the skin shells and the geo pieces. */
    public static int quads(Recipe r) { return r.ribbons() * r.ribbonSegs() + r.drops() + r.rings() * r.ringSegs() * 2 + (r.sigil() ? 16 : 0) + (r.backSigil() ? 2 : 0) + (r.eyes() ? 2 : 0); }

    /** The most sprite quads one player's forms may draw in a frame (the tests hold every form at every level under it). */
    public static final int QUAD_CAP = 400;

    // ------------------------------------------------------------------ what a form gives and costs (server)
    /** The form lasts until it is toggled off, the mana runs out, the wearer is knocked out or dies (the armour policy) EXCEPT these limits (game ticks; 0 = none). */
    public static long limitTicks(Form f) {
        return switch (f) {
            case SEA_DRAGON -> 6000L;        // five minutes
            case BLACK_DRAGON -> 1800L;      // ninety seconds
            default -> 0L;
        };
    }

    /** After the Black Dragon Form: the exhaustion that keeps the wearer out of it (game ticks). Slowness and weakness only: never hunger, never the food bar. */
    public static final long BLACK_AFTERMATH_TICKS = 3600L;

    public static boolean expired(Form f, long since, long now) { long lim = limitTicks(f); return lim > 0 && since > 0 && now - since >= lim; }

    public static boolean blocked(long aftermathUntil, long now) { return now < aftermathUntil; }

    /** One potion effect a form keeps up (refreshed every second while worn): name, amplifier, whether only in water. */
    public record Buff(String effect, int amplifier, boolean waterOnly, boolean landOnly) {}

    public static Buff[] buffs(Form f) {
        return switch (f) {
            case MERMAID -> new Buff[]{new Buff("water_breathing", 0, false, false), new Buff("dolphins_grace", 1, true, false), new Buff("regeneration", 0, true, false), new Buff("movement_slowdown", 0, false, true)};
            case SEA_DRAGON -> new Buff[]{new Buff("damage_boost", 1, false, false), new Buff("damage_resistance", 1, false, false), new Buff("movement_speed", 0, false, false), new Buff("water_breathing", 0, false, false)};
            case BLACK_DRAGON -> new Buff[]{new Buff("damage_boost", 2, false, false), new Buff("damage_resistance", 2, false, false), new Buff("movement_speed", 1, false, false), new Buff("night_vision", 0, false, false)};
            default -> new Buff[0];
        };
    }

    /** Entry shockwave of a form: raw damage, radius in blocks (0 = none). Damage goes through the balance law, the radius is capped. */
    public static float entryDamage(Form f) { return f == Form.SEA_DRAGON ? 12f : f == Form.BLACK_DRAGON ? 18f : 0f; }

    public static double entryRadius(Form f) { return f == Form.SEA_DRAGON ? 6.0 : f == Form.BLACK_DRAGON ? 8.0 : 0.0; }

    /** Motes of the form's particle theme sent round the wearer once a second (server side; the particle facade thins them further). */
    public static final int AMBIENT_MOTES = 4;

    /** Most foes one entry shockwave may hit (a bound on the work of one cast). */
    public static final int ENTRY_MAX_TARGETS = 24;
}
