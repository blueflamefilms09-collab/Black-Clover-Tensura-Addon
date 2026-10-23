package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Anchor;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Spirit Dive, Djinn-equip style (the {@code DJINN} look, the default): the armour sets of the four dives as data. Pure (no game classes), unit tested by SpiritDiveDjinnTest. The look is the style
 * of the Djinn Full Equip (docs/magic/spirit_dives_djinn.md, section 1): a full set of plates on every limb, forged on piece by piece in the Djinn's order (the light under-layers first, the plates,
 * the heavy cuirass, then the wings and the back pieces), each piece a geo model with an engraved glow map, drawn on the player's own model parts by {@code client.aura.SpiritDiveDjinn} through
 * {@code EquipAnimation} (the arithmetic of the Djinn layer).
 *
 * <p>Two tiers, the same as the other looks ({@link SpiritDiveTier}): the STANDARD set of every mage (a plain full armour, a quieter glow) and the CHARACTER set of Yuno (Wind), Fuegoleon (Fire) and
 * Noelle (Water): the ornate armour with gorget, cuisses and skirt, the signature crest, plume, mantle and wings. Earth has no character: its set is always the standard one (heavier: cuisses and a
 * crystal cluster on the back). The files are {@code assets/nusmp/geo/entity/sdj_<element>_<s|c>_arm_<piece>.geo.json} (+ texture, {@code _glow} map, animation), written by
 * {@code tools/gen_spirit_dive_djinn.py}.</p>
 *
 * <p><b>Stages.</b> HALF is the Djinn's Half Equip: the crest and the arms (Wind: the LEFT half of the body, as in the series: the left crest, arm, hip and leg); FULL is the whole set; UPGRADE forges
 * the missing pieces onto a half dive that stays on. <b>Fuegoleon</b>: his right arm is the flaming arm of {@code client.aura.FuegoleonArmAura}, so no plate of the right arm is listed for his grade.</p>
 */
public final class SpiritDiveDjinnPieces {
    private SpiritDiveDjinnPieces() {}

    /** The look clients draw. The ordinal is stored nowhere (a client option); append only anyway. */
    public enum Look { DJINN, V2, CLASSIC, ANIME, REEL }

    /** The default look (the owner's request: the style of the Full Djinn equip). */
    public static final Look DEFAULT_LOOK = Look.REEL;   // 3.3: the owner liked the V2 design and asked for it like the videos: the default is the REEL look (SpiritDiveReelRules); ANIME (3.2) and the others stay choosable

    /**
     * One piece. {@code delay} 0 to 12 ticks, {@code dx dy dz} the side it flies in from (model pixels), {@code glass} always translucent, {@code detail} skipped beyond 24 blocks, {@code silhouette} still
     * drawn up to 64 blocks (without its glow), {@code head} skipped for the local player in first person, {@code clip} the looped animation clip ("" none), {@code flap} the clip runs faster in the air,
     * {@code glow} the glow multiplier of the piece (0 = no glow pass at all: the piece costs one pass instead of two).
     */
    public record Def(String name, Anchor anchor, float delay, float dx, float dy, float dz, boolean glass, boolean detail, boolean silhouette, boolean head, String clip, boolean flap, float glow) {
        /** Draw passes of the piece: the body, and the glow map when it has any. */
        public int passes() { return glow > 0f ? 2 : 1; }
    }

    private static Def d(String name, Anchor a, float delay, float dx, float dy, float dz) { return new Def(name, a, delay, dx, dy, dz, false, false, false, false, "", false, 0f); }

    private static Def with(Def x, boolean glass, boolean detail, boolean silhouette, boolean head, String clip, boolean flap, float glow) {
        return new Def(x.name, x.anchor, x.delay, x.dx, x.dy, x.dz, glass, detail, silhouette, head, clip, flap, glow);
    }

