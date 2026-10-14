package com.newuniverse.nusmp.client.relics.djinn;

/**
 * The pure decisions of the Djinn's look (no Minecraft types, so they are unit tested): which set of meshes and textures a Djinn is drawn from
 * (the shared rig it had first, its own unique look, the Dark Djinn look), the colour maths of the dark variants, the per-Djinn motion and
 * particle tables, and how many particles a tick's share of a rate spawns.
 *
 * <p>Three mesh sets exist per Djinn: {@link #BASE} {@code djinn/<id>/} (the old shared rig, kept as the fallback), {@link #UNIQUE}
 * {@code djinn/u_<id>/} and {@link #DARK} {@code djinn/d_<id>/}. A Djinn is drawn from ONE set: the first of the preferred chain whose marker piece
 * exists, so a piece a set leaves out on purpose (Baal has no halo, Sitri no tail) is never filled in from an older set.
 */
public final class DjinnLook {
    private DjinnLook() {}

    public static final int BASE = 0, UNIQUE = 1, DARK = 2;

    private static final int[] CHAIN_DARK = {DARK, UNIQUE, BASE}, CHAIN_LIGHT = {UNIQUE, BASE};

    /** The sets to try, best first. The arrays are shared and never modified. */
    public static int[] chain(boolean dark) { return dark ? CHAIN_DARK : CHAIN_LIGHT; }

    /** The set a Djinn is drawn from: the first of the preferred chain whose marker piece exists ({@code hasSet} tells), the shared rig if none does. */
    public static int pickSet(boolean dark, java.util.function.IntPredicate hasSet) {
        for (int kind : chain(dark)) if (kind == BASE || hasSet.test(kind)) return kind;
        return BASE;
    }

    /** The piece every set has, tested for the set's existence. */
    public static final String MARKER_PIECE = "chest";

    /** The mesh directory under assets/nusmp/djinn/. */
    public static String dir(String id, int kind) { return kind == DARK ? "d_" + id : kind == UNIQUE ? "u_" + id : id; }

    /** The armour skin (and with {@code glow} its additive twin) of a set: textures/entity/djinn/&lt;id&gt;[_u|_d]_armor[_glow].png. */
    public static String skinPath(String id, int kind, boolean glow) {
        return "textures/entity/djinn/" + id + (kind == DARK ? "_d" : kind == UNIQUE ? "_u" : "") + "_armor" + (glow ? "_glow" : "") + ".png";
    }

    /** The additive body-art overlay: the dark one, the Djinn's own pattern, or (the first look) the shared rune. */
    public static String runePath(String id, boolean dark, boolean uniqueExists) {
        return "textures/entity/djinn/" + id + (dark ? "_d" : uniqueExists ? "_u" : "") + "_rune.png";
    }

    /** The Lesser Summon's bust: the dark one for a dark Djinn if it exists, else the shared one. */
    public static String bustPath(String id, boolean dark, boolean darkExists) {
        return "textures/entity/djinn/" + id + (dark && darkExists ? "_d" : "") + "_bust.png";
    }

    /** The black veins drawn over the skin of a dark Djinn (alpha blended). */
    public static String artPath(String id) { return "textures/entity/djinn/" + id + "_d_art.png"; }

