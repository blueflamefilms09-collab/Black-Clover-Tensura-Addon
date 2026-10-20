package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

/**
 * Why: the three rarities of a dimensional rift, in one enum so the spawner, the block entity, the renderer, the commands and the tests agree on names. COMMON is the floor shortcut (a tear
 * that drops the delver three to five floors on), RARE is the old pocket (a hidden realm with one secret boss or one puzzle vault), MEGA_RARE is the exclusive pocket dungeon of 1.99.0 (an
 * ancient tomb with its own biome, its own monsters, a mega-boss and relics that drop nowhere else). The {@code id} is what is saved and what a config or a command names; the ordinal is
 * what the client is told. Values are only ever appended.
 */
public enum RiftTier {
    COMMON("common"), RARE("rare"), MEGA_RARE("mega_rare");

    private final String id;

    RiftTier(String id) { this.id = id; }

    public String id() { return id; }

    /** A tier by its saved id; anything unknown is COMMON (a rift of a future version reads as the plainest tear, never as a crash). */
    public static RiftTier byId(String id) {
        for (RiftTier t : values()) if (t.id.equals(id)) return t;
        return COMMON;
    }

    /** A tier by its ordinal, clamped (what the client receives). */
    public static RiftTier byOrdinal(int ordinal) {
        RiftTier[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal))];
    }
}