    // ------------------------------------------------------------------ the pieces (the Djinn's fly-in sides; the delay follows the weight of a piece, as requip.ArmourSets)
    /** The part a piece sits on and when it forges, by name; {@code character} sets the glow level of the tier. Null for a name no set has. */
    public static Def def(String name, boolean character) {
        float g = character ? 1f : 0.8f;
        return switch (name) {
            case "belt" -> with(d(name, Anchor.BODY, 0, 0, 8, -8), false, true, false, false, "", false, 0f);
            case "gorget" -> with(d(name, Anchor.BODY, 1, 0, -10, -4), false, true, false, false, "", false, 0f);
            case "gauntlet_r" -> with(d(name, Anchor.ARM_R, 2, -10, 8, -6), false, true, false, false, "", false, 0.9f * g);
            case "gauntlet_l" -> with(d(name, Anchor.ARM_L, 2, 10, 8, -6), false, true, false, false, "", false, 0.9f * g);
            case "vambrace_r" -> with(d(name, Anchor.ARM_R, 3, -12, 2, -6), false, false, false, false, "", false, character ? 0.8f : 0f);
            case "vambrace_l" -> with(d(name, Anchor.ARM_L, 3, 12, 2, -6), false, false, false, false, "", false, character ? 0.8f : 0f);
            case "sabaton_r" -> with(d(name, Anchor.LEG_R, 3, -3, 14, -8), false, true, false, false, "", false, 0f);
            case "sabaton_l" -> with(d(name, Anchor.LEG_L, 4, 3, 14, -8), false, true, false, false, "", false, 0f);
            case "greave_r" -> with(d(name, Anchor.LEG_R, 5, -3, 14, -6), false, false, false, false, "", false, character ? 0.7f : 0f);
            case "greave_l" -> with(d(name, Anchor.LEG_L, 6, 3, 14, -6), false, false, false, false, "", false, character ? 0.7f : 0f);
            case "cuisse_r" -> with(d(name, Anchor.LEG_R, 5, -4, 12, -6), false, true, false, false, "", false, 0f);
            case "cuisse_l" -> with(d(name, Anchor.LEG_L, 6, 4, 12, -6), false, true, false, false, "", false, 0f);
            case "skirt" -> with(d(name, Anchor.BODY, 6, 0, 10, -6), false, true, false, false, "idle", false, 0f);
            case "helm" -> with(d(name, Anchor.HEAD, 6, 0, -16, 0), false, false, true, true, "", false, g);
            case "crest", "crest_l", "crest_r", "crest_c" -> with(d(name, Anchor.HEAD, name.equals("crest_c") ? 8 : 7, name.equals("crest_l") ? 4 : name.equals("crest_r") ? -4 : 0, -16, 0),
                    false, false, true, true, "", false, 1.2f * g);
            case "plume" -> with(d(name, Anchor.HEAD, 8, 0, -8, 8), false, true, true, true, "idle", false, g);
            case "pauldron_r" -> with(d(name, Anchor.ARM_R, 7, -14, -4, 0), false, false, true, false, "", false, g);
            case "pauldron_l" -> with(d(name, Anchor.ARM_L, 7, 14, -4, 0), false, false, true, false, "", false, g);
            case "chest" -> with(d(name, Anchor.BODY, 8, 0, 0, -14), false, false, true, false, "", false, g);
            case "backplate" -> with(d(name, Anchor.BODY, 9, 0, 0, 12), false, false, true, false, "", false, 0.9f * g);
            case "mantle" -> with(d(name, Anchor.BODY, 9, 0, -4, 12), false, true, true, false, "idle", false, 0.7f);
            case "wing_r" -> with(d(name, Anchor.BODY, 10, -10, -4, 12), true, false, true, false, "idle", true, 1.1f);
            case "wing_l" -> with(d(name, Anchor.BODY, 10, 10, -4, 12), true, false, true, false, "idle", true, 1.1f);
            case "backcrystals" -> with(d(name, Anchor.BODY, 11, 0, 0, 12), false, false, true, false, "", false, 1.2f);
            default -> null;
        };
    }

