package com.newuniverse.nusmp.elite.forge.session;

import com.newuniverse.nusmp.elite.forge.rules.ForgeRng;
import com.newuniverse.nusmp.elite.forge.rules.ForgeRecipeDef;
import com.newuniverse.nusmp.elite.forge.rules.SessionCheck;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Why: the server's book of open forging sessions. When a player starts a recipe at a station the server picks one random seed per minigame step (the client needs
 * them to draw the game), remembers them with the time (milliseconds, from {@link #clock}), the station and the recipe, and asks for nothing more until the client
 * reports what it played; the report is accepted only for the player who started it, only once (the session is removed first), and judged by
 * {@link SessionCheck} against the real time that passed. One session per player (starting another replaces the old one), one per station (a second player gets "in
 * use"); a session older than the longest the recipe allows is dropped by {@link #expire}. The clock is replaceable so a self-test can play a whole session in zero
 * real time. Server thread only (the map is concurrent only to make a stray read harmless).
 */
public final class ForgeSessions {
    private ForgeSessions() {}

    /** One open session. */
    public record Session(int id, UUID player, String recipeId, ResourceKey<Level> dimension, BlockPos pos, StationTier tier, boolean premium,
                          List<SessionCheck.Step> steps, long[] seeds, long startMillis) {}

    private static final Map<UUID, Session> BY_PLAYER = new ConcurrentHashMap<>();
    private static int nextId = 1;

    /** Milliseconds, monotonic. A self-test replaces it. */
    public static volatile LongSupplier clock = () -> System.nanoTime() / 1_000_000L;

    /** Who is forging at the position in the level, or null. */
    public static UUID userAt(Level level, BlockPos pos) {
        for (Session s : BY_PLAYER.values()) if (s.dimension().equals(level.dimension()) && s.pos().equals(pos)) return s.player();
        return null;
    }

    public static boolean activeAt(Level level, BlockPos pos) { return userAt(level, pos) != null; }

    public static Session of(UUID player) { return BY_PLAYER.get(player); }

    public static int openCount() { return BY_PLAYER.size(); }

    /** Opens a session, replacing the player's old one. The caller has checked everything else. */
    public static Session open(UUID player, Level level, BlockPos pos, StationTier tier, ForgeRecipeDef recipe, boolean premium, long baseSeed) {
        long[] seeds = new long[recipe.steps().size()];
        for (int i = 0; i < seeds.length; i++) seeds[i] = ForgeRng.hash(baseSeed, i, 7);
        Session s;
        synchronized (ForgeSessions.class) {
            s = new Session(nextId++, player, recipe.id(), level.dimension(), pos.immutable(), tier, premium, recipe.steps(), seeds, clock.getAsLong());
        }
        BY_PLAYER.put(player, s);
        return s;
    }

    /** Takes the session out of the book if it is the player's and has this id; returns it, else null. */
    public static Session take(UUID player, int id) {
        Session s = BY_PLAYER.get(player);
        if (s == null || s.id() != id) return null;
        BY_PLAYER.remove(player, s);
        return s;
    }

    /** Drops the player's session (logout, abort); returns it or null. */
    public static Session drop(UUID player) { return BY_PLAYER.remove(player); }

    /** Real milliseconds since the session was opened. */
    public static long elapsed(Session s) { return clock.getAsLong() - s.startMillis(); }

    /** Removes sessions older than their limit; returns the dropped ones (so their stations can be darkened). */
    public static List<Session> expire() {
        List<Session> gone = new java.util.ArrayList<>();
        for (Session s : BY_PLAYER.values()) {
            if (elapsed(s) > SessionCheck.maxElapsedMillis(s.steps())) {
                if (BY_PLAYER.remove(s.player(), s)) gone.add(s);
            }
        }
        return gone;
    }

    /** Test helper: forgets everything. */
    public static void clearAll() { BY_PLAYER.clear(); }
}
