package com.newuniverse.nusmp.labyrinth.props;

/**
 * Why: a rack is one of three things, and what a hand may do to it follows from that. DECOR is the dungeon's furniture (a converted armour stand): it only shows its items, nobody takes them. LOOT is a
 * loot-filled variant (themed, see {@link RackFamilies}): the items on it are a prize and can be taken one by one, but nothing can be put back. OPEN is the rack a player crafts and places: take
 * and put. The id is what the block entity saves; an unknown id reads as DECOR (the safe one) so a corrupt tag never opens a rack.
 */
public enum RackMode {
    DECOR(0, false, false), LOOT(1, true, false), OPEN(2, true, true);

    private final int id;
    private final boolean take, put;

    RackMode(int id, boolean take, boolean put) {
        this.id = id;
        this.take = take;
        this.put = put;
    }

    public int id() { return id; }

    public boolean canTake() { return take; }

    public boolean canPut() { return put; }

    public static RackMode byId(int id) {
        for (RackMode m : values()) if (m.id == id) return m;
        return DECOR;
    }
}
