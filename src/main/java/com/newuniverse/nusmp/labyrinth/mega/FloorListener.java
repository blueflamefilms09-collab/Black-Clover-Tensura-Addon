package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.server.level.ServerPlayer;

/**
 * Why: several systems care when a player crosses from one floor to the next (the Directional Lock counts a cleared first floor, the System overlay starts and
 * ends the floor quest, the Rukh flow changes the air the player breathes, the achievements count floors), and only one module (the floor tracker) can know
 * it, because it is the one that watches rooms. Like {@link SectorListener} this is a tiny callback interface with default empty methods: a listener registers
 * once from its registry's {@code init} through {@link MegaDungeon#addFloorListener} and the tracker calls {@link MegaDungeon#fireFloorEntered} /
 * {@link MegaDungeon#fireFloorCleared}. Floor numbers are signed (Holy positive, Abyssal negative, see {@link FloorMap}); a listener must never throw (the
 * facade catches and logs, but a thrown exception skips the listener's own work).
 */
public interface FloorListener {
    /** The player stepped into a room of {@code floor}; {@code previous} is the floor they were on (0 when they came from outside the dungeon or the Hub). */
    default void onFloorEntered(ServerPlayer player, int floor, int previous) {}

    /** The player left {@code floor} for a deeper one of the same polarity (they "cleared" it by moving on). Fired once per player and floor. */
    default void onFloorCleared(ServerPlayer player, int floor) {}
}
