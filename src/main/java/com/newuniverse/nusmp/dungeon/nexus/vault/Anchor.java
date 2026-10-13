package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import net.minecraft.world.entity.player.Player;

/** The one place the vault code asks whether a player wears an attuned Band of the Spatial Anchor, which ignores the Dungeon's spatial traps. */
final class Anchor {
    private Anchor() {}

    static boolean worn(Player p) {
        try {
            return Relics.anchored(p);
        } catch (RuntimeException | LinkageError unavailable) {
            return false;                                                    // the relic code failed: the traps apply, as they do to everyone
        }
    }
}