    // ------------------------------------------------------------------ the sets
    private static final String[] ARMS_R = {"gauntlet_r", "vambrace_r", "pauldron_r"}, ARMS_L = {"gauntlet_l", "vambrace_l", "pauldron_l"};

    /** The piece names of the FULL set of an element and tier, in forge order (the light under-layers first, the back pieces last). */
    public static List<String> fullNames(Element e, boolean character) {
        List<String> n = new ArrayList<>();
        n.add("belt");
        if (character) n.add("gorget");
        n.add("gauntlet_r"); n.add("gauntlet_l"); n.add("vambrace_r"); n.add("vambrace_l");
        n.add("sabaton_r"); n.add("sabaton_l");
        n.add("greave_r"); n.add("greave_l");
        n.add("cuisse_r"); n.add("cuisse_l");                                                   // every tier: a plated thigh (the skin never shows between the plates)
        if (character || e == Element.WATER) n.add("skirt");
        n.add("helm");
        switch (e) {
            case WIND -> { n.add("crest_l"); n.add("crest_r"); if (character) n.add("crest_c"); }
            default -> n.add("crest");
        }
        if (character && e == Element.FIRE) n.add("plume");
        n.add("pauldron_r"); n.add("pauldron_l");
        n.add("chest"); n.add("backplate");
        if (character && e == Element.FIRE) n.add("mantle");
        if (character && e != Element.EARTH) { n.add("wing_r"); n.add("wing_l"); }
        if (e == Element.EARTH) n.add("backcrystals");
        return Collections.unmodifiableList(n);
    }

    /** The piece names of the HALF set (the Djinn's Half Equip: the crest and the arms; Wind: the left half of the body). */
    public static List<String> halfNames(Element e, boolean character) {
        List<String> n = new ArrayList<>();
        if (e == Element.WIND) {
            n.addAll(List.of("gauntlet_l", "vambrace_l", "sabaton_l", "greave_l"));
            n.add("cuisse_l");
            n.add("crest_l");
            n.add("pauldron_l");
            if (character) n.add("wing_l");
        } else {
            n.addAll(List.of("gauntlet_r", "gauntlet_l", "vambrace_r", "vambrace_l"));
            n.add("crest");
            if (character && e == Element.FIRE) n.add("plume");
            n.add("pauldron_r");
            n.add("pauldron_l");
        }
        return Collections.unmodifiableList(n);
    }

    /** True for the plates of the right arm, which Fuegoleon's flaming arm replaces. */
    public static boolean rightArmPlate(String name) { return name.equals("gauntlet_r") || name.equals("vambrace_r") || name.equals("pauldron_r"); }

    /** True when the dive of this element and grade has no plates on the right arm: Fuegoleon's molten arm ({@code FuegoleonArmAura}) is that arm. */
    public static boolean moltenRightArm(Element e, Grade g) { return SpiritDivePieces.flameArm(e, g); }

    /**
     * The names of the pieces drawn for an element, stage and tier, in forge order. FULL and UPGRADE are the full set; HALF the half set. {@code character} picks the character set (the caller decides
     * it: {@link #character}); the grade only removes the plates of the right arm of the Fuegoleon dive.
     */
    public static List<String> names(Element e, Stage s, Grade g, boolean character) {
        List<String> base = SpiritDivePieces.full(s) ? fullNames(e, character) : halfNames(e, character);
        if (!moltenRightArm(e, g)) return base;
        List<String> out = new ArrayList<>(base);
        out.removeIf(SpiritDiveDjinnPieces::rightArmPlate);
        return Collections.unmodifiableList(out);
    }

    /** True when this dive draws the character set: the grade belongs to the element (Earth never). */
    public static boolean character(Element e, Grade g) { return SpiritDiveTier.character(e, g); }

    /** The pieces (with their definitions) drawn for an element, stage and tier, in forge order. */
    public static List<Def> pieces(Element e, Stage s, Grade g, boolean character) {
        List<String> names = names(e, s, g, character);
        List<Def> out = new ArrayList<>(names.size());
        for (String n : names) {
            Def x = def(n, character);
            if (x != null) out.add(x);
        }
        return Collections.unmodifiableList(out);
    }

