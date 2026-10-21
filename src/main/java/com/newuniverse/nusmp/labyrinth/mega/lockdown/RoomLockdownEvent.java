package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.List;

/**
 * Why: "a challenge room was entered" is the one moment other parts of the mod will want to hook (a quest that counts boss rooms, a relic that softens the first
 * wave, a mutator that adds a wave, an admin who wants to keep a room open for a test), and the lockdown must not know about any of them. So the director posts this
 * NeoForge event on the game bus BEFORE it spawns anything or places a block, with what it is about to do (the room, the party, the band and the number of waves,
 * both mutable) and the source: {@link Source#DIRECTOR} when the room is about to lock, {@link Source#TRIAL} when a trial chamber's own keystone has just sealed
 * (informational: a trial hall is a template of its own and cancelling the event does not stop it). Cancelling a DIRECTOR event keeps the room open this time; the
 * director does not ask again for the same room until its cooldown has run out. The listeners run on the server thread, inside the director's tick, so they must be quick.
 */
public class RoomLockdownEvent extends Event implements ICancellableEvent {
    /** Who posted it. */
    public enum Source { DIRECTOR, TRIAL }

    private final ServerLevel level;
    private final Sector sector;
    private final Room room;
    private final List<ServerPlayer> party;
    private final Source source;
    private Band band;
    private int waves;

    public RoomLockdownEvent(ServerLevel level, Sector sector, Room room, List<ServerPlayer> party, Band band, int waves, Source source) {
        this.level = level;
        this.sector = sector;
        this.room = room;
        this.party = List.copyOf(party);
        this.band = band;
        this.waves = waves;
        this.source = source;
    }

    public ServerLevel getLevel() { return level; }

    /** The sector the room is in (null for a trial chamber outside every sector). */
    public Sector getSector() { return sector; }

    /** The room that was entered (null for a trial chamber whose keystone is in no room box). */
    public Room getRoom() { return room; }

    /** The survival and adventure players inside the room now. */
    public List<ServerPlayer> getParty() { return party; }

    public Source getSource() { return source; }

    /** The size of trouble of the wave about to spawn; a listener may change it (NOTHING is ignored). */
    public Band getBand() { return band; }

    public void setBand(Band band) { if (band != null && band != Band.NOTHING) this.band = band; }

    /** How many waves must be beaten one after the other (1 to 3). */
    public int getWaves() { return waves; }

    public void setWaves(int waves) { this.waves = Math.max(1, Math.min(LockdownRule.MAX_WAVES, waves)); }
}
