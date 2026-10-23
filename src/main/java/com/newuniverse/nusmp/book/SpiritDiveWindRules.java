package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Pal;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;

/**
 * Spirit Dive, WIND, PACK look (the client option {@code spiritDive.windLook}, default {@code PACK}; {@code REEL} brings back the Wind of the REEL look unchanged): the Wind element of the default look
 * drawn the way the owner's reference pack shows Yuno's Spirit Dive (Spirit_Dive.png, Spirit_of_Zephyr.png, the manga panels of Boreas, Euros and Notos, the AMV). Everything about it that can be decided
 * without a game lives here, pure and unit tested ({@code SpiritDiveWindTest}); the painter ({@code client.aura.SpiritDiveWind}) reads these tables and nothing else, so the worst case of a dive is exactly
 * what is counted here.
 *
 * <p>What the pack shows, in words: a pale green-white light over the whole body (the clothes, the fur collar and the skin stay visible under it), a crown of tall vertical bars with arrow-tip tops on a
 * thin ring floating over the head, ONE huge smooth leaf-shaped wing swept up and back from the shoulder blade with an ornate cross at its tip and a smaller second leaf, a circle of connected crosses
 * and branches round the wrist of the outstretched hand, and, for Yuno, the long glowing sword of Spirit of Zephyr with crossed circles at its hilt, the leaf bow of Spirit of Boreas with an arrow of light,
 * white ribbons of wind and four-point sparkles. The entry keeps the grammar of the REEL look (flash, white ribbons, a ring running out, a cage of circles, the body assembling) in these colours.</p>
 *
 * <p>Cost model: the same vertex counting as the REEL look ({@link SpiritDiveReelRules#vertices}); {@link #vertices} never exceeds the REEL Wind dive's count at any tier, level of detail or stage (the
 * unit test sums both tables), so the PACK look is never dearer than what it replaces.</p>
 */
public final class SpiritDiveWindRules {
    private SpiritDiveWindRules() {}

    /** The client option {@code spiritDive.windLook}: which Wind the default look draws. The ordinal is not stored anywhere (a text option), but the values are append only. */
    public enum WindLook { PACK, REEL }

    /** The default of the option. */
    public static final WindLook DEFAULT_LOOK = WindLook.PACK;

    /** Parses the option text; anything unknown is the default (PACK). */
    public static WindLook parse(String s) {
        if (s == null) return DEFAULT_LOOK;
        for (WindLook l : WindLook.values()) if (l.name().equalsIgnoreCase(s.trim())) return l;
        return DEFAULT_LOOK;
    }

    // ------------------------------------------------------------------ the textures (the generator is tools/gen_spirit_dive_wind_textures.py)
    public static final String LEAF = "aura/sdw_leaf", LEAF_GLOW = "aura/sdw_leaf_glow", CROSS = "aura/sdw_cross", BAR = "aura/sdw_bar", RING = "aura/sdw_ring", WRIST = "aura/sdw_wrist",
            CIRCLE = "aura/sdw_circle", SWORD = "aura/sdw_sword", GUARD = "aura/sdw_guard", BOW = "aura/sdw_bow", ARROW = "aura/sdw_arrow", STAR = "aura/sdw_star";

    /** Every texture path the look uses (the asset test checks that each file exists and is clean under SafeAdditive where it is drawn additive). */
    public static java.util.List<String> allTextures() { return java.util.List.of(LEAF, LEAF_GLOW, CROSS, BAR, RING, WRIST, CIRCLE, SWORD, GUARD, BOW, ARROW, STAR); }

    /** The textures of the REEL look the PACK look draws too (the ground ring, the soft glow): they must stay. */
    public static java.util.List<String> sharedTextures() { return java.util.List.of(SpiritDiveReelRules.GROUND, SpiritDiveReelRules.RIBBON, SpiritDiveReelRules.RING_TEX, SpiritDiveReelRules.FLASH); }

    // ------------------------------------------------------------------ the colours (pale green, white and a little teal: no blue)
    private static final Pal PAL_STANDARD = new Pal(0xB8FFC8, 0x86FFA6, 0xF2FFF4, 0xE8FFEE, 0xD6FFE2);
    private static final Pal PAL_CHARACTER = new Pal(0xC4FFD2, 0x9CFFB6, 0xF6FFF8, 0xF0FFF4, 0xE0FFE8);

    /** The palette of the tier: the body wash, the glow, the bright light (bars, ribbons), the rim of the body and the mint that stands where the REEL look has gold. */
    public static Pal pal(boolean character) { return character ? PAL_CHARACTER : PAL_STANDARD; }

    /** The colour of the cage of circles of the entry (REEL's is cream; here a pale mint). */
    public static final int CAGE_RGB = 0xE6FFEA;

    // ------------------------------------------------------------------ the weapon
    /** The two spirit weapons of the character tier. */
    public enum Weapon { SWORD, BOW }

    /**
     * Which weapon Yuno holds: Zephyr's sword at rest, Boreas' bow with its arrow while a page of the grimoire is being cast (the client already knows that from the cast animation clips the server sends: no
     * packet; the dive does not know WHICH page, so every cast draws the bow).
     */
    public static Weapon weapon(boolean casting) { return casting ? Weapon.BOW : Weapon.SWORD; }

