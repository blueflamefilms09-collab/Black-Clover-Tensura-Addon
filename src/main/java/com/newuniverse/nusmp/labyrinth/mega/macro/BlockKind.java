package com.newuniverse.nusmp.labyrinth.mega.macro;

/**
 * Why: the whole Hub (plains, Holy Spire, Abyssal Chasm) is computed by PURE code ({@link HubModel}) so that it can be unit tested, previewed as
 * PNG slices and walked by a BFS without a game, and the part that needs Minecraft ({@link MacroBlocks}) is a single lookup table from this enum to
 * block states. Keeping the vocabulary a small CLOSED set also forces every landmark to use the same few vanilla blocks (the Vanilla Block Primacy
 * Rule) and makes the two tilesets plain data. Pure: no Minecraft import. The only block of the mod in the list is {@link #GATE}, which maps to the
 * existing {@code nusmp:labyrinth_gate}.
 *
 * <p>{@link #NATIVE} is a pseudo kind: inside a tile pattern it means "whatever rock the model puts here" (stone, deepslate, blackstone by depth),
 * and {@link HubModel#kindAt} resolves it, so it is never returned to a caller.
 *
 * <p>The flags drive the walkability test: {@code supportsStanding} (a solid block or a bottom slab: a player can stand on it) and {@code passable}
 * (a player walks through it: air and the no-collision plants). Everything else is an obstacle.
 */
public enum BlockKind {
    AIR(false, false, true),

    // terrain
    GRASS(true, false, false), DIRT(true, false, false), STONE(true, false, false), DEEPSLATE(true, false, false), BEDROCK(true, false, false),

    // road and lamp posts
    PATH(true, false, false), POST(false, false, false), LANTERN(false, false, false), SOUL_LANTERN(false, false, false),

    // the Holy Spire: white stone, gold trim, glass
    QUARTZ_BRICKS(true, false, false), SMOOTH_QUARTZ(true, false, false), CALCITE(true, false, false), GOLD_TRIM(true, false, false),
    GLASS(true, false, false), QUARTZ_PILLAR(true, false, false), CHISELED_QUARTZ(true, false, false),
    QUARTZ_SLAB(false, true, false), SMOOTH_QUARTZ_SLAB(false, true, false),
    QUARTZ_STAIR_N(true, false, false), QUARTZ_STAIR_E(true, false, false), QUARTZ_STAIR_S(true, false, false), QUARTZ_STAIR_W(true, false, false),

    // the spiral road of the Abyssal Chasm (deepslate bricks)
    RAMP_BLOCK(true, false, false), RAMP_SLAB(false, true, false),

    // railings (their connections are computed from the neighbours by MacroBlocks)
    RAIL_LIGHT(false, false, false), RAIL_DARK(false, false, false), RAIL_CHAIN(false, false, false),

    // the Chasm's rock and caverns
    BLACKSTONE(true, false, false), BASALT(true, false, false), COBBLED_DEEPSLATE(true, false, false), DRIPSTONE_BLOCK(true, false, false),
    SCULK(true, false, false), MOSS(true, false, false), MOSS_CARPET(false, false, true), GLOW_LICHEN(false, false, true),
    POINTED_DRIPSTONE_UP(false, false, false), POINTED_DRIPSTONE_DOWN(false, false, false), WATER(false, false, false),

    // the two polarity gates: the plane of the existing Labyrinth Gate block and its gold frame
    GATE_FRAME(true, false, false), GATE(false, false, true),

    /** Pseudo kind used inside tile patterns only: "the rock of the surrounding stratum". Never returned by {@link HubModel#kindAt}. */
    NATIVE(true, false, false);

    /** A full block a player can stand on (and that blocks movement). */
    public final boolean solid;
    /** A bottom slab: a player can stand on it (its top is half a block up), it blocks nothing above it. */
    public final boolean slab;
    /** A player walks through it (air, plants without collision, the gate plane itself). */
    public final boolean passable;

    BlockKind(boolean solid, boolean slab, boolean passable) {
        this.solid = solid;
        this.slab = slab;
        this.passable = passable;
    }

    /** A player can stand on top of this block. */
    public boolean supportsStanding() { return solid || slab; }

    /** One of the rail kinds whose connections depend on the neighbours. */
    public boolean isRail() { return this == RAIL_LIGHT || this == RAIL_DARK || this == RAIL_CHAIN; }

    /** The four stair kinds. */
    public boolean isStair() { return this == QUARTZ_STAIR_N || this == QUARTZ_STAIR_E || this == QUARTZ_STAIR_S || this == QUARTZ_STAIR_W; }
}
