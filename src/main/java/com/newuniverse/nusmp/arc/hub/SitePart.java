package com.newuniverse.nusmp.arc.hub;

/**
 * Why: the Chronicle (a monument with its lectern) and the Promenade round the plaza are drawn from a formula, like the arenas, and the formula must not name
 * blocks. A part is a ROLE; {@link ChronicleSite#palette()} says which vanilla block plays it, so a test can read every block name and check it against the game's
 * table of states. {@link #solid} says whether a part stops a walker (the tests prove the entrance to the Chronicle is free and the posts have air above them).
 * Only ever APPEND entries.
 */
public enum SitePart {
    // ---- the Promenade
    PAVE_A(true), PAVE_B(true), PAVE_EDGE(true), POST_TILE(true), LAMP_WALL(true), LAMP(false),
    // ---- the Chronicle
    ROT_FLOOR(true), ROT_CENTER(true), ROT_INLAY(true), COLUMN(true), COLUMN_BAND(true), COLUMN_CAP(true), COLUMN_LAMP(false), LECTERN(true),
    OBELISK_BASE(true), OBELISK(true), OBELISK_BAND(true), OBELISK_LAMP(true), OBELISK_TOP(true);

    private final boolean solid;

    SitePart(boolean solid) { this.solid = solid; }

    /** Does this part stop a walker? */
    public boolean solid() { return solid; }
}
