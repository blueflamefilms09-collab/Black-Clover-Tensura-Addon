package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import com.newuniverse.nusmp.labyrinth.mega.enc.EncounterDirector;
import com.newuniverse.nusmp.labyrinth.mega.enc.SpawnLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: the wave of a locked room is the Dungeon Master's own encounter, not a second spawner. This is the seam: it turns the room (a box of the saved floor plan and the
 * template's spawn markers) into cells inside it ({@link WaveCells}), hands them to {@link EncounterDirector#spawnAt}, which picks the entry of the sector's table for the
 * wave's band, sizes it for the party and the depth, forms the squad (5 to 8 with roles), checks every cell against the world and stamps the monsters, and then marks the
 * monsters it got as the room's: the {@link LockdownRegistry#MOB_TAG} tag and persistence, so a wave never despawns because the party backed away from it. The first cells
 * are the group the encounter spreads over, the rest the reserve the spawner falls back on. Server thread only.
 */
final class LockdownWaves {
    private LockdownWaves() {}

    /** The monsters of a wave, and a sentence for the log and the status command. */
    record Result(List<Mob> mobs, String text) {}

    /** No monster of a wave is placed nearer than this to a player. */
    static final double MIN_DIST = 4.0;
    private static final int GROUP = 8, SPOTS = 24;

    static Result spawn(ServerLevel level, int sectorId, int roomId, List<ServerPlayer> party, Band band, Random rnd) {
        Sector sector = MegaData.get(level.getServer()).sector(sectorId);
        Room room = sector == null ? null : sector.room(roomId);
        if (room == null) return new Result(List.of(), "the room is not in the floor plan");
        List<int[]> spots = new ArrayList<>();
        for (Marker m : sector.markers) {
            if (m.kind() == MarkerKind.SPAWN && room.contains(m.pos())) spots.add(new int[] {m.pos().getX(), m.pos().getY(), m.pos().getZ()});
        }
        List<double[]> players = new ArrayList<>();
        for (ServerPlayer p : party) players.add(new double[] {p.getX(), p.getY(), p.getZ()});
        SpawnLayout.RoomBox rb = new SpawnLayout.RoomBox(room.id, room.kind, room.box.minX(), room.box.minY(), room.box.minZ(), room.box.maxX(), room.box.maxY(), room.box.maxZ());
        List<WaveCells.Cell> cells = WaveCells.pick(room.box.minX(), room.box.minZ(), room.box.maxX(), room.box.maxZ(), rb.standY(), spots, players, MIN_DIST, rnd);
        if (cells.isEmpty()) return new Result(List.of(), "the room has no cell " + (int) MIN_DIST + " blocks from the party");
        List<BlockPos> group = new ArrayList<>(), reserve = new ArrayList<>();
        for (int i = 0; i < cells.size() && i < SPOTS; i++) {
            WaveCells.Cell c = cells.get(i);
            (i < GROUP ? group : reserve).add(new BlockPos(c.x(), c.y(), c.z()));
        }
        EncounterDirector.Outcome o = EncounterDirector.spawnAt(level, party, group, reserve, MIN_DIST, band, false, rnd);
        if (!o.spawned()) return new Result(List.of(), o.kind() + ": " + o.text());
        List<Mob> mobs = new ArrayList<>();
        for (Mob m : o.mobs()) {
            if (!m.isAlive()) continue;
            m.addTag(LockdownRegistry.MOB_TAG);
            m.setPersistenceRequired();
            mobs.add(m);
        }
        return new Result(mobs, o.text());
    }
}
