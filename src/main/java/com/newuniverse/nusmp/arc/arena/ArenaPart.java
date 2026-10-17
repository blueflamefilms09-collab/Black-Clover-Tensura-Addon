package com.newuniverse.nusmp.arc.arena;

/**
 * Why: six arenas (a ruin, a throne room, a court, a plain, a pit, a ring) are drawn by six different pieces of arithmetic, and the game must turn every
 * cell they produce into a block without knowing which arena it is. A PART is that common vocabulary: a ROLE ("the floor", "a pillar", "a lantern on its post",
 * "the way home"), not a block; each arena's palette says which vanilla block plays each role in its own colours, so the ruin's floor is cracked stone bricks
 * and the court's floor is quartz while the shapes stay free of block names. Each part also says whether it STOPS a walker ({@link #solid}): the tests walk the
 * columns to prove an arena is closed (nothing leads off its edge), that the boss, the arrival and the sigils stand on something and have air above them, and
 * a lantern or a flower must not count as ground. Only ever APPEND entries (nothing is saved by ordinal, but the palettes and tests name them).
 */
public enum ArenaPart {
    // ---- the ground
    FOUNDATION(true), FLOOR_A(true), FLOOR_B(true), FLOOR_C(true), FLOOR_CRACKED(true), FLOOR_MOSSY(true), INLAY(true), INLAY_B(true), LAMP_FLOOR(true),
    GRASS(true), DIRT(true), PATH(true), COARSE(true),
    // ---- the dais and the steps (STEP_X ascends towards X: its high side is on the X side)
    DAIS_BASE(true), DAIS_TOP(true), DAIS_EDGE(true), DAIS_RUNE(true), STEP_N(true), STEP_E(true), STEP_S(true), STEP_W(true), THRONE(true),
    // ---- walls, pillars, roofs, trees and stones
    WALL(true), WALL_B(true), WALL_CAP(true), RUBBLE(true), RUBBLE_B(true), PILLAR(true), PILLAR_BAND(true), PILLAR_CAP(true), CEILING(true), CEILING_LAMP(true),
    HEDGE(true), LOG(true), LEAVES(true), STONE(true), STONE_B(true), POST(true),
    // ---- things that stand on the ground and do not stop a walker
    CARPET(false), LANTERN(false), FLOWER_A(false), FLOWER_B(false), TALL_GRASS(false),
    // ---- the way home (a pad and the block that answers a click)
    SIGIL_PAD(true), SIGIL(true);

    private final boolean solid;

    ArenaPart(boolean solid) { this.solid = solid; }

    /** Does this part stop a walker (is it ground or wall), as opposed to a light, a flower or a carpet that is walked over? */
    public boolean solid() { return solid; }
}
