package com.newuniverse.nusmp.labyrinth.mega.hub;

/**
 * Why: "only passive mobs live in the Sanctuary" is decided in two places that must agree: when a mob is about to spawn ({@code FinalizeSpawnEvent}) and
 * in the sparse sweep that removes a hostile mob which got in another way (a mod that calls {@code addFreshEntity} fires no spawn event at all).
 * Both decide from a handful of facts about the mob (is it hostile, did an admin ask for it, does someone own it, is it named or persistent) and
 * about the switch, so the decisions are plain booleans here, away from the game objects, and the truth tables are unit tested. The exemptions are
 * the owner's: an admin who runs {@code /summon} or uses a spawn egg gets what they asked for, and a mob that belongs to a player (a tamed wolf, a
 * minion extracted with a Shadow Core, a summon) or that someone gave a name tag is never taken away.
 */
public final class HubSpawnPolicy {
    private HubSpawnPolicy() {}

    /** Must this spawn be cancelled? Only a hostile mob, only while the switch is on, and never one an admin asked for or one that has an owner. */
    public static boolean cancels(boolean passiveOnly, boolean hostile, boolean adminSpawn, boolean owned) {
        return passiveOnly && hostile && !adminSpawn && !owned;
    }

    /**
     * Must the sweep remove this mob? The same rule applied to a mob that is already in the world, where how it came is unknown: an admin's mob
     * is recognised by the tag the spawn handler left on it ({@code allowed}) or by being named or persistent.
     */
    public static boolean sweeps(boolean passiveOnly, boolean hostile, boolean owned, boolean named, boolean persistent, boolean allowed) {
        return passiveOnly && hostile && !owned && !named && !persistent && !allowed;
    }
}
