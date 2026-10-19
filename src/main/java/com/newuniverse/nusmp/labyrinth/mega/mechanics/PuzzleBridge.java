package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Why: the puzzle rooms ({@code labyrinth/mega/puzzle}) are a sibling of the mechanics library, not a copy of it: they must obey the same safety rule (never in a safe room, the Hub or a rift
 * pocket, never on a creative or spectating player), pay out through the same reward path (the dungeon's trial loot table, never a relic; experience that grows with depth), use the same
 * damage cap (a penalty never takes more than 70 percent of a player's health) and scale monsters the same way. Those helpers are package-private in the mechanics engine, so this
 * one small class re-exports exactly the ones the puzzles need, and nothing else, without changing a line of the engine. Every method is a one-line delegation.
 */
public final class PuzzleBridge {
    private PuzzleBridge() {}

    /** May a mechanism exist at this position (not a safe room, the Hub or a rift pocket)? */
    public static boolean allowedAt(Level level, BlockPos pos) { return Safety.allowedAt(level, pos); }

    /** May this player be affected by a mechanism where they stand (survival or adventure, alive, in a playable place)? */
    public static boolean affects(ServerPlayer p) { return Safety.affects(p); }

    /** Rolls the dungeon's trial loot table {@code rolls} times (once per call is one chest) and pops the stacks at the position. */
    public static void loot(ServerLevel level, BlockPos at, ServerPlayer luck, long seed, int rolls) { Rewards.loot(level, Rewards.TABLE, rolls, at, luck, seed); }

    /** Drops experience orbs worth {@code amount} at the position. */
    public static void xp(ServerLevel level, BlockPos at, int amount) { Rewards.xp(level, at, amount); }

    /** Hurts a creature, capped at 70 percent of its maximum health. */
    public static void hurt(ServerLevel level, LivingEntity target, double amount, String kind) { PrimUtil.hurt(level, target, amount, kind); }

    /** Adds a mob effect by id; false when the id is unknown. */
    public static boolean effect(LivingEntity e, String id, int ticks, int amp) { return PrimUtil.addEffect(e, id, ticks, amp); }

    /** Teleports a creature (a player through the connection, so the client agrees). */
    public static void teleport(LivingEntity e, double x, double y, double z) { PrimUtil.teleport(e, x, y, z); }

    /** The default state of a block id (air when unknown). */
    public static BlockState block(String id) { return PrimUtil.block(id); }

    /** Does a living creature stand in this cell? */
    public static boolean occupied(ServerLevel level, BlockPos pos) { return PrimUtil.occupied(level, pos); }

    /** Scales a monster's health and damage by permanent modifiers, the way the mechanics scale theirs. */
    public static void scaleMob(Mob mob, double health, double damage) { Mobs.scale(mob, health, damage); }

    /** The depth coordinate used by every mechanic number (0 at floor 1, just under 5 at floor 1000). */
    public static double depth(int floor) { return Scaling.s(floor); }

    /** The tier 1 to 5 of a signed floor. */
    public static int tier(int floor) { return Scaling.tierOf(floor); }
}
