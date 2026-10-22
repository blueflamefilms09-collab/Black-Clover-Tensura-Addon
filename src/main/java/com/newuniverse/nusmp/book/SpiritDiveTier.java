package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;

import java.util.Locale;

/**
 * Spirit Dive remake: the two TIERS of the look and everything about them that can be decided without a game (pure, unit tested by SpiritDiveTierTest). The server picks the tier when the
 * dive starts, from the canon grimoire the mage holds, and sends it inside the style number of the {@code Aura.SPIRIT_DIVE} lease ({@link SpiritDivePieces#style(Element, SpiritDivePieces.Stage, Grade)},
 * bits 4 and 5); the clients draw it. Nothing here touches a player, a level or a client class.
 *
 * <ul>
 *   <li><b>Standard tier</b> (every mage): the remade dive of the element, kept light: the mana skin, a handful of signature pieces, one aura column, a ground ring, a few motes.</li>
 *   <li><b>Character tier</b> (the mage whose bound grimoire is the canon character of the element: Yuno for Wind, Fuegoleon for Fire, Noelle for Water): everything of the standard tier plus the
 *       character's own signature pieces (Yuno's crown halo and wind blades, Fuegoleon's flame mane, cape and flaming arm, Noelle's water ribbons, ring halo and train), a shockwave and a light pillar
 *       on entry, orbiting rune rings, a large ground sigil, rising motes and the colour grading of the character (Yuno green-gold, Fuegoleon crimson-gold, Noelle blue-silver).</li>
 * </ul>
 * Earth has no canon character in the mod (the earth spirit is never named), so it is always the standard tier.
 */
public final class SpiritDiveTier {
    private SpiritDiveTier() {}

    /** Which look a dive draws. The ordinal is part of the style number (bits 4 and 5): append only. */
    public enum Grade { STANDARD, YUNO, FUEGOLEON, NOELLE }

    /** The level of detail the client draws a dive at, from the distance to the camera. */
    public enum Lod { FULL, MID, FAR, CULLED }

    /** The canon character (the {@code CanonBook} id, lower case) that earns the character tier of an element, or null (Earth). */
    public static String characterOf(Element e) {
        return switch (e) {
            case WIND -> "yuno";
            case FIRE -> "fuegoleon";
            case WATER -> "noelle";
            case EARTH -> null;
        };
    }

    /** The grade of the character tier of an element, STANDARD for an element that has none. */
    public static Grade gradeOfElement(Element e) {
        return switch (e) {
            case WIND -> Grade.YUNO;
            case FIRE -> Grade.FUEGOLEON;
            case WATER -> Grade.NOELLE;
            case EARTH -> Grade.STANDARD;
        };
    }

    /**
     * The grade a dive gets: the character tier only when the mage's bound grimoire ({@code canonId}: the id of the CanonBook, any case, null or empty for none) is the character of the
     * element that dives. Yuno's grimoire diving with Wind earns the Yuno look; the same grimoire can not dive with another element anyway, but a stray id never earns another character's look.
     */
    public static Grade gradeFor(Element e, String canonId) {
        if (e == null || canonId == null || canonId.isEmpty()) return Grade.STANDARD;
        String c = characterOf(e);
        return c != null && c.equals(canonId.toLowerCase(Locale.ROOT)) ? gradeOfElement(e) : Grade.STANDARD;
    }

    /** The grade a client really draws: a grade that does not belong to the element (a bad byte) is the standard look. */
    public static Grade effective(Element e, Grade g) { return g != null && g == gradeOfElement(e) ? g : Grade.STANDARD; }

    public static boolean character(Element e, Grade g) { return effective(e, g) != Grade.STANDARD; }

    // ------------------------------------------------------------------ the colours of each look (RGB)
    /** The main glow colour: the standard colour of the element, or the colour grading of the character. */
    public static int glow(Element e, Grade g, boolean saint) {
        return switch (effective(e, g)) {
            case YUNO -> 0xA8FFD8;
            case FUEGOLEON -> 0xFF7A2A;
            case NOELLE -> 0xA8E4FF;
            case STANDARD -> switch (e) {
                case WIND -> 0x98FFD0;
                case WATER -> saint ? 0xFFE49A : 0xCFF8FF;
                case FIRE -> 0xFF8A2A;
                case EARTH -> 0xFFB040;
            };
        };
    }

    /** The second colour (the body of the effect): Yuno's mint, Fuegoleon's crimson, Noelle's blue, the element's own colour for the standard look. */
    public static int primary(Element e, Grade g) {
        return switch (effective(e, g)) {
            case YUNO -> 0x6CF2B0;
            case FUEGOLEON -> 0xE8301A;
            case NOELLE -> 0x5CB4FF;
            case STANDARD -> switch (e) {
                case WIND -> 0x7CE8B8;
                case WATER -> 0x6EBEF4;
                case FIRE -> 0xE8561C;
                case EARTH -> 0xB89A60;
            };
        };
    }

    /** The accent (the highlights): Yuno's pale gold-green, Fuegoleon's gold, Noelle's silver. */
    public static int accent(Element e, Grade g) {
        return switch (effective(e, g)) {
            case YUNO -> 0xF4FFC0;
            case FUEGOLEON -> 0xFFD860;
            case NOELLE -> 0xEEF4FF;
            case STANDARD -> switch (e) {
                case WIND -> 0xE8FFF0;
                case WATER -> 0xF2FCFF;
                case FIRE -> 0xFFD070;
                case EARTH -> 0xFFE8B0;
            };
        };
    }

