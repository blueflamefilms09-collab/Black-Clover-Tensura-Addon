package com.newuniverse.nusmp.labyrinth.mega.rift;

/**
 * Why: the four things a tear in the dungeon can be, in one enum so the block entity, the renderer, the commands and the saved data agree on names and numbers. SHORTCUT (Rift Type B): a
 * hole to a floor 3 to 5 floors on; POCKET (Rift Type A): a door to an instanced hidden realm that collapses; RETURN: the Return Portal that opens on Floor 0 and leads a player to the exact
 * block where they left off; EXIT: the way out of a pocket back to the place it was entered from. The {@code id} is what is saved (never the ordinal, so a type can be added later).
 */
public enum RiftType {
    SHORTCUT("shortcut", 0x7FE8FF), POCKET("pocket", 0xC060FF), RETURN("return", 0xFFD060), EXIT("exit", 0xA0FFB0);

    private final String id;
    private final int rgb;

    RiftType(String id, int rgb) {
        this.id = id;
        this.rgb = rgb;
    }

    public String id() { return id; }

    /** The colour of its arcs and its aura, 0xRRGGBB. */
    public int rgb() { return rgb; }

    public static RiftType byId(String id) {
        for (RiftType t : values()) if (t.id.equals(id)) return t;
        return SHORTCUT;
    }
}
