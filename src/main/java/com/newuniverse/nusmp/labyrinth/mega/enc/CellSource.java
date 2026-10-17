package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: the Dungeon Master needs places to put monsters, and the sector already knows them. The level designers (the template builders) mark good
 * ambush spots with {@code nusmp:spawn} markers (a doorway's shadow, the back of a crypt, behind a pillar); those are the first choice, weighted
 * by the marker's {@code weight}. A sector with few markers in reach (a sparse template, a cavern) is topped up with random floor cells from the
 * rooms that are neither rest rooms, boss rooms nor corridors, so the Director never runs dry. Everything here reads only the sector's SAVED data
 * (markers and room boxes), so it touches no chunk and loads nothing; whether a cell can actually hold a monster (floor, air above) is decided
 * later, at the moment of spawning, when the chunk is known to be loaded. A cell in a Sanctuary room is marked safe so the picker drops it.
 */
final class CellSource {
    private CellSource() {}

    /** Below this many marker cells in reach the room floors are added. */
    private static final int ENOUGH = 8;
    private static final int PER_ROOM = 3;

    /** Candidate cells of the sector within {@code reach} blocks (horizontally) of the point. */
    static List<SpawnPicker.Cell> candidates(Sector sector, double cx, double cz, double reach, Random r) {
        List<SpawnPicker.Cell> out = new ArrayList<>();
        double r2 = reach * reach;
        for (Marker m : sector.markers) {
            if (m.kind() != MarkerKind.SPAWN) continue;
            double dx = m.pos().getX() + 0.5 - cx, dz = m.pos().getZ() + 0.5 - cz;
            if (dx * dx + dz * dz > r2) continue;
            Room room = sector.roomAt(m.pos());
            boolean safe = room != null && Sanctuary.is(room.kind);
            out.add(new SpawnPicker.Cell(m.pos().getX(), m.pos().getY(), m.pos().getZ(), Math.max(0.1, m.intArg("weight", 1)), room == null ? -1 : room.id, safe));
        }
        if (out.size() >= ENOUGH) return out;
        for (Room room : sector.rooms) {
            if (Sanctuary.is(room.kind) || room.kind.isCirculation()) continue;
            int minX = room.box.minX() + 3, maxX = room.box.maxX() - 3, minZ = room.box.minZ() + 3, maxZ = room.box.maxZ() - 3;
            if (maxX < minX || maxZ < minZ) continue;
            double mx = (room.box.minX() + room.box.maxX()) * 0.5 - cx, mz = (room.box.minZ() + room.box.maxZ()) * 0.5 - cz;
            double halfDiagonal = Math.hypot(room.box.getXSpan(), room.box.getZSpan()) * 0.5;
            if (Math.hypot(mx, mz) - halfDiagonal > reach) continue;
            for (int i = 0; i < PER_ROOM; i++) {
                int x = minX + r.nextInt(maxX - minX + 1), z = minZ + r.nextInt(maxZ - minZ + 1);
                out.add(new SpawnPicker.Cell(x, MegaGrid.FLOOR_Y + 1, z, 0.5, room.id, false));
            }
        }
        return out;
    }
}
