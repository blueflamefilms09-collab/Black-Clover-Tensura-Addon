package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Pal;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.WindLook;

/**
 * Spirit Dive WIND remake: THREE selectable designs (client option {@code spiritDive.windLook} = {@code A_ANIME}, {@code B_CLIP}, {@code C_SAINT}; {@code PACK} and {@code REEL} stay) with REAL 3D
 * weapons and pieces ({@link SpiritDiveWind3Mesh}, authored by {@code tools/gen_sdw3_models.py}). Everything that can be decided without a game lives here, pure and unit tested
 * ({@code SpiritDiveWind3Test}); the painter ({@code client.aura.SpiritDiveWind3}) reads these tables and the mesh sizes, so the worst case of a dive is exactly what is counted here.
 *
 * <ul>
 *   <li><b>A "Anime-true"</b>: the PACK look (pale green-white body light, thin arrow-bar crown on a ring, the big smooth leaf wing with the cross, the wrist circle of crosses) with the weapons now real
 *       3D: Zephyr's sword with a real blade, fuller, cross-guard and two crossed rings floats at the right hip, Boreas' leaf bow rides on the back and is in the hand (with a real arrow of light) while a
 *       page is cast. The standard tier floats a small leaf blade.</li>
 *   <li><b>B "Clip-style"</b>: what the clips show: a ring of glowing cubes and tall cube bars as the crown, cube gauntlets, a voxel leaf wing of stacked glowing panels, a flat magic circle round the hilt of
 *       the real sword, a second blade standing up behind the shoulder, white spiral ribbons on entry, in mint / teal / white.</li>
 *   <li><b>C "Saint Spirit"</b>: the spirit assimilated: wings of long spirit feathers (real feather blades fanned from the shoulder blades), a tall crown of 3D arrow bars, light-armour pauldrons and a halo of
 *       small spirit arrows circling the body; no sword, the arrows are the weapon (and one big arrow of light flies out of the hand in a cast).</li>
 * </ul>
 *
 * <p>Cost model: the vertices of the pack look's body light, ribbons, sparkles, ground ring and entry (shared by the three) plus the pieces of the version, each counted from the real mesh files
 * (the piece's vertices per layer drawn). {@link #CAP_CHARACTER} 4000 and {@link #CAP_STANDARD} 2500 are what a dive may draw; {@code SpiritDiveWind3Test} sums every tier, level of detail and stage.</p>
 */
public final class SpiritDiveWind3Rules {
    private SpiritDiveWind3Rules() {}

    /** The three designs. */
    public enum Ver { A, B, C }

    /** The design a client option value draws, or null for PACK and REEL (the older Winds). */
    public static Ver ver(WindLook look) {
        if (look == null) return null;
        return switch (look) {
            case A_ANIME -> Ver.A;
            case B_CLIP -> Ver.B;
            case C_SAINT -> Ver.C;
            default -> null;
        };
    }

    /** The option value of a design. */
    public static WindLook look(Ver v) { return v == Ver.A ? WindLook.A_ANIME : v == Ver.B ? WindLook.B_CLIP : WindLook.C_SAINT; }

    public static final int CAP_CHARACTER = 4000, CAP_STANDARD = 2500;

    // ------------------------------------------------------------------ textures and meshes
    public static final String SKIN = "aura/sdw3_skin", SOFT = "aura/sdw3_soft";
    public static final String CIRCLE = "aura/sdw_circle", CROSS = "aura/sdw_cross";

    /** Every texture the three looks add (the older Wind textures they share are listed by {@link SpiritDiveWindRules#allTextures}). */
    public static java.util.List<String> allTextures() { return java.util.List.of(SKIN, SOFT); }

    // ------------------------------------------------------------------ the colours
    private static final Pal A_STD = SpiritDiveWindRules.pal(false), A_CHAR = SpiritDiveWindRules.pal(true);
    private static final Pal B_STD = new Pal(0x8FFFE0, 0x5CF0C8, 0xF0FFFA, 0xD8FFF4, 0xB8FFE8), B_CHAR = new Pal(0x98FFE6, 0x66F6D0, 0xF4FFFC, 0xE0FFF8, 0xC0FFEE);
    private static final Pal C_STD = new Pal(0xC8FFDC, 0x96FFC0, 0xF8FFFA, 0xF0FFF6, 0xE0FFEC), C_CHAR = new Pal(0xD0FFE2, 0xA2FFC8, 0xFAFFFC, 0xF4FFF8, 0xE8FFF0);

