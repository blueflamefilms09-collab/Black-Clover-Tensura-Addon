package com.newuniverse.nusmp.dungeon.sentient;

/** The scoreboard tags and persistent-data keys the Sentient Dungeon marks its creatures with (they survive a restart with the mob). */
public final class SentientTags {
    private SentientTags() {}

    /** A mob born from a wall of the Dungeon; a Juggernaut of the immune system (it is also born). */
    public static final String BORN = "nusmp_born", JUGGERNAUT = "nusmp_juggernaut";
    /** The mark every floor boss puts on itself: the stones it leaves are the boss grade. */
    public static final String FLOOR_BOSS = "nusmp_floor_boss";
    /** A slime made at home in the water of the Deep Waterways. */
    public static final String AQUA = "nusmp_aqua_slime";
    /** Persistent data of a born mob: the room it was born in (a RoomKey) and the floor kind it was born on (the ordinal of FloorKind). */
    public static final String ROOM = "nusmp_room", FLOOR = "nusmp_floor";
}
