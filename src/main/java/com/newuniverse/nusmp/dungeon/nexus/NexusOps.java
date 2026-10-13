package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.nexus.NexusOp.Kind;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the placement ops of a Nexus plan, in the manner of the Golden City's ops (dungeon/city/CityOps) but with blocks named by id,
 * so a zone is free to use any block, the mod's own included. Callers work in a local frame: x and z are offsets from the centre point
 * (ox, oz), and the frame can be mirrored and swapped so one description of a tower or a gate serves every side it stands on; y is always
 * absolute. A "facing" or "axis" in the properties is turned with the frame (stair corner shapes are not mirrored: use straight stairs).
 * Ops come out in world coordinates, in the order they were added; {@link #phase()} marks where the next phase of the build starts.
 */
public final class NexusOps {
    private final List<NexusOp> list = new ArrayList<>();
    private final List<Integer> phases = new ArrayList<>();
    private final int ox, oz;
    private int cu, cv, su = 1, sv = 1;
    private boolean swap;

    /** ox, oz: the centre point in world coordinates. */
    public NexusOps(int ox, int oz) {
        this.ox = ox;
        this.oz = oz;
    }

    public List<NexusOp> list() { return list; }

    public int size() { return list.size(); }

    /** The op indices at which phases 1, 2, ... begin (phase 0 begins at 0). */
    public List<Integer> phaseStarts() { return phases; }

    /** Starts the next phase of the build. On resume, only ops of the same phase are replayed (carve, then floor, then walls, then details). */
    public void phase() { phases.add(list.size()); }

    // ---------------------------------------------------------------- the frame
    /** The frame for what follows: the centre of the local frame as an offset from the origin, whether local x and z are swapped, and the mirror signs of x and z. */
    public void frame(int cu, int cv, boolean swap, int su, int sv) {
        this.cu = cu;
        this.cv = cv;
        this.swap = swap;
        this.su = su;
        this.sv = sv;
    }

    public void resetFrame() { frame(0, 0, false, 1, 1); }

    private int wx(int x, int z) {
        int xx = x * su, zz = z * sv;
        return ox + cu + (swap ? zz : xx);
    }

    private int wz(int x, int z) {
        int xx = x * su, zz = z * sv;
        return oz + cv + (swap ? xx : zz);
    }

    /** A facing (0 north, 1 east, 2 south, 3 west) seen from the local frame, as it points in the world. */
    public int face(int f) {
        if (su < 0 && (f == 1 || f == 3)) f = 4 - f;
        if (sv < 0 && (f == 0 || f == 2)) f = 2 - f;
        return swap ? 3 - f : f;
    }

    private String turned(String props) {
        if (props.isEmpty() || (!swap && su > 0 && sv > 0)) return props;
        return BlockProps.remap(props, new int[]{face(0), face(1), face(2), face(3)}, swap);
    }

    // ---------------------------------------------------------------- blocks
    public void box(String block, String props, int x1, int y1, int z1, int x2, int y2, int z2) { add(Kind.BOX, block, props, x1, y1, z1, x2, y2, z2); }

    public void box(String block, int x1, int y1, int z1, int x2, int y2, int z2) { add(Kind.BOX, block, "", x1, y1, z1, x2, y2, z2); }

    public void put(String block, String props, int x, int y, int z) { add(Kind.BOX, block, props, x, y, z, x, y, z); }

    public void put(String block, int x, int y, int z) { add(Kind.BOX, block, "", x, y, z, x, y, z); }

    public void air(int x1, int y1, int z1, int x2, int y2, int z2) { add(Kind.BOX, NexusOp.AIR, "", x1, y1, z1, x2, y2, z2); }

    /** Fills only the cells of the box that are air when the op runs. */
    public void fillAir(String block, String props, int x1, int y1, int z1, int x2, int y2, int z2) { add(Kind.FILL_AIR, block, props, x1, y1, z1, x2, y2, z2); }

    private void add(Kind kind, String block, String props, int x1, int y1, int z1, int x2, int y2, int z2) {
        int a = wx(x1, z1), b = wx(x2, z2), c = wz(x1, z1), d = wz(x2, z2);
        int minX = Math.min(a, b), maxX = Math.max(a, b), minZ = Math.min(c, d), maxZ = Math.max(c, d);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        boolean one = minX == maxX && minY == maxY && minZ == maxZ;
        list.add(new NexusOp(one && kind == Kind.BOX ? Kind.BLOCK : kind, block, turned(props), minX, minY, minZ, maxX, maxY, maxZ));
    }

    /** The four walls of a rectangle (no inside), from y1 to y2. */
    public void ring(String block, String props, int x1, int z1, int x2, int z2, int y1, int y2) {
        box(block, props, x1, y1, z1, x2, y2, z1);
        box(block, props, x1, y1, z2, x2, y2, z2);
        if (z2 - z1 > 1) {
            box(block, props, x1, y1, z1 + 1, x1, y2, z2 - 1);
            box(block, props, x2, y1, z1 + 1, x2, y2, z2 - 1);
        }
    }

    /** A filled ellipse of half-widths a (x) and b (z) round the frame's origin, from y1 to y2. */
    public void disc(String block, String props, int a, int b, int y1, int y2) {
        for (int v = -b; v <= b; v++) {
            int e = Footprint.extent(v, a, b);
            if (e >= 0) box(block, props, -e, y1, v, e, y2, v);
        }
    }

    /** The wall of an ellipse: the cells inside the ellipse (a, b) and outside the one that is `wall` blocks smaller in each direction. */
    public void discHollow(String block, String props, int a, int b, int wall, int y1, int y2) {
        int ia = a - wall, ib = b - wall;
        for (int v = -b; v <= b; v++) {
            int e = Footprint.extent(v, a, b);
            if (e < 0) continue;
            int in = ia >= 0 && ib >= 0 && Math.abs(v) <= ib ? Footprint.extent(v, ia, ib) : -1;
            if (in < 0) {
                box(block, props, -e, y1, v, e, y2, v);
            } else if (e > in) {
                box(block, props, -e, y1, v, -in - 1, y2, v);
                box(block, props, in + 1, y1, v, e, y2, v);
            }
        }
    }

    /** Clears everything in a box (bedrock stays). Always in offsets from the origin: the frame does not apply. */
    public void carve(int u1, int y1, int v1, int u2, int y2, int v2) {
        list.add(new NexusOp(Kind.CARVE, NexusOp.AIR, "", ox + Math.min(u1, u2), Math.min(y1, y2), oz + Math.min(v1, v2), ox + Math.max(u1, u2), Math.max(y1, y2), oz + Math.max(v1, v2)));
    }

    /** Clears the cells of an ellipse of half-widths a and b round the origin, from y1 to y2 (the frame does not apply). */
    public void carveDisc(int a, int b, int y1, int y2) {
        for (int v = -b; v <= b; v++) {
            int e = Footprint.extent(v, a, b);
            if (e >= 0) carve(-e, y1, v, e, y2, v);
        }
    }
}
