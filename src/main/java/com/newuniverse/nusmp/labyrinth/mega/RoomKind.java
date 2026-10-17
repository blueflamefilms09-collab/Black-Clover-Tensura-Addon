package com.newuniverse.nusmp.labyrinth.mega;

/**
 * Why: the encounter director, the rest system, the factions and the audit all ask "what kind of room is this?", and the answer must come
 * from ONE place that costs the template builders nothing: the directory a template lives in. {@code structure/mega/safe/hearth_hall.nbt}
 * is a SAFE room because of the {@code safe/} folder, so dropping a new template into the right folder is the whole registration (no Java,
 * no JSON), and the assembler, the pools and the saved {@link Room}s all derive the kind with {@link #of}. Pure (no Minecraft types).
 */
public enum RoomKind {
    /** The safe hall at the start of every sector (arrival, exit gate, rest anchors). */
    LANDING("landing"),
    /** A Safe Zone / Break Level: rest anchors, no spawns. */
    SAFE("safe"),
    CORRIDOR("corridor"), JUNCTION("junction"),
    /** A furnished room (cells, crypt, library, forge ...). */
    HALL("hall"),
    /** A grand room, at most one per sector (the Cathedral Rotunda). */
    SET_PIECE("set"),
    CAVERN("cavern"), TRIAL("trial"), TRAP("trap"), PUZZLE("puzzle"), TREASURE("treasure"),
    /** The Labyrinth Guardian's hall. */
    ARENA("arena"),
    /** The room behind the sector's Gatekeeper door: the Vault door on one side, the Way Onward on the other. */
    ANTECHAMBER("antechamber"),
    /** The Crucible Vault. */
    VAULT("vault"),
    /** The corridor that ends in the rubble seal where the next sector attaches. */
    ONWARD("onward"),
    /** A straight tunnel between two sectors (fallback when the next region would collide with an old one). */
    LINK("link"),
    /** A 3x4x2 door cap / niche (never stored as a room). */
    CAP("cap"),
    OTHER("");

    private final String folder;

    RoomKind(String folder) { this.folder = folder; }

    /** The folder under {@code structure/mega/} this kind lives in ({@code ""} for {@link #OTHER}). */
    public String folder() { return folder; }

    public boolean isSafe() { return this == LANDING || this == SAFE; }

    /**
     * Corridors, junctions, links and onward pieces: rooms whose job is to carry traffic. They are not "dead" for the monsters: the corridor and
     * junction templates do carry a few {@code spawn} and {@code patrol} markers, so the Dungeon Master can meet a party between two halls; what they
     * never hold is loot, a boss, a seat or a gate.
     */
    public boolean isCirculation() { return this == CORRIDOR || this == JUNCTION || this == LINK || this == ONWARD; }

    /**
     * The kind of a template id such as {@code nusmp:mega/safe/hearth_hall} or {@code mega/safe/hearth_hall}: the first path segment after
     * {@code mega/}. Anything else is {@link #OTHER}.
     */
    public static RoomKind of(String templateId) {
        if (templateId == null) return OTHER;
        String p = templateId;
        int colon = p.indexOf(':');
        if (colon >= 0) p = p.substring(colon + 1);
        if (!p.startsWith("mega/")) return OTHER;
        p = p.substring(5);
        int slash = p.indexOf('/');
        String first = slash < 0 ? p : p.substring(0, slash);
        for (RoomKind k : values()) if (!k.folder.isEmpty() && k.folder.equals(first)) return k;
        return OTHER;
    }
}
