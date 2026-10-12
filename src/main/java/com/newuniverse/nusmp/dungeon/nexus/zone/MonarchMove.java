package com.newuniverse.nusmp.dungeon.nexus.zone;

/**
 * What the Monarch can do besides strike. Each move is telegraphed (a ground ring, marked circles, a drawn line, a gathering of shadow) and
 * has a counter: jump the shockwave, step out of the circles, step off the line, kill or outlast the soldiers, leave the field or hold a Mana Zone.
 */
public enum MonarchMove {
    /** Domain Slam: a shockwave ring spreading over the floor from the Monarch; jump it. */
    SLAM,
    /** Shadow Rain: circles marked on the floor that erupt a moment later; leave them. */
    RAIN,
    /** Summon Shadow Soldiers: real daemons stepping out of its shadow; they die with it. */
    SUMMON,
    /** Soul Rend: a long line drawn across the hall, then cut; step off it. */
    BEAM,
    /** Monarch's Authority: a field of heavy air round it that slows everything inside; leave it or hold a Mana Zone. */
    AUTHORITY
}
