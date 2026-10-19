package com.newuniverse.nusmp.labyrinth.mega.lock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: there are two kinds of place that close on a player (a trial chamber's barriers, which are the block entity of the Sealed Archway, and a mechanic's bars or cage, which are
 * an {@code Instance} of the mechanics library), and the escape rules must treat them alike: the same timers, the same rune, the same messages, the same {@code /dungeon release}.
 * So the engine ({@link LockBook}) knows only this small interface: what the lock is called, where its rune stands, who is inside, the FACTS the pure rules need, and how to open
 * it. The trial chamber implements it directly, a mechanic through a small adapter, and a lock added tomorrow needs only to implement it and call {@link LockBook#keep} while it
 * is closed. Server thread only.
 */
public interface Lockable {
    /** A stable key for the lock ({@code trial:<block position>} or {@code mech:<instance key>}); the rune entities carry it so a click finds its lock again after a restart. */
    String lockKey();

    EscapeRules.Kind lockKind();

    /** An English name for the messages ("the trial chamber", the mechanic's name). */
    String lockName();

    ServerLevel lockLevel();

    /** Where the rune stands: a free cell on the floor of the locked place, as far as possible from its walls. */
    Vec3 runePos();

    /** The living players inside right now. */
    List<ServerPlayer> prisoners();

    /** The facts the rules decide on. Called once per evaluation; an adapter may keep its own clocks here. */
    EscapeRules.Facts facts(long now);

    /** A soft lock is an obstacle that could wall a doorway but is not a seal: no message, no rune, only the longest-time and nobody-inside rules open it. */
    default boolean soft() { return false; }

    /** The timings that apply to this lock (a trial chamber keeps its own reset time as the "nobody inside" time). */
    default EscapeRules.Config config(EscapeRules.Config base) { return base; }

    /** Opens the lock. {@code by} is the player who used the rune or the command, null when a timer opened it. For a boss lock a rune use also sends the prisoners back to the entrance. */
    void open(ServerPlayer by, EscapeRules.Why why);
}
