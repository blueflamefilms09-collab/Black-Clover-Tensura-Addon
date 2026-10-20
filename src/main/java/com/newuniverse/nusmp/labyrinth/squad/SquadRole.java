package com.newuniverse.nusmp.labyrinth.squad;

import java.util.Locale;

/**
 * Why: a squad is only more than a crowd if every member knows what it is FOR, and the rest of the squad code (the plan, the formation, the goals, the morale, the
 * tooltip of the System line) reads that one word. The six roles are the ones of the owner's brief: a TANK steps between the target and the squad's weak members, a
 * melee DPS and a FLANKER hurt the target (the flanker from its side or back), a ranged DPS hurts it from behind the front line, a SUPPORT heals and buffs from the
 * rear, and an optional LEADER is the one the others follow when nothing is in sight (and whose death shakes the squad's morale). It is a pure enum (no Minecraft type)
 * so the planner and the geometry are unit tested, it is saved in the mob's persistent data by {@link #id()} (never by ordinal, so an old save still reads), and entries
 * are only ever appended.
 */
public enum SquadRole {
    TANK("tank"),
    DPS_MELEE("melee"),
    DPS_RANGED("ranged"),
    FLANKER("flanker"),
    SUPPORT("support"),
    LEADER("leader");

    private final String id;

    SquadRole(String id) { this.id = id; }

    /** The lower-case id saved in persistent data and printed in the System line. */
    public String id() { return id; }

    /** The role with this id (any case, trimmed), or null for null or an unknown id: a stack saved by a future version must still load. */
    public static SquadRole fromId(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT);
        for (SquadRole r : values()) if (r.id.equals(s)) return r;
        return null;
    }

    /** Does this role stay at the front of the squad (it walks into the fight first)? */
    public boolean isFrontLine() { return this == TANK || this == LEADER; }

    /** Does this role stay behind the others (it is the squad's soft part, which a tank covers)? */
    public boolean isRear() { return this == SUPPORT || this == DPS_RANGED; }

    /** Does this role hurt the target with melee blows? */
    public boolean isMeleeDamage() { return this == DPS_MELEE || this == FLANKER || this == LEADER; }
}