    /** The colours of a design and tier: pale green-white (A), mint-teal-white (B), white with a green breath (C). No blue in any (a test pins it). */
    public static Pal pal(Ver v, boolean character) {
        return switch (v) { case A -> character ? A_CHAR : A_STD; case B -> character ? B_CHAR : B_STD; case C -> character ? C_CHAR : C_STD; };
    }

    /** The colour of the entry's cage of circles per design. */
    public static int cage(Ver v) { return v == Ver.B ? 0xD8FFF4 : v == Ver.C ? 0xF2FFF6 : SpiritDiveWindRules.CAGE_RGB; }

    // ------------------------------------------------------------------ what a dive of a design draws
    /**
     * The new pieces of a design at a level of detail (counts for a dive at a full stage; the first dive draws the left half only).
     *
     * @param cubes      B: cubes and bars of the crown ring
     * @param lights     B: white lights on the tall bars (sprites)
     * @param gauntlets  B: cube gauntlets
     * @param panels     B: panels of the big voxel wing (left); {@code panelsSmall} the small wing (right)
     * @param sword      0 none, 1 Zephyr's sword (all layers), 2 the small leaf blade of the standard tier
     * @param spear      B: the second blade behind the shoulder
     * @param bow        A: the leaf bow (on the back at rest, in the hand in a cast) with its arrow
     * @param circle     B: the flat magic circle round the hilt (a sprite and its glow)
     * @param crown3d    C: the tall crown of 3D arrow bars (else the sprite crown of the pack look)
     * @param feathers   C: spirit feathers per side
     * @param plates     C: pauldron plates
     * @param orbiters   C: arrows of the halo
     * @param arrow      C: the big arrow of light in a cast
     * @param detail     the layers of the small pieces: true = the glow shell and ornaments too (FULL), false = the solid layer only
     */
    public record Spec(int cubes, int lights, int gauntlets, int panels, int panelsSmall, int sword, int spear, int bow, boolean circle, boolean crown3d, int feathers, int plates, int orbiters,
                       boolean arrow, boolean detail) {}

    private static final Spec NONE = new Spec(0, 0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, 0, false, false);

    public static Spec spec(Ver v, boolean character, Lod lod) {
        if (lod == Lod.CULLED || lod == Lod.FAR) return NONE;
        boolean f = lod == Lod.FULL;
        return switch (v) {
            case A -> f ? new Spec(0, 0, 0, 0, 0, character ? 1 : 2, 0, character ? 1 : 0, false, false, 0, 0, 0, character, true) : NONE;
            case B -> f ? (character ? new Spec(10, 4, 2, 7, 4, 1, 1, 0, true, false, 0, 0, 0, false, true) : new Spec(8, 4, 1, 7, 0, 2, 0, 0, false, false, 0, 0, 0, false, true))
                    : new Spec(8, 0, 1, 5, 0, 0, 0, 0, false, false, 0, 0, 0, false, false);
            case C -> f ? (character ? new Spec(0, 0, 0, 0, 0, 0, 0, 0, false, true, 4, 2, 7, true, true) : new Spec(0, 0, 0, 0, 0, 0, 0, 0, false, false, 3, 0, 4, false, true))
                    : new Spec(0, 0, 0, 0, 0, 0, 0, 0, false, false, 2, 0, 0, false, false);
        };
    }

    // ------------------------------------------------------------------ the layers drawn of each piece (the painter uses the same masks)
    public static final int ALL = 7, SOLID_GLOW = 3, SOLID = 1, SOLID_ORN = 5;
    /** Sprites: a billboard is one quad, four vertices. */
    public static final int SPRITE = SpiritDiveWindRules.SPRITE;

    private static int mv(String name, int mask) { return SpiritDiveWind3Mesh.get(name).vertsOf(mask); }

    /** Vertices of the sword mesh as the version draws it (all layers; the layers named by {@code spec.sword}). */
    public static int swordVerts(int kind) { return kind == 1 ? mv("sword", ALL) : kind == 2 ? mv("blade", SOLID_GLOW) : 0; }

    public static int bowVerts() { return mv("bow", ALL); }

    public static int arrowVerts() { return mv("arrow", ALL); }

    /** The layer mask of the small pieces of a spec (panels, feathers, ...). */
    public static int mask(Spec s) { return s.detail() ? SOLID_GLOW : SOLID; }

