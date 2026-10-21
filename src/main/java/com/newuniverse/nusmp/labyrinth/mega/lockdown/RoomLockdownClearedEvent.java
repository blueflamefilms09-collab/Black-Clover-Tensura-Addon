package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

import java.util.List;

/**
 * Why: the other half of {@link RoomLockdownEvent}: posted once when the last wave of a locked room is dead and the barriers come down, so a quest, a reward or a
 * statistic can follow the moment without polling the dungeon. Not cancellable (the room IS cleared). {@code sectorId} and {@code roomId} are the ids of the saved
 * floor plan, {@code survivors} the players who were inside when it opened, {@code ticksSealed} how long it was closed.
 */
public class RoomLockdownClearedEvent extends Event {
    private final ServerLevel level;
    private final int sectorId, roomId, waves;
    private final long ticksSealed;
    private final List<ServerPlayer> survivors;

    public RoomLockdownClearedEvent(ServerLevel level, int sectorId, int roomId, int waves, long ticksSealed, List<ServerPlayer> survivors) {
        this.level = level;
        this.sectorId = sectorId;
        this.roomId = roomId;
        this.waves = waves;
        this.ticksSealed = ticksSealed;
        this.survivors = List.copyOf(survivors);
    }

    public ServerLevel getLevel() { return level; }

    public int getSectorId() { return sectorId; }

    public int getRoomId() { return roomId; }

    public int getWaves() { return waves; }

    public long getTicksSealed() { return ticksSealed; }

    public List<ServerPlayer> getSurvivors() { return survivors; }
}