    // ------------------------------------------------------------------ the budgets of the procedural effects (sprites per paint)
    /**
     * What the procedural effects of one dive may draw in one frame, per tier and level of detail (the pieces are separate: see {@link #pieceCap}). Every number is the most sprites of
     * that kind; a sprite is one quad (4 vertices). The painter reads these and nothing else, so the worst case of a dive is {@link #worstQuads}.
     *
     * @param columns light-column planes round the body (two quads each, front and back)
     * @param pillars the crossed planes of the entry light pillar (two quads each; character tier, only during the entry)
     * @param motes   mana motes streaming into the body
     * @param bits    element bits rising round the body
     * @param strands hair / mane / ribbon strands (each a few segments, see {@link #SEGMENTS})
     * @param spirals helices of the element rising round the body (wind streams, flame ribbons, water ribbons), {@link #SPIRAL_SEGMENTS} quads each
     * @param rings   flat rings (ground sigil, counter ring, rune rings, the entry shockwave): two quads each (both faces)
     * @param flares  camera-facing flares (eyes, hands, the entry flash)
     */
    public record Budget(int columns, int pillars, int motes, int bits, int strands, int spirals, int rings, int flares) {
        public int quads() { return columns * 2 + pillars * 2 + motes + bits + strands * SEGMENTS + spirals * SPIRAL_SEGMENTS + rings * 2 + flares; }
    }

    /** Segments of one strand (a ribbon of this many quads). */
    public static final int SEGMENTS = 4;
    /** Segments of one helix. */
    public static final int SPIRAL_SEGMENTS = 8;

    private static final Budget[][] BUDGET = {
            //            FULL                                            MID                                             FAR                                CULLED
            {new Budget(4, 0, 6, 4, 3, 1, 1, 2), new Budget(2, 0, 2, 0, 0, 0, 1, 0), new Budget(0, 0, 0, 0, 0, 0, 1, 0), new Budget(0, 0, 0, 0, 0, 0, 0, 0)},       // standard tier
            {new Budget(8, 2, 18, 12, 9, 3, 6, 6), new Budget(4, 0, 6, 0, 3, 1, 2, 2), new Budget(0, 0, 0, 0, 0, 0, 1, 0), new Budget(0, 0, 0, 0, 0, 0, 0, 0)},    // character tier
    };

    /** The effect budget of a tier at a level of detail. */
    public static Budget budget(boolean character, Lod lod) { return BUDGET[character ? 1 : 0][lod.ordinal()]; }

    /**
     * The most vertices the geo pieces of one dive may have in a frame, per tier: 24 vertices a cube and two passes (the body and its glow map). Not enforced while drawing: SpiritDiveBudgetTest sums the cubes
     * of the model files of every piece list (every element, stage and tier) and fails if one list is over, so a list can not grow past it unnoticed.
     */
    public static int pieceCap(boolean character) { return character ? 8000 : 6500; }

    /** The most vertices the whole dive of a tier may draw in one frame at the full level of detail (pieces, four passes of the mana skin of at most twelve boxes, and the procedural effects); same test. */
    public static int vertexCap(boolean character) { return character ? 10500 : 8500; }

    /** The worst case of the procedural effects, in quads, of a tier at the full level of detail. */
    public static int worstQuads(boolean character) { return budget(character, Lod.FULL).quads(); }

    // ------------------------------------------------------------------ the levels of detail
    /** Up to this many blocks the whole look is drawn (squared in {@link #lod}). */
    public static final double NEAR = 24.0, MID_RANGE = 48.0, FAR_RANGE = 64.0;

    /**
     * The level of detail at a squared distance from the camera: FULL up to 24 blocks, MID (no small detail pieces, the cheap effects) up to 48, FAR (a ring on the ground) up to 64 and CULLED beyond.
     * The character tier shrinks to the standard tier beyond {@link #NEAR} (the effects of far players are skipped, the signature pieces stay up to {@link #MID_RANGE}).
     */
    public static Lod lod(double distSq) {
        if (distSq <= NEAR * NEAR) return Lod.FULL;
        if (distSq <= MID_RANGE * MID_RANGE) return Lod.MID;
        if (distSq <= FAR_RANGE * FAR_RANGE) return Lod.FAR;
        return Lod.CULLED;
    }

    /** True when the character extras (rings, shockwave, extra motes) may draw at this level: only the full level of detail. */
    public static boolean extras(Lod lod) { return lod == Lod.FULL; }

    /** How many dives of the character tier one frame may draw as such (the rest draw the standard tier): the nearest ones come first in the render order of the layer. */
    public static final int CHARACTER_PER_FRAME = 3;

    // ------------------------------------------------------------------ the particle trail (client side, through the client particle budget)
    /** Dives within this many blocks of the camera trail particles, and at most {@link #TRAIL_PLAYERS} dives per client tick do. */
    public static final double TRAIL_RANGE = 24.0;
    public static final int TRAIL_PLAYERS = 6;

    /** Particles one diving player spawns this client tick: the standard tier one every third tick (staggered by entity id), the character tier two every tick. */
    public static int trailCount(boolean character, long tick, int entityId) {
        if (character) return 2;
        return Math.floorMod(tick + entityId, 3L) == 0 ? 1 : 0;
    }

    /** The burst of particles on the tick after a dive begins: the character tier's entry is a big one. */
    public static int entryBurst(boolean character) { return character ? 28 : 8; }

    /** The most particles per second the trails of all dives in view can ask for (before the client particle budget and the effect quality thin them): players * per tick * 20. */
    public static int worstTrailPerSecond() { return TRAIL_PLAYERS * 2 * 20; }

    // ------------------------------------------------------------------ the entry
    /** Ticks the entry (the shockwave and the light pillar) lasts from the start of the lease. */
    public static final int ENTRY_TICKS = 22;

    /** 0..1 progress of the entry at tick {@code t} of the dive (1 = over). */
    public static float entry(float t) { return t <= 0f ? 0f : Math.min(1f, t / ENTRY_TICKS); }
}