    /** True when the piece is already fully on: in an UPGRADE the pieces of the half dive stay on, only the missing ones forge in. */
    public static boolean alreadyOn(Element e, Stage s, Grade g, boolean character, String name) {
        return s == Stage.UPGRADE && halfNames(e, character).contains(name) && names(e, s, g, character).contains(name);
    }

    /** The key of a set: {@code sdj_<element>_<s|c>}. */
    public static String key(Element e, boolean character) { return "sdj_" + e.name().toLowerCase(Locale.ROOT) + "_" + (character ? "c" : "s"); }

    /** The geo model id of a piece (the file stem under assets/nusmp/geo/entity). */
    public static String modelId(Element e, boolean character, String name) { return key(e, character) + "_arm_" + name; }

    /** The name part of the model for {@code GeoSpec.of(key, "arm_" + name)}. */
    public static String modelName(String name) { return "arm_" + name; }

    // ------------------------------------------------------------------ the body skin under the plates (the veins pass)
    /** Which parts of the body the vein layer covers: every full dive the whole body, a half dive the arms (Wind: the left arm and leg; Fuegoleon: the left arm only). Never the head. */
    public static int veinParts(Element e, Stage s, Grade g) {
        if (SpiritDivePieces.full(s)) return moltenRightArm(e, g) ? SpiritDivePieces.SHELL_ALL & ~SpiritDivePieces.SHELL_ARM_R : SpiritDivePieces.SHELL_ALL;
        if (e == Element.WIND) return SpiritDivePieces.SHELL_ARM_L | SpiritDivePieces.SHELL_LEG_L;
        return moltenRightArm(e, g) ? SpiritDivePieces.SHELL_ARM_L : SpiritDivePieces.SHELL_ARM_R | SpiritDivePieces.SHELL_ARM_L;
    }

    // ------------------------------------------------------------------ the levels of detail
    /**
     * Whether a piece is drawn at a level of detail: FULL everything (up to 24 blocks); MID (to 48) without the {@code detail} pieces; FAR (to 64) only the {@code silhouette} pieces, body pass only
     * (no glow); CULLED nothing.
     */
    public static boolean drawn(Def p, Lod lod) {
        return switch (lod) {
            case FULL -> true;
            case MID -> !p.detail();
            case FAR -> p.silhouette() && !p.detail();
            case CULLED -> false;
        };
    }

    /** Passes of a piece at a level of detail: the glow pass only up to 48 blocks. */
    public static int passes(Def p, Lod lod) { return lod == Lod.FAR ? 1 : p.passes(); }

    // ------------------------------------------------------------------ the budgets (rule 11): bounded by the Djinn Full Equip's own numbers
    /** The Djinn Full Equip's quads, counted from its mesh files (assets/nusmp/djinn, tools/relics): the largest and the smallest of the 36 unique sets (the look a Djinn is drawn with), body plus glow quads. */
    public static final int DJINN_QUADS_MAX = 4351, DJINN_QUADS_MIN = 2856;
    /** Vertices a quad takes. */
    public static final int VERTICES_PER_QUAD = 4;

    /** The most vertices the pieces of one dive may submit in a frame at full detail: the character set the Djinn's largest set, the standard set its smallest. Pinned by the asset test. */
    public static int pieceVertexCap(boolean character) { return (character ? DJINN_QUADS_MAX : DJINN_QUADS_MIN) * VERTICES_PER_QUAD - extrasVertices(character) - veinVertices(); }

    /** Rings of light the forge opens with (white and the element's colour; the character tier adds a third, larger one): flat rings, two quads each. */
    public static int flashRings(boolean character) { return character ? 3 : 2; }

    /** Motes rising round the body (one quad each): none at the levels of detail where the extras are dropped. */
    public static int motes(boolean character, Lod lod) {
        return switch (lod) {
            case FULL -> character ? 12 : 6;
            case MID -> character ? 4 : 0;
            default -> 0;
        };
    }

