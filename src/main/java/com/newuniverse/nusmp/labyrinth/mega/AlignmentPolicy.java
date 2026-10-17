package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the Directional Lock ("once you clear Floor +1 or -1 you cannot enter the opposite polarity's staircases or portals") is ONE rule that three
 * different doors must obey: the Hub's polarity gates, the chain travel of the generator, and any old Gate or Maw that now leads into the dungeon. If
 * each door implemented it, the rule would drift; if each door imported the alignment code, the builders would depend on each other. So the rule lives behind
 * this one-method interface: the alignment module registers its implementation ({@link MegaDungeon#setAlignmentPolicy}), every door asks
 * {@link MegaDungeon#mayEnter}, and with nothing registered the answer is yes (a server without the lock behaves like 1.54.0).
 */
public interface AlignmentPolicy {
    /** May this player take the road of the polarity? The implementation tells the player why when it refuses (the door adds no message of its own). */
    boolean mayEnter(ServerPlayer player, Polarity polarity);
}
