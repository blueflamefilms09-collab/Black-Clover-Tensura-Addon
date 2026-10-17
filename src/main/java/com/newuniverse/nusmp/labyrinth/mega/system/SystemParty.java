package com.newuniverse.nusmp.labyrinth.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Why: the floor quests are walked by a group, and the owner wants "a quest completes for every player present in the floor", but the group concept itself
 * (the dungeon party, an FTB Team) is another builder's wave. So the System asks ONE question through ONE seam, {@link #members}: "who plays together with this
 * player?", and does not know how it is answered. Until the party layer exists the answer is the simplest honest one, the players standing in the same
 * SECTOR of the Mega-Dungeon (a sector is one floor since 1.58.0, so these are the players on the same floor); the party builder replaces it with
 * {@link #setProvider} (the FTB Teams bridge), and nothing in the System changes. The answer always contains the player themselves, first, and never contains
 * a player twice, whatever a provider returns; a provider that throws or returns null falls back to the default for that call, so a broken party layer can
 * never break a quest. Server thread only.
 */
public final class SystemParty {
    private SystemParty() {}

    /** Who plays together with a player. The list should contain the player; {@link #members} makes sure it does. */
    @FunctionalInterface
    public interface Provider {
        List<ServerPlayer> membersOf(ServerPlayer player);
    }

    private static volatile Provider provider = SystemParty::sectorMates;

    /** Replaces the way members are found (the party builder calls this once); null restores the default (players of the same sector). */
    public static void setProvider(Provider p) { provider = p == null ? SystemParty::sectorMates : p; }

    /** The player and everyone who plays with them, the player first, each once. Never null, never empty. */
    public static List<ServerPlayer> members(ServerPlayer player) {
        List<ServerPlayer> found;
        try {
            found = provider.membersOf(player);
        } catch (RuntimeException broken) {
            found = null;
        }
        if (found == null) found = sectorMates(player);
        return PartyRules.sameGroup(player, found, m -> Boolean.TRUE);
    }

    /** The default: the players of the same sector of the Mega-Dungeon; just the player outside it or in a gap between sectors. */
    static List<ServerPlayer> sectorMates(ServerPlayer player) {
        if (!MegaDungeon.in(player.level())) return List.of(player);
        Sector mine = MegaDungeon.sectorAt(player.level(), player.blockPosition());
        if (mine == null) return List.of(player);
        return PartyRules.sameGroup(player, player.serverLevel().players(), m -> {
            Sector s = MegaDungeon.sectorAt(m.level(), m.blockPosition());
            return s == null ? null : s.id;
        });
    }
}
