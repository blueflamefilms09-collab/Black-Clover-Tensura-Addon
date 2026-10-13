package com.newuniverse.nusmp.dungeon.nexus.zone;

/**
 * What the Blood-Red Commander can do besides strike. Each move is telegraphed (a ground ring, marked circles, a drawn line, a gathering of
 * shadow, a red mark on the floor) and has a counter: jump the shockwave, step out of the circles, step off the line, kill or outlast the
 * soldiers, leave the field or hold a Mana Zone. The entries are only ever appended: the first five are the Monarch's moves of 1.21.0, the last
 * three are the fast weapon arts of the Commander.
 */
public enum MonarchMove {
    /** Domain Slam: a shockwave ring spreading over the floor from the Commander; jump it. */
    SLAM,
    /** Shadow Rain: circles marked on the floor that erupt a moment later; leave them. */
    RAIN,
    /** Summon Shadow Soldiers: real daemons stepping out of its shadow; they die with it. Started by the fight at 50% and 25% of its health, never chosen at random. */
    SUMMON,
    /** Soul Rend: a long line drawn across the hall, then cut; step off it. */
    BEAM,
    /** Monarch's Authority: a field of heavy air round it that slows everything inside; leave it or hold a Mana Zone. */
    AUTHORITY,
    /** Crimson Cross: two long lines crossing in an X are drawn on the floor under the target, then cut; step off the lines or jump them. Two crosses in the second phase, three in the third. */
    CROSS_SLASH,
    /** Blood-Red Lunge: a red lane is marked towards the target, then the Commander dashes down it and stops at its end, open to attack; leave the lane. */
    LUNGE,
    /** Executioner's Arc: a huge half-circle in front of it is marked on the floor, then cut (and, from the second phase, cut back); jump it or get outside its radius. */
    SWEEP;

    /** The three fast weapon arts: short wind-ups (12 to 20 ticks) under a very readable red mark. */
    public boolean isArt() { return this == CROSS_SLASH || this == LUNGE || this == SWEEP; }
}