    // ------------------------------------------------------------------ counting
    /** The vertices every design shares: the body light, the steady ribbons and sparkles, the ground ring and the entry (the pack look's numbers). */
    public static int base(boolean character, Lod lod, Stage st) {
        if (lod == Lod.CULLED) return 0;
        SpiritDiveWindRules.Spec s = SpiritDiveWindRules.spec(character, lod);
        SpiritDiveReelRules.Spec reel = SpiritDiveReelRules.spec(Element.WIND, character, lod);
        boolean full = SpiritDivePieces.full(st);
        int v = SpiritDiveAnimeRules.shellVertices(Element.WIND, st, lod);
        if (full) v += s.sparks() * SPRITE + s.ribbons() * s.ribbonSegs() * 4;
        v += SpiritDiveWindRules.RING_VERTS;
        if (lod != Lod.FAR) v += SpiritDiveReelRules.ribbonVertices(reel);
        if (lod == Lod.FULL) v += SpiritDiveReelRules.entryVertices(reel) - SpiritDiveReelRules.ribbonVertices(reel);
        return v;
    }

    /** Vertices of the cube gauntlet of one arm: the forearm cube (with its glow at full detail), the fist cube and the band (full detail). */
    public static int gauntletVerts(boolean detail) { return detail ? mv("cube", SOLID_GLOW) + 2 * mv("cube", SOLID) : mv("cube", SOLID); }

    /** The pieces' vertices of design B at a stage. */
    private static int piecesB(Spec s, Stage st, boolean full, boolean half) {
        int v = 0;
        boolean crowned = SpiritDiveReelRules.crowned(Element.WIND, st);
        if (crowned && s.cubes() > 0) v += SpiritDiveWind3Geometry.crownCount(s.cubes(), half) * mv("cube", SOLID) + (half ? s.lights() / 2 : s.lights()) * SPRITE;
        v += Math.min(s.gauntlets(), SpiritDiveReelRules.arms(Element.WIND, st)) * gauntletVerts(s.detail());
        int m = mask(s);
        v += s.panels() * mv("panel", m) + (half ? 0 : s.panelsSmall() * mv("panel", m));
        if (full) v += swordVerts(s.sword()) + (s.spear() > 0 ? mv("spear", ALL) : 0) + (s.circle() ? 2 * SPRITE : 0);
        return v;
    }

    private static int piecesC(Spec s, Stage st, boolean full, boolean half, boolean character, Lod lod) {
        int v = 0;
        boolean crowned = SpiritDiveReelRules.crowned(Element.WIND, st);
        if (crowned) {
            if (s.crown3d()) v += (half ? mv("crown", SOLID) / 2 : mv("crown", SOLID));
            else {                                                                                           // the sprite crown of the pack look
                SpiritDiveWindRules.Spec p = SpiritDiveWindRules.spec(character, lod);
                if (p.bars() > 0) v += SpiritDiveWindGeometry.barCount(p.bars(), half) * SPRITE + p.rings() * SpiritDiveWindRules.BAND;
            }
        }
        int sides = half ? 1 : 2;
        v += sides * s.feathers() * mv("feather", SOLID);
        v += (half ? Math.min(1, s.plates()) : s.plates()) * mv("plate", SOLID);
        if (full) v += (half ? (s.orbiters() + 1) / 2 : s.orbiters()) * (mv("blade", SOLID) + SPRITE) + (s.arrow() ? arrowVerts() : 0);
        return v;
    }

    /**
     * The most vertices one dive of a design draws at a tier, level of detail and stage, counted the way the painter draws.
     */
    public static int vertices(Ver ver, boolean character, Lod lod, Stage st) {
        if (lod == Lod.CULLED) return 0;
        boolean full = SpiritDivePieces.full(st), half = SpiritDivePieces.halfBody(Element.WIND, st);
        Spec s = spec(ver, character, lod);
        if (ver == Ver.A) {
            int v = SpiritDiveWindRules.vertices(character, lod, st);
            SpiritDiveWindRules.Spec p = SpiritDiveWindRules.spec(character, lod);
            if (full && p.weapon()) v -= Math.max(SpiritDiveWindRules.SWORD_VERTICES, SpiritDiveWindRules.BOW_VERTICES);
            if (full) v += swordVerts(s.sword()) + (s.bow() > 0 ? bowVerts() : 0) + (s.arrow() ? arrowVerts() : 0);
            return v;
        }
        int v = base(character, lod, st);
        if (lod == Lod.FAR) return v;
        return v + (ver == Ver.B ? piecesB(s, st, full, half) : piecesC(s, st, full, half, character, lod));
    }

    /** The worst case over the stages of a design and tier at the full level of detail. */
    public static int worst(Ver ver, boolean character) {
        int w = 0;
        for (Stage st : Stage.values()) w = Math.max(w, vertices(ver, character, Lod.FULL, st));
        return w;
    }

    /** The cap a tier is held to. */
    public static int cap(boolean character) { return character ? CAP_CHARACTER : CAP_STANDARD; }
}
