package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.enc.SpawnLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: an ambush is only an ambush if it is AHEAD. A squad that appears behind a running player is a chase, one beside them is a spawn-in, and one in the next room
 * but sideways is invisible; the squad should be in the corridor the player is running into, far enough that it does not appear in their face and near enough that
 * they reach it before it is forgotten. So the cells are chosen from the saved floor plan: every room that is not a safe room and not the room the player stands in,
 * its spawn markers and its middle (and, for a long corridor, a cell every six blocks along its length), kept when the direction to it is within the cone of the
 * player's heading and its distance is in the band, and ranked by how near the middle of the band it is, with a corridor or junction preferred (a squad in a
 * corridor cannot be walked round). Reads only the floor plan numbers ({@link SpawnLayout}), so it is a unit test. Pure.
 */
public final class AmbushGeometry {
    private AmbushGeometry() {}

    /** A chosen cell (the feet row), its room, and its score (higher is better). */
    public record Pick(int x, int y, int z, int roomId, double score) {}

    /** Cells along a long room, in blocks apart. */
    public static final int STEP = 6;

    /**
     * The cells of up to {@code want} places ahead of a player at ({@code px}, {@code pz}) heading ({@code hx}, {@code hz}) (any length; a zero heading means "any way"),
     * in the distance band {@code minDist} to {@code maxDist}, with cos(angle) at least {@code minCos} between the heading and the way to the cell. The room
     * {@code ownRoom} (the one the player is in) and every safe room are left out, and two picks are at least {@code separation} blocks apart.
     */
    public static List<Pick> ahead(SpawnLayout layout, double px, double pz, double hx, double hz, int ownRoom, double minDist, double maxDist, double minCos, int separation, int want) {
        double hl = Math.hypot(hx, hz);
        boolean anyWay = hl < 1.0e-6;
        double ux = anyWay ? 0.0 : hx / hl, uz = anyWay ? 0.0 : hz / hl;
        double mid = 0.5 * (minDist + maxDist), half = Math.max(1.0, 0.5 * (maxDist - minDist));
        List<Pick> all = new ArrayList<>();
        for (SpawnLayout.RoomBox r : layout.rooms()) {
            if (r.id() == ownRoom || r.safe()) continue;
            boolean line = r.kind().isCirculation();
            List<int[]> cells = new ArrayList<>();
            int cx = (r.minX() + r.maxX()) / 2, cz = (r.minZ() + r.maxZ()) / 2, y = r.standY();
            cells.add(new int[] {cx, y, cz});
            for (SpawnLayout.Spot s : layout.spots()) if (r.contains(s.x(), s.y(), s.z())) cells.add(new int[] {s.x(), s.y(), s.z()});
            if (line) {
                boolean alongX = r.spanX() >= r.spanZ();
                int span = alongX ? r.spanX() : r.spanZ();
                for (int o = STEP; o < span / 2; o += STEP) {
                    if (alongX) {
                        cells.add(new int[] {cx + o, y, cz});
                        cells.add(new int[] {cx - o, y, cz});
                    } else {
                        cells.add(new int[] {cx, y, cz + o});
                        cells.add(new int[] {cx, y, cz - o});
                    }
                }
            }
            for (int[] c : cells) {
                double dx = c[0] + 0.5 - px, dz = c[2] + 0.5 - pz, d = Math.hypot(dx, dz);
                if (d < minDist || d > maxDist) continue;
                double cos = anyWay ? 1.0 : (dx * ux + dz * uz) / d;
                if (cos < minCos) continue;
                double score = (1.0 - Math.abs(d - mid) / (half + 8.0)) + 0.6 * cos + (line ? 0.5 : 0.0);
                all.add(new Pick(c[0], c[1], c[2], r.id(), score));
            }
        }
        all.sort(Comparator.comparingDouble(Pick::score).reversed().thenComparingInt(Pick::x).thenComparingInt(Pick::z));
        List<Pick> out = new ArrayList<>();
        for (Pick p : all) {
            if (out.size() >= want) break;
            boolean apart = true;
            for (Pick q : out) {
                if (Math.hypot(p.x() - q.x(), p.z() - q.z()) < separation) {
                    apart = false;
                    break;
                }
            }
            if (apart) out.add(p);
        }
        return out;
    }
}
