package com.newuniverse.nusmp.labyrinth.mega.eco;

import java.util.List;

/**
 * Why: the eleven factions are fixed by the Mega-Dungeon contract because three builders meet on their ids: the room templates write
 * {@code nusmp:faction;faction=<id>} markers, the encounter director reads the entity-type tags {@code nusmp:mega/<id>}, and this package ships
 * the data and the tags. The loader takes whatever files it finds (a datapack may add a twelfth faction), so production code never needs this
 * list; it exists so the data test can insist that the eleven agreed ids are all there, in the agreed spelling. Pure (no Minecraft types).
 */
public final class FactionIds {
    private FactionIds() {}

    /** The ids of the contract, in the contract's order. */
    public static final List<String> CONTRACT = List.of("undead", "goblin", "ogre", "beast", "slime", "construct", "cultist", "demon", "elemental", "aquatic", "insect");
}
