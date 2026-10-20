package com.newuniverse.nusmp.labyrinth.loot;

/**
 * Why: the three families of loot gear differ in where they are worn, and that decides the rules of their stats: a WEAPON is held (its attribute modifiers apply in the main
 * hand), a piece of ARMOUR is worn in an armour slot (its modifiers apply there), and a TRINKET is worn in a Curios slot (nothing in vanilla applies a modifier for it, so
 * the mod's bridge does). Pure.
 */
public enum GearKind { WEAPON, ARMOUR, TRINKET }
