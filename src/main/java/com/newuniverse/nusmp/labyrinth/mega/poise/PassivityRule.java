package com.newuniverse.nusmp.labyrinth.mega.poise;

/**
 * Why: the dungeon's monsters are ONE side. They exist to hunt the player; a goblin must not wage war on an ogre while the party stands in the next room, and a skeleton's arrow must not
 * hurt the zombie in front of it. This is the pure decision of that rule, so the owner's switch is tested: when {@code passive} (the master switch {@code foundry.poise.dungeonPassive}) is on, a
 * monster of the dungeon may not target or hurt another monster of the dungeon, whatever the old quarrel switches say (a server config file written by an older version still holds
 * {@code turfWarsEnabled = true}, and the owner's default must win over a value nobody chose). To get the old quarrels back, set the master switch to false: then the old switches
 * ({@code eco.turfWarsEnabled}, {@code foundry.factionWars}, both default false) decide again. Servants of players, the player's pets and the player are never "dungeon monsters", so they
 * are never protected from the monsters and never blocked from them.
 */
public final class PassivityRule {
    private PassivityRule() {}

    public static boolean blocks(boolean passive, boolean attackerDungeonMob, boolean victimDungeonMob) {
        return passive && attackerDungeonMob && victimDungeonMob;
    }

    /** Do the old quarrel systems (the eco turf wars, the foundry's rival goal) run at all? Only when the master switch is off AND their own switch is on. */
    public static boolean quarrelsRun(boolean passive, boolean ownSwitch) {
        return !passive && ownSwitch;
    }
}
