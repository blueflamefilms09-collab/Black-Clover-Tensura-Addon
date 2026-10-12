package com.newuniverse.nusmp.dungeon.city;

import com.newuniverse.nusmp.dungeon.city.CityPlan.Kind;
import com.newuniverse.nusmp.dungeon.city.CityPlan.Op;
import com.newuniverse.nusmp.dungeon.city.CityPlan.Pal;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the placement ops of a city. Callers work in a local frame: x and z are offsets from a centre point (a building's middle), which
 * can be mirrored and swapped so one description of a tower or a gate serves every side it stands on. The ops come out in world coordinates.
 */
final class CityOps {
    private final List<Op> list = new ArrayList<>();
    private final int ox, oz;
    private int cu, cv, su = 1, sv = 1;
    private boolean swap;

    /** ox, oz: the district centre in world coordinates. */
    CityOps(int ox, int oz) {
        this.ox = ox;
        this.oz = oz;
    }

    List<Op> list() { return list; }

    int size() { return list.size(); }

    /** The frame for what follows: the centre in district coordinates, whether local x and z are swapped, and the mirror signs of x and z. */
    void frame(int cu, int cv, boolean swap, int su, int sv) {
        this.cu = cu;
        this.cv = cv;
        this.swap = swap;
        this.su = su;
        this.sv = sv;
    }

    void resetFrame() { frame(0, 0, false, 1, 1); }

    private int wx(int x, int z) {
        int xx = x * su, zz = z * sv;
        return ox + cu + (swap ? zz : xx);
    }

    private int wz(int x, int z) {
        int xx = x * su, zz = z * sv;
        return oz + cv + (swap ? xx : zz);
    }

    /** A facing (0 north, 1 east, 2 south, 3 west) seen from the local frame, as it points in the world. */
    int face(int f) {
        if (su < 0 && (f == CityPlan.EAST || f == CityPlan.WEST)) f = 4 - f;
        if (sv < 0 && (f == CityPlan.NORTH || f == CityPlan.SOUTH)) f = 2 - f;
        return swap ? 3 - f : f;
    }

    private int variantOf(Pal p, int variant) { return p == Pal.STAIRS ? (variant & ~3) | face(variant & 3) : variant; }

    void box(Pal p, int variant, int x1, int y1, int z1, int x2, int y2, int z2) {
        int a = wx(x1, z1), b = wx(x2, z2), c = wz(x1, z1), d = wz(x2, z2);
        int minX = Math.min(a, b), maxX = Math.max(a, b), minZ = Math.min(c, d), maxZ = Math.max(c, d);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        boolean one = minX == maxX && minY == maxY && minZ == maxZ;
        list.add(new Op(one ? Kind.BLOCK : Kind.BOX, p, variantOf(p, variant), minX, minY, minZ, maxX, maxY, maxZ));
    }

    void box(Pal p, int x1, int y1, int z1, int x2, int y2, int z2) { box(p, 0, x1, y1, z1, x2, y2, z2); }

    void put(Pal p, int variant, int x, int y, int z) { box(p, variant, x, y, z, x, y, z); }

    void put(Pal p, int x, int y, int z) { box(p, 0, x, y, z, x, y, z); }

    void air(int x1, int y1, int z1, int x2, int y2, int z2) { box(Pal.AIR, 0, x1, y1, z1, x2, y2, z2); }

    /** The four walls of a rectangle (no inside), from y1 to y2. */
    void ring(Pal p, int variant, int x1, int z1, int x2, int z2, int y1, int y2) {
        box(p, variant, x1, y1, z1, x2, y2, z1);
        box(p, variant, x1, y1, z2, x2, y2, z2);
        if (z2 - z1 > 1) {
            box(p, variant, x1, y1, z1 + 1, x1, y2, z2 - 1);
            box(p, variant, x2, y1, z1 + 1, x2, y2, z2 - 1);
        }
    }

    /** Clears everything in a box of the district (used for the carve; always in district coordinates). */
    void carve(int u1, int y1, int v1, int u2, int y2, int v2) {
        list.add(new Op(Kind.CARVE, Pal.AIR, 0, ox + Math.min(u1, u2), Math.min(y1, y2), oz + Math.min(v1, v2), ox + Math.max(u1, u2), Math.max(y1, y2), oz + Math.max(v1, v2)));
    }
}
