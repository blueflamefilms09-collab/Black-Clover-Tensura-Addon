package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Why: candidate cells are where every encounter begins, and the first version of this step ({@code CellSource}) had three weaknesses that, together, made
 * encounters fail to appear on a floor of the cyclic generator (two thirds of whose rooms are corridors and junctions): it looked for candidates round the
 * CENTRE of the party (a party of three spread over 90 blocks has a centre that is far from all of them), it ignored every corridor and junction that carried
 * no spawn marker, and it did not care whether a cell could be walked to from the party (a hall behind a sealed wall is a place a monster stands for ten minutes).
 * This class keeps the first behaviour exactly ({@link Mode#LEGACY}: markers first, topped up with three floor cells of every ordinary room) and adds the
 * fallback ({@link Mode#DENSE}: every marker, a grid of cells over every room that is not a safe room, corridors included, and the cells just inside each doorway),
 * measures reach from EVERY member of the party, and weights a cell by how many doorways it is from the party's rooms. Whether a cell can really hold a monster is
 * decided later, in the world, when the chunk is known to be loaded. Pure.
 */
public final class CellFinder {
    private CellFinder() {}

    /** LEGACY: markers, then three random cells per ordinary room when the markers are few. DENSE: markers, a grid over every non-safe room, the doorway cells. */
    public enum Mode { LEGACY, DENSE }

    /** Below this many marker cells in reach the room floors are added (LEGACY). */
    static final int ENOUGH = 8;
    static final int PER_ROOM = 3;
    /** The most cells DENSE returns (the work of one roll stays bounded). */
    static final int DENSE_CAP = 320;
    private static final int GRID = 4;

    /** The weight factor of a cell for the number of doorways between its room and the nearest room of the party; a room that cannot be reached counts little. */
    public static double hopFactor(Integer hops) {
        if (hops == null) return 0.15;
        if (hops <= 2) return 1.0;
        if (hops <= 5) return 0.7;
        return 0.45;
    }

    /**
     * The candidate cells of the layout within {@code reach} blocks (horizontally) of ANY of the eyes. {@code hops} (nullable) is {@link SpawnLayout#hopsFrom} of the
     * party's rooms and scales every weight by {@link #hopFactor}. The cells of a safe room are returned with {@code safe = true}; the picker drops them.
     */
    public static List<SpawnPicker.Cell> candidates(SpawnLayout layout, List<SpawnPicker.Eye> eyes, double reach, Mode mode, Map<Integer, Integer> hops, Random r) {
        List<SpawnPicker.Cell> out = new ArrayList<>();
        if (eyes.isEmpty()) return out;
        for (SpawnLayout.Spot s : layout.spots()) {
            if (!near(eyes, s.x() + 0.5, s.z() + 0.5, reach)) continue;
            SpawnLayout.RoomBox room = layout.roomAt(s.x(), s.y(), s.z());
            out.add(new SpawnPicker.Cell(s.x(), s.y(), s.z(), scaled(Math.max(0.1, s.weight()), room, hops), room == null ? -1 : room.id(), room != null && room.safe()));
        }
        if (mode == Mode.LEGACY) {
            if (out.size() >= ENOUGH) return out;
            for (SpawnLayout.RoomBox room : layout.rooms()) {
                if (room.safe() || room.kind().isCirculation()) continue;
                int minX = room.minX() + 3, maxX = room.maxX() - 3, minZ = room.minZ() + 3, maxZ = room.maxZ() - 3;
                if (maxX < minX || maxZ < minZ) continue;
                if (!roomInReach(eyes, room, reach)) continue;
                for (int i = 0; i < PER_ROOM; i++) {
                    int x = minX + r.nextInt(maxX - minX + 1), z = minZ + r.nextInt(maxZ - minZ + 1);
                    out.add(new SpawnPicker.Cell(x, room.standY(), z, scaled(0.5, room, hops), room.id(), false));
                }
            }
            return out;
        }
        List<SpawnPicker.Cell> dense = new ArrayList<>();
        for (SpawnLayout.RoomBox room : layout.rooms()) {
            if (room.safe() || !roomInReach(eyes, room, reach)) continue;
            boolean circulation = room.kind().isCirculation();
            int inset = circulation ? 2 : 3;
            int minX = room.minX() + inset, maxX = room.maxX() - inset, minZ = room.minZ() + inset, maxZ = room.maxZ() - inset;
            if (maxX < minX || maxZ < minZ) continue;
            double w = circulation ? 0.3 : 0.5;
            int offX = r.nextInt(GRID), offZ = r.nextInt(GRID);
            for (int x = minX + offX % Math.max(1, maxX - minX + 1); x <= maxX; x += GRID) {
                for (int z = minZ + offZ % Math.max(1, maxZ - minZ + 1); z <= maxZ; z += GRID) {
                    if (near(eyes, x + 0.5, z + 0.5, reach)) dense.add(new SpawnPicker.Cell(x, room.standY(), z, scaled(w, room, hops), room.id(), false));
                }
            }
        }
        for (int[] e : layout.edges()) {
            SpawnLayout.RoomBox a = layout.room(e[0]), b = layout.room(e[1]);
            if (a == null || b == null) continue;
            addDoorCells(dense, a, b, eyes, reach, hops);
            addDoorCells(dense, b, a, eyes, reach, hops);
        }
        if (dense.size() > DENSE_CAP) {
            Collections.shuffle(dense, r);
            dense = new ArrayList<>(dense.subList(0, DENSE_CAP));
        }
        out.addAll(dense);
        return out;
    }

    /** The cell just inside room {@code a} where it touches room {@code b} (a doorway stands in the middle of the contact), when they touch along a wall. */
    private static void addDoorCells(List<SpawnPicker.Cell> out, SpawnLayout.RoomBox a, SpawnLayout.RoomBox b, List<SpawnPicker.Eye> eyes, double reach, Map<Integer, Integer> hops) {
        if (a.safe()) return;
        int ox = Math.min(a.maxX(), b.maxX()) - Math.max(a.minX(), b.minX()) + 1;
        int oz = Math.min(a.maxZ(), b.maxZ()) - Math.max(a.minZ(), b.minZ()) + 1;
        int x, z;
        if (ox <= 0 && ox >= -1 && oz >= 3) {                                    // they touch along x: the door is on the plane between them
            z = (Math.max(a.minZ(), b.minZ()) + Math.min(a.maxZ(), b.maxZ())) / 2;
            x = b.minX() > a.maxX() ? a.maxX() - 3 : a.minX() + 3;
        } else if (oz <= 0 && oz >= -1 && ox >= 3) {
            x = (Math.max(a.minX(), b.minX()) + Math.min(a.maxX(), b.maxX())) / 2;
            z = b.minZ() > a.maxZ() ? a.maxZ() - 3 : a.minZ() + 3;
        } else {
            return;
        }
        if (x < a.minX() || x > a.maxX() || z < a.minZ() || z > a.maxZ()) return;
        if (near(eyes, x + 0.5, z + 0.5, reach)) out.add(new SpawnPicker.Cell(x, a.standY(), z, scaled(0.4, a, hops), a.id(), false));
    }

    private static double scaled(double weight, SpawnLayout.RoomBox room, Map<Integer, Integer> hops) {
        if (hops == null || room == null) return weight;
        return weight * hopFactor(hops.get(room.id()));
    }

    private static boolean near(List<SpawnPicker.Eye> eyes, double x, double z, double reach) {
        double r2 = reach * reach;
        for (SpawnPicker.Eye e : eyes) {
            double dx = x - e.x(), dz = z - e.z();
            if (dx * dx + dz * dz <= r2) return true;
        }
        return false;
    }

    private static boolean roomInReach(List<SpawnPicker.Eye> eyes, SpawnLayout.RoomBox room, double reach) {
        double cx = (room.minX() + room.maxX()) * 0.5, cz = (room.minZ() + room.maxZ()) * 0.5;
        double half = Math.hypot(room.spanX(), room.spanZ()) * 0.5;
        for (SpawnPicker.Eye e : eyes) if (Math.hypot(cx - e.x(), cz - e.z()) - half <= reach) return true;
        return false;
    }
}