    /** The steady ring on the ground (two quads) and the one camera-facing flare of the forge (one quad, the first 12 ticks). */
    public static int steadyRings(Lod lod) { return lod == Lod.CULLED ? 0 : 1; }

    /**
     * The most quads the extras of a dive draw at a level of detail: restrained, no light columns, helices, pillars or sigils (the Djinn's own particle count is the model): the forge rings and the
     * flare (full detail only), the steady ring, the motes.
     */
    public static int extrasQuads(boolean character, Lod lod) {
        if (lod == Lod.CULLED) return 0;
        return (lod == Lod.FULL ? flashRings(character) * 2 + 1 : 0) + steadyRings(lod) * 2 + motes(character, lod);
    }

    public static int extrasVertices(boolean character) { return extrasQuads(character, Lod.FULL) * VERTICES_PER_QUAD; }

    /** The vein layer: the posed player model drawn once more with the additive vein texture and once with the haze: at most twelve boxes (the body parts and their overlay layers), 24 vertices a box, two passes. */
    public static int veinVertices() { return 12 * 24 * 2; }

    /** The most vertices one dive submits at full detail from every source: pieces (as listed), the vein layer and the extras. The test compares this with the Djinn's. */
    public static int worstVertices(boolean character, int pieceVertices) { return pieceVertices + veinVertices() + extrasVertices(character); }

    /** The worst case of the character set across a crowd: at most this many dives per frame draw it ({@link SpiritDiveTier#CHARACTER_PER_FRAME}); every other dive draws the standard set. */
    public static int worstFrameVertices(int characterVertices, int standardVertices, int divers) {
        int c = Math.min(divers, SpiritDiveTier.CHARACTER_PER_FRAME);
        return c * characterVertices + Math.max(0, divers - c) * standardVertices;
    }

    // ------------------------------------------------------------------ the particle trail of the Djinn look (restrained: the Djinn's own ambient is 6 to 13 particles a second)
    /** Particles one diving player spawns this client tick: the standard set one every fourth tick (5 a second), the character set one every second tick (10 a second), staggered by entity id. */
    public static int trailCount(boolean character, long tick, int entityId) {
        return Math.floorMod(tick + entityId, character ? 2L : 4L) == 0 ? 1 : 0;
    }

    /** The burst on the tick after a dive begins: smaller than the remake's (the forge itself is the show). */
    public static int entryBurst(boolean character) { return character ? 16 : 6; }

    /** The most particles per second the trails of all dives in view can ask for (before the client particle budget and the effect quality thin them). */
    public static int worstTrailPerSecond() { return SpiritDiveTier.TRAIL_PLAYERS * 10; }

    // ------------------------------------------------------------------ the forge-in
    /** The tick (from the start of the dive) at which the last piece of a set has finished forging in: the largest delay plus the forge time of EquipAnimation. */
    public static float forgeEnd(List<Def> pieces) {
        float m = 0f;
        for (Def x : pieces) m = Math.max(m, x.delay());
        return m + 8f;
    }

    /** Ticks of the flash the forge opens with (a ring of light opening at the feet and a second one, white and in the element's colour), and its brightness at tick {@code t} (1 at the start, 0 when over). */
    public static final float FLASH_TICKS = 16f;

    public static float flash(float t) { return t <= 0f ? 1f : t >= FLASH_TICKS ? 0f : 1f - t / FLASH_TICKS; }

    /** The extra shock the character tier forges with: a third, larger ring and a brighter first one while the heavy pieces (the cuirass, the wings) assemble. */
    public static float bigForge(boolean character) { return character ? 1.5f : 1f; }

    // ------------------------------------------------------------------ the colours of the glow map and the rings, per element and tier (RGB)
    public static int glowRgb(Element e, Grade g) { return SpiritDiveTier.glow(e, g, false); }

    /** The colour of the second ring of the flash (the primary colour of the tier). */
    public static int ringRgb(Element e, Grade g) { return SpiritDiveTier.primary(e, g); }
}