    // ------------------------------------------------------------------ the tiers and the levels of detail
    /**
     * What one Wind dive draws at a level of detail (counts are per dive unless the field says so).
     *
     * @param bars        crown bars (each a quad)
     * @param rings       crown ring bands (each four outward quads)
     * @param leafSegs    segments of a leaf strip
     * @param crosses     ornate crosses on the leaves
     * @param wrists      wrist circles (each a plane seen from both sides and a sprite that faces the viewer)
     * @param backCircle  the big circle of crosses behind the back
     * @param weapon      the spirit weapon
     * @param sparks      four-point sparkles round the body
     * @param ribbons     steady white ribbons of wind (one pass)
     * @param ribbonSegs  segments of a steady ribbon
     * @param shellPasses passes of the body light ({@link SpiritDiveAnimeRules#shellPasses})
     */
    public record Spec(int bars, int rings, int leafSegs, int crosses, int wrists, boolean backCircle, boolean weapon, int sparks, int ribbons, int ribbonSegs, int shellPasses) {}

    private static final Spec NONE = new Spec(0, 0, 0, 0, 0, false, false, 0, 0, 0, 0);

    public static Spec spec(boolean character, Lod lod) {
        return switch (lod) {
            case FULL -> character ? new Spec(SpiritDiveWindGeometry.BARS_MAX, 2, 10, 2, 2, true, true, 8, 3, 12, 3) : new Spec(SpiritDiveWindGeometry.BARS_STANDARD, 1, 10, 1, 1, false, false, 4, 2, 12, 3);
            case MID -> new Spec(SpiritDiveWindGeometry.BARS_STANDARD, 1, 6, 0, 1, false, false, 0, 0, 0, 2);
            case FAR -> new Spec(0, 0, 0, 0, 0, false, false, 0, 0, 0, 1);
            case CULLED -> NONE;
        };
    }

    /** Leaves of a side: bit 0 the big leaf, bit 1 the small one. The standard tier: the big leaf on the left, the small one on the right; the character tier both on both sides (at full detail). */
    public static int leafMask(boolean character, Lod lod, boolean left) {
        if (lod == Lod.FAR || lod == Lod.CULLED) return 0;
        if (character && lod == Lod.FULL) return 3;
        return left ? 1 : 2;
    }

    private static int bits(int m) { return (m & 1) + ((m >> 1) & 1); }

    /** Leaves drawn by a dive at a stage (the first dive of Wind draws the left side only). */
    public static int leaves(boolean character, Lod lod, Stage st) {
        boolean half = SpiritDivePieces.halfBody(Element.WIND, st);
        return bits(leafMask(character, lod, true)) + (half ? 0 : bits(leafMask(character, lod, false)));
    }

    /** Crosses drawn: one on each big leaf that is drawn, at most the spec's count. */
    public static int crossesDrawn(boolean character, Lod lod, Stage st) {
        boolean half = SpiritDivePieces.halfBody(Element.WIND, st);
        int n = (leafMask(character, lod, true) & 1) + (half ? 0 : (leafMask(character, lod, false) & 1));
        return Math.min(n, spec(character, lod).crosses());
    }

    // ------------------------------------------------------------------ counting
    /** Vertices of a sprite, a plane seen from both sides (a ring), a band of four outward quads. */
    public static final int SPRITE = 4, RING_VERTS = 8, BAND = 16;
    /** The most vertices of a weapon: the sword is a strip, its glow strip and the guard (12); the bow is a strip, its glow, the arrow and its glow (16). */
    public static final int SWORD_VERTICES = 3 * SPRITE, BOW_VERTICES = 4 * SPRITE;

    /** Arms of the shell mask of a stage (the wrist circles sit on them). */
    public static int arms(Stage st) { return SpiritDiveReelRules.arms(Element.WIND, st); }

    /**
     * The most vertices one Wind dive of this tier draws at a level of detail, counted the way the painter draws: the body light, the crown (bars, ring bands), the wrist circles, the
     * leaves (two passes of strips) and their crosses, and, in the full stage, the circle behind the back, the weapon, the sparkles, the steady ribbons, the ground ring, and, only during the entry, the
     * ribbons, the cage of circles and the flash ({@link SpiritDiveReelRules#entryVertices}).
     */
    public static int vertices(boolean character, Lod lod, Stage st) {
        if (lod == Lod.CULLED) return 0;
        Spec s = spec(character, lod);
        SpiritDiveReelRules.Spec reel = SpiritDiveReelRules.spec(Element.WIND, character, lod);
        boolean full = SpiritDivePieces.full(st), half = SpiritDivePieces.halfBody(Element.WIND, st);
        int v = SpiritDiveAnimeRules.shellVertices(Element.WIND, st, lod);
        if (SpiritDiveReelRules.crowned(Element.WIND, st) && s.bars() > 0) v += SpiritDiveWindGeometry.barCount(s.bars(), half) * SPRITE + s.rings() * BAND;
        v += Math.min(s.wrists(), arms(st)) * (RING_VERTS + SPRITE);
        v += leaves(character, lod, st) * s.leafSegs() * 4 * 2 + crossesDrawn(character, lod, st) * SPRITE;
        if (full && s.backCircle()) v += RING_VERTS;
        if (full && s.weapon()) v += Math.max(SWORD_VERTICES, BOW_VERTICES);
        if (full) v += s.sparks() * SPRITE + s.ribbons() * s.ribbonSegs() * 4;
        v += RING_VERTS;                                                                                       // the ground ring
        if (lod != Lod.FAR) v += SpiritDiveReelRules.ribbonVertices(reel);
        if (lod == Lod.FULL) v += SpiritDiveReelRules.entryVertices(reel) - SpiritDiveReelRules.ribbonVertices(reel);
        return v;
    }

    /** The worst case over the stages of a tier at the full level of detail. */
    public static int worstVertices(boolean character) {
        int worst = 0;
        for (Stage st : Stage.values()) worst = Math.max(worst, vertices(character, Lod.FULL, st));
        return worst;
    }

    /** True when the tier of this grade draws the character look: the same rule as the other looks. */
    public static boolean character(Grade g) { return SpiritDiveTier.character(Element.WIND, g); }
}
