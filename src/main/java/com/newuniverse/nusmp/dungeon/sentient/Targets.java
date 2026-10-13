package com.newuniverse.nusmp.dungeon.sentient;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Who the Dungeon goes for: players in survival or adventure, alive, not spectating. */
final class Targets {
    private Targets() {}

    static boolean valid(Player p) { return p != null && p.isAlive() && !p.isSpectator() && !p.isCreative(); }

    /** The nearest valid player of the level within the range, or null. */
    static ServerPlayer nearest(ServerLevel level, Entity from, double range) { return nearest(level, from.getX(), from.getY(), from.getZ(), range); }

    /** The nearest valid player of the level within the range of a point, or null. */
    static ServerPlayer nearest(ServerLevel level, double x, double y, double z, double range) {
        ServerPlayer best = null;
        double bestD = range * range;
        for (ServerPlayer p : level.players()) {
            if (!valid(p)) continue;
            double d = p.distanceToSqr(x, y, z);
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** Is any player (a spectator excepted) within the range of the entity? */
    static boolean anyPlayerWithin(ServerLevel level, Entity from, double range) {
        double r2 = range * range;
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && from.distanceToSqr(p) <= r2) return true;
        return false;
    }
}
