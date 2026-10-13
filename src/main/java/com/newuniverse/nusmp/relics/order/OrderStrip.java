package com.newuniverse.nusmp.relics.order;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * What the Order does to duplicates: checks that one is still there (COUNT, changes nothing) and takes them (STRIP). Both re-judge every stack of the
 * place with the ledger as it is now, so the valid copy is never taken, whatever was seen earlier; a stack that was a duplicate a minute ago and has
 * become the valid copy since (the ledger was reset, the old copy was lost) is kept.
 */
final class OrderStrip {
    private OrderStrip() {}

    /** The outcome of a STRIP: the relic ids taken (one entry per stack) and how many stacks. */
    record Result(List<String> ids) {
        int removed() { return ids.size(); }

        List<String> distinct() { return new ArrayList<>(new LinkedHashSet<>(ids)); }
    }

    /** How many duplicates are at the place of a sighting now (of any relic: the Order takes every duplicate it finds on a visit). */
    static int count(MinecraftServer server, Sighting s, long tick) {
        int n = 0;
        for (RelicScanner.Job j : RelicScanner.locate(server, RelicScanner.Run.COUNT, s)) {
            RelicScanner.runAll(server, j, tick);
            n += j.duplicateIds.size();
        }
        return n;
    }

    /** Takes every duplicate at the place of a sighting. */
    static Result strip(MinecraftServer server, Sighting s, long tick) {
        List<String> ids = new ArrayList<>();
        for (RelicScanner.Job j : RelicScanner.locate(server, RelicScanner.Run.STRIP, s)) {
            RelicScanner.runAll(server, j, tick);
            ids.addAll(j.removedIds);
        }
        return new Result(ids);
    }

    /** Takes every duplicate a player carries (and has open). */
    static Result stripPlayer(MinecraftServer server, ServerPlayer p, long tick) {
        return strip(server, new Sighting("", Spot.player(p), null, tick), tick);
    }
}
