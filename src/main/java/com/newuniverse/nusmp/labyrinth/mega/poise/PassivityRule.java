package com.newuniverse.nusmp.labyrinth.mega.poise;

/**
 * Why: the dungeon's monsters are ONE side. They exist to hunt the player; a goblin must not wage war on an ogre while the party stands in the next room, and a skeleton's arrow must not
 * hurt the zombie in front of it. This is the pure decision of that rule, so the owner's switches are tested: when {@code passive} (the master switch) is on, a monster of the dungeon may not
 * target or hurt another monster of the dungeon UNLESS the old quarrel system that owns that attacker is switched on (the eco turf wars for ordinary monsters, the foundry's faction
 * wars for foundry mobs). Servants of players, the player's pets and the player are never "dungeon monsters", so they are never protected from the monsters and never blocked from them.
 */
public final class PassivityRule {
    private PassivityRule() {}

    public static boolean blocks(boolean passive, boolean attackerDungeonMob, boolean victimDungeonMob, boolean attackerFoundry, boolean turfWars, boolean factionWars) {
        if (!passive || !attackerDungeonMob || !victimDungeonMob) return false;
        return attackerFoundry ? !factionWars : !turfWars;
    }
}
