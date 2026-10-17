package com.newuniverse.nusmp.avatar.ai;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Why: an item a player THREW has the player as its thrower, so the looting rule "never take what a player dropped a moment ago" can see it. A player's belongings
 * after a death have no thrower at all (the game drops them without one), and an Avatar that gathered a dead player's pile within seconds would be a thief. So the
 * places where players died lately are remembered for five minutes (the last 32 of them, in memory only), and anything lying within six blocks of one is treated as
 * theirs and never taken by an Avatar. Server side.
 */
public final class Graves {
    private Graves() {}

    private record Grave(ResourceKey<Level> dimension, Vec3 at, long when) {}

    private static final Deque<Grave> RECENT = new ArrayDeque<>();
    private static final long KEEP_TICKS = 6000L;
    private static final double RADIUS_SQ = 6.0 * 6.0;
    private static final int MAX = 32;

    /** Remembers a player's death (a game-bus listener). */
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        synchronized (RECENT) {
            RECENT.addLast(new Grave(p.level().dimension(), p.position(), p.level().getGameTime()));
            while (RECENT.size() > MAX) RECENT.removeFirst();
        }
    }

    /** Lies the position within six blocks of a place where a player died in the last five minutes, in this dimension? */
    public static boolean near(Level level, Vec3 pos) {
        if (level == null || level.isClientSide || RECENT.isEmpty()) return false;
        long now = level.getGameTime();
        synchronized (RECENT) {
            RECENT.removeIf(g -> now - g.when() > KEEP_TICKS);
            for (Grave g : RECENT) if (g.dimension() == level.dimension() && g.at().distanceToSqr(pos) <= RADIUS_SQ) return true;
        }
        return false;
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        synchronized (RECENT) { RECENT.clear(); }
    }
}
