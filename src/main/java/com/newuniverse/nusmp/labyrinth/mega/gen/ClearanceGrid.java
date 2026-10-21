package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: whether the cuts of {@link Clearance7} really leave a 7 x 7 passage can be answered without a game for one case that matters most: the template files that
 * ship. This is a template as a grid of block ids (in the template's own frame, which is where a piece with no turn and a zero origin puts it), with the same
 * decisions the game side makes ({@link Clearance7#carvable}, {@link Clearance7#needsCap}) applied to it, so the unit test and the in-game self-test can cut every
 * real template, measure it with the rule and count what the cuts had to leave standing. It is a {@link Clearance7.Space}. Pure; one small array per template.
 */
public final class ClearanceGrid implements Clearance7.Space {
    public final int sx, sy, sz;
    private final short[] cell;
    private final boolean[] entity;
    private final boolean[] bottomStair;
    private final List<String> names = new ArrayList<>();
    private final Map<String, Short> index = new HashMap<>();

    /** What a cut did: blocks removed, blocks it had to leave (a chest, a gate), roof blocks added, light blocks added. */
    public record Stats(int cleared, int kept, int capped, int lit) {
        Stats plus(Stats o) { return new Stats(cleared + o.cleared, kept + o.kept, capped + o.capped, lit + o.lit); }
    }

    public ClearanceGrid(int sx, int sy, int sz) {
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.cell = new short[sx * sy * sz];
        this.entity = new boolean[sx * sy * sz];
        this.bottomStair = new boolean[sx * sy * sz];
        names.add("minecraft:air");
        index.put("minecraft:air", (short) 0);
    }

    private int at(int x, int y, int z) { return (y * sz + z) * sx + x; }

    private boolean in(int x, int y, int z) { return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz; }

    /** Puts a block (the template's air and void cells are simply never set). */
    public void set(int x, int y, int z, String id, boolean blockEntity, boolean bottomHalfStair) {
        if (!in(x, y, z)) return;
        Short i = index.get(id);
        if (i == null) {
            i = (short) names.size();
            names.add(id);
            index.put(id, i);
        }
        int a = at(x, y, z);
        cell[a] = i;
        entity[a] = blockEntity;
        bottomStair[a] = bottomHalfStair;
    }

    public String id(int x, int y, int z) { return in(x, y, z) ? names.get(cell[at(x, y, z)]) : "minecraft:bedrock"; }

    /** Does the cell hold a block entity (a chest, a furnace, a banner ...)? */
    public boolean blockEntity(int x, int y, int z) { return in(x, y, z) && entity[at(x, y, z)]; }

    /** Is the cell free for a walker? Outside the template counts as blocked (a cut never reaches outside). */
    @Override
    public boolean free(int x, int y, int z) { return in(x, y, z) && Clearance7.passable(names.get(cell[at(x, y, z)])); }

    /** Receives every non-air block of a grid (see {@link #forEach}). */
    @FunctionalInterface
    public interface Visitor {
        void block(int x, int y, int z, String id, boolean blockEntity, boolean bottomStair);
    }

    /** Visits every block that is not air, in no particular order (a test pastes whole templates into one big grid with it). */
    public void forEach(Visitor v) {
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    int a = at(x, y, z);
                    if (cell[a] != 0) v.block(x, y, z, names.get(cell[a]), entity[a], bottomStair[a]);
                }
            }
        }
    }

    /** The cells a player stands on a stair: every bottom-half stair block. */
    public List<Vec> stairStands() {
        List<Vec> out = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) if (bottomStair[at(x, y, z)]) out.add(new Vec(x, y, z));
        return out;
    }

    /** Applies a cut exactly as the game side does: removes what may be removed, adds a roof where the room would open to the void, adds the light blocks. */
    public Stats cut(Clearance7.Cut c) {
        Box b = c.box();
        int cleared = 0, kept = 0, capped = 0, lit = 0;
        boolean stair = c.kind() == Clearance7.Kind.STAIR;
        int rows = b.sizeY();
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                boolean[] was = new boolean[rows];
                for (int y = b.minY(); y <= b.maxY(); y++) {
                    if (!in(x, y, z)) continue;
                    int a = at(x, y, z);
                    String id = names.get(cell[a]);
                    if (Clearance7.isAir(id)) continue;
                    if (stair && bottomStair[a]) continue;                                   // a step of the staircase is never part of its own head room
                    if (!Clearance7.carvable(id, entity[a])) {
                        if (!Clearance7.passable(id)) kept++;
                        continue;
                    }
                    was[y - b.minY()] = !Clearance7.passable(id);
                    cell[a] = 0;
                    entity[a] = false;
                    cleared++;
                }
                if (stair) continue;
                int above = b.maxY() + 1;
                if (in(x, above, z) && Clearance7.needsCap(was, Clearance7.isAir(names.get(cell[at(x, above, z)])))) {
                    set(x, above, z, "minecraft:deepslate_bricks", false, false);
                    capped++;
                }
            }
        }
        for (Vec v : Clearance7.lightCells(c)) {
            if (in(v.x(), v.y(), v.z()) && Clearance7.isAir(names.get(cell[at(v.x(), v.y(), v.z())]))) {
                set(v.x(), v.y(), v.z(), "minecraft:light", false, false);
                lit++;
            }
        }
        return new Stats(cleared, kept, capped, lit);
    }

    /** Cuts all of them and sums what happened. */
    public Stats cutAll(List<Clearance7.Cut> cuts) {
        Stats s = new Stats(0, 0, 0, 0);
        for (Clearance7.Cut c : cuts) s = s.plus(cut(c));
        return s;
    }
}