    // ================================================================ colours
    public static int mix(int a, int b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k), g = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k),
                bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }

    /** Every channel multiplied by k, clamped to 0..255. */
    public static int scale(int rgb, float k) {
        int r = Math.min(255, Math.max(0, (int) (((rgb >> 16) & 255) * k))), g = Math.min(255, Math.max(0, (int) (((rgb >> 8) & 255) * k))),
                b = Math.min(255, Math.max(0, (int) ((rgb & 255) * k)));
        return (r << 16) | (g << 8) | b;
    }

    /** The glow colour as the Zagred shader's custom tint (it reads a pure white as "use the default colours", so a pure white is nudged). */
    public static int tintSafe(int rgb) { return (rgb & 0xFFFFFF) == 0xFFFFFF ? 0xFFFFFE : rgb & 0xFFFFFF; }

    /** The aura of a Dark Djinn: a dark red-violet with a little of the Djinn's own glow, so each Dark Djinn is still told apart. */
    public static int darkAura(int glow) { return tintSafe(mix(0x5A146E, glow & 0xFFFFFF, 0.22f)); }

    /** The edge tint of the dark screen: a black violet with a trace of the Djinn's colour. */
    public static int darkVignette(int glow) { return mix(0x1C0626, glow & 0xFFFFFF, 0.14f); }

    /** The additive glow of a dark set's skin: the Djinn's saturated colour, only a little of its pale glow (the pale glow washes out against black). */
    public static int darkGlow(int color, int glow) { return mix(color & 0xFFFFFF, glow & 0xFFFFFF, 0.3f); }

    /** Gold for the Dark Djinn's HUD frame. */
    public static final int GOLD = 0xD8B048;

    // ================================================================ motion per Djinn (indexed by the Djinn's ordinal)
    /**
     * How a Djinn's Full Equip moves: {@code flap} multiplies the wing beat, {@code tailSegs} the chain of the tail (0 none), {@code haloSpin} degrees
     * a tick, {@code backSpin} degrees a tick of the back piece about its centre (0 for one that must not turn), {@code backPulse} its breathing
     * (0..0.1 of its size), {@code weaponScale} the size of the Half Equip weapon in hand, {@code tailWave} the sway of the tail.
     */
    public record Style(float flap, int tailSegs, float haloSpin, float backSpin, float backPulse, float weaponScale, float tailWave) {}

    private static final Style[] STYLES = {
            new Style(1.0f, 5, 0.0f, 0.6f, 0.00f, 0.80f, 1.0f),     // BAAL: the blood ring turns slowly
            new Style(1.5f, 5, 2.5f, 0.0f, 0.05f, 0.75f, 1.3f),     // AMON: flame wings flicker fast, the arch breathes
            new Style(0.6f, 4, 1.5f, 0.0f, 0.03f, 0.80f, 0.8f),     // VINEA: slow water ribbons
            new Style(1.3f, 5, 3.0f, -1.2f, 0.00f, 0.85f, 1.2f),    // FOCALOR: the cyclone rings spin
            new Style(0.8f, 6, 1.0f, 0.0f, 0.02f, 0.85f, 0.7f),     // ZAGAN: a long vine tail
            new Style(0.7f, 3, 0.0f, 0.8f, 0.00f, 0.70f, 0.5f),     // ZEPAR: heavy wings, a short club tail, the shard crown turns
            new Style(0.9f, 4, 1.2f, 0.0f, 0.08f, 0.80f, 0.9f),     // FURFUR: the sound rings pulse
            new Style(1.1f, 4, 0.0f, 0.0f, 0.04f, 0.90f, 1.4f),     // MARCHOSIAS: the mane of fire breathes
            new Style(1.0f, 0, 0.8f, 0.0f, 0.00f, 0.80f, 1.0f),     // SITRI: no tail, the extra arms stand still
            new Style(1.4f, 5, 2.0f, 0.0f, 0.06f, 0.80f, 1.5f),     // BIFRON: jittery storm wings
            new Style(0.8f, 3, 1.0f, 0.0f, 0.00f, 0.80f, 0.8f),     // PAIMON: majestic, the moon stands still
            new Style(0.7f, 6, 0.0f, 0.0f, 0.03f, 0.80f, 1.0f),     // VASSAGO: tentacle tail, the reaper moon breathes
    };
    private static final Style FALLBACK = new Style(1.0f, 5, 0.0f, 0.0f, 0.0f, 0.8f, 1.0f);

    public static Style style(int ordinal) { return ordinal >= 0 && ordinal < STYLES.length ? STYLES[ordinal] : FALLBACK; }

    /** The halo of a dark Djinn always spins (its ring of thorns), at least this fast. */
    public static float haloSpin(Style st, boolean dark) { return dark ? Math.max(1.2f, st.haloSpin()) : st.haloSpin(); }

    // ================================================================ ambient particles
    /** Where an ambient particle is born. */
    public enum Anchor { WINGS, BODY, FEET, ABOVE, HAND }

    /**
     * What a Djinn sheds while it is Full (Half Equip sheds {@link #HALF_SHARE} of it, from the body and the hand only): {@code main} and
     * {@code second} are particle keys (resolved in DjinnAmbient), the rates are particles a second.
     */
    public record Ambient(String main, int mainRate, String second, int secondRate, Anchor anchor) {}

    private static final Ambient[] AMBIENT = {
            new Ambient("spark", 11, "mote", 2, Anchor.WINGS),          // BAAL: sparks off the feathers
            new Ambient("ember", 12, "smoke", 2, Anchor.BODY),          // AMON: embers rising
            new Ambient("bubble", 9, "drip", 4, Anchor.BODY),           // VINEA: bubbles and drops
            new Ambient("wind", 10, "feather", 2, Anchor.WINGS),        // FOCALOR: gusts and white feathers
            new Ambient("petal", 8, "spore", 4, Anchor.BODY),           // ZAGAN: petals and spores
            new Ambient("shard", 6, "ash", 3, Anchor.WINGS),            // ZEPAR: crystal shards and stone dust
            new Ambient("note", 3, "enchant", 6, Anchor.ABOVE),         // FURFUR: notes over the head
            new Ambient("ember", 13, "flame", 3, Anchor.FEET),          // MARCHOSIAS: a trail of embers from the feet
            new Ambient("mote", 10, "mote2", 3, Anchor.WINGS),          // SITRI: golden motes of light off the rays
            new Ambient("spark", 13, "crit", 2, Anchor.WINGS),          // BIFRON: violet sparks
            new Ambient("feather", 8, "snow", 4, Anchor.WINGS),         // PAIMON: white feathers and frost
            new Ambient("drip", 6, "ink", 4, Anchor.BODY),              // VASSAGO: abyssal drops and ink
    };
    private static final Ambient FALLBACK_AMBIENT = new Ambient("mote", 6, "mote", 0, Anchor.BODY);

    public static Ambient ambient(int ordinal) { return ordinal >= 0 && ordinal < AMBIENT.length ? AMBIENT[ordinal] : FALLBACK_AMBIENT; }

    /** The black rukh a Dark Djinn streams off its wings (particles a second), on top of its own ambient. */
    public static final String RUKH_KEY = "rukh";
    public static final int RUKH_RATE = 14;
    public static final float HALF_SHARE = 0.35f;

    /**
     * How many particles of {@code perSecond} spawn in one tick at {@code scale} (the phase's share): the whole part of the expectation plus one more
     * when the random draw {@code roll} (0 to 1) falls under the fraction. Never negative, and capped so a bad table cannot flood the client.
     */
    public static int spawnCount(float perSecond, float scale, float roll) {
        if (perSecond <= 0f || scale <= 0f) return 0;
        float e = perSecond / 20f * scale;
        int n = (int) e;
        if (roll < e - n) n++;
        return Math.min(n, 6);
    }

    /** True for the particles that sink (feathers, petals, drops, ash, rukh), false for the ones that rise (embers, bubbles, notes, motes). */
    public static boolean falls(String key) {
        return switch (key) {
            case "feather", "petal", "drip", "ash", "snow", "rukh", "spore", "ink", "shard" -> true;
            default -> false;
        };
    }

    /** Where an ambient anchor really is at a phase: a Half Equip has no wings and no mantle of air, so it sheds from the body. */
    public static Anchor anchorAt(Anchor a, int phase) {
        return phase >= 3 || a == Anchor.BODY || a == Anchor.FEET ? a : Anchor.BODY;
    }

    /** The share of an ambient rate a phase sheds: 0 before the Half Equip, {@link #HALF_SHARE} in it, all in the Full Equip and the Extreme Magic. */
    public static float phaseShare(int phase) { return phase >= 3 ? 1f : phase == 2 ? HALF_SHARE : 0f; }

    // ================================================================ which piece is drawn from which phase
    /**
     * The first phase a piece is drawn in: the Half Equip (2) dresses the main-hand arm, the brow crest, the Dark Djinn's eye and the weapon; the
     * Full Equip (3) everything else. {@code mainArm} says whether an arm piece belongs to the main-hand arm.
     */
    public static int minPhase(String piece, boolean mainArm) {
        return switch (piece) {
            case "crest", "eye", "weapon" -> 2;
            case "pauldron_l", "pauldron_r", "vambrace_l", "vambrace_r", "gauntlet_l", "gauntlet_r" -> mainArm ? 2 : 3;
            default -> 3;
        };
    }

    /** The eye crest belongs to the Dark Djinn only. */
    public static boolean drawsEye(boolean dark) { return dark; }
}
