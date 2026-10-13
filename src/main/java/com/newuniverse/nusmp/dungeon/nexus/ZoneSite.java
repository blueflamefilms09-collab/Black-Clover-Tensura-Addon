package com.newuniverse.nusmp.dungeon.nexus;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * One face of the Variable Core. {@link NexusBuilder} asks the site for a pure {@link ZonePlan}, applies it, then calls
 * {@link #populate}; {@link NexusZones#site} maps each {@link NexusZone} to its site (a final class with a public static final INSTANCE
 * in dungeon/nexus/zone). The centre handed to every method is the floor block in the middle of the Nexus.
 */
public interface ZoneSite {
    NexusZone zone();

    /** The plan of the Nexus in the core cell (cellX, cellZ): pure and deterministic, the same cell always gives the same plan. */
    ZonePlan plan(int cellX, int cellZ);

    /** Called once, right after the Nexus is built (never again unless an admin resets it): relic pedestals, gates, creatures, the boss. All chunks of the footprint are loaded. */
    void populate(ServerLevel level, int cellX, int cellZ, BlockPos centre);

    /** Extra staged work after the plan's own ops, in order (see {@link ZoneStage}). The list must be the same on every call for the same plan: a restart resumes by stage index. */
    default List<ZoneStage> stages(ZonePlan plan) { return List.of(); }

    /** About once a second while players (spectators excluded) are inside the Nexus. */
    default void tickPlayers(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> inside) {}

    /** About every five seconds while players (spectators excluded) are within 96 blocks of the centre, inside or not (the Golden City looks for a way in here). */
    default void tickNear(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> near) {}

    /** May the Dungeon's ecosystem breed in the chunks of this Nexus once it is built? */
    default boolean lifeAllowed() { return true; }
}
