package com.newuniverse.nusmp.elite.skills;

/**
 * Why: every elite skill with more than one power names its modes the same way: an id (lang key {@code nusmp.skill.mode.<id>}), an English name that is the lang fallback, the
 * magicule cost and the cooldown in ticks (both BEFORE the elite and pack-wide multipliers). One tiny record shared by the skills and the aspectual magics, written with plain
 * string literals so the lang generator reads them straight from the source.
 */
public record EliteMode(String id, String name, double cost, int cooldown) {
    public static EliteMode of(String id, String name, double cost, int cooldown) { return new EliteMode(id, name, cost, cooldown); }
}
