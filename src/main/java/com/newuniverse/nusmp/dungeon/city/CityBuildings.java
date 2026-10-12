package com.newuniverse.nusmp.dungeon.city;

import com.newuniverse.nusmp.dungeon.city.CityPlan.Pal;

import java.util.ArrayList;
import java.util.List;

import static com.newuniverse.nusmp.dungeon.city.CityPlan.EAST;
import static com.newuniverse.nusmp.dungeon.city.CityPlan.FLIP;
import static com.newuniverse.nusmp.dungeon.city.CityPlan.NORTH;
import static com.newuniverse.nusmp.dungeon.city.CityPlan.SOUTH;
import static com.newuniverse.nusmp.dungeon.city.CityPlan.WEST;
import static com.newuniverse.nusmp.dungeon.city.CityPlan.candles;

/**
 * The buildings of the Golden City, each described once in a local frame (x and z from the middle of the lot, the door of a tower on its
 * north side) and placed by {@link CityOps}, which mirrors and swaps the frame to suit the lot. A ruined building has chunks cut out of it
 * after it is built, and rubble at its foot.
 */
final class CityBuildings {
    private CityBuildings() {}

    /**
     * A tower, (2m+1) blocks square and h tall to its top platform, with a spiral stair round the inside wall, slit windows, gilded bands,
     * a door on the north side and a crenellated top with lanterns on the corners.
     */
    static void tower(CityOps o, CityRng r, int fy, int m, int h, boolean ruined) {
        o.ring(Pal.BRICKS, 0, -m, -m, m, m, fy + 1, fy + h);
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) o.box(Pal.GILDED, 0, sx * m, fy + 1, sz * m, sx * m, fy + h, sz * m);
        o.ring(Pal.GILDED, 0, -m, -m, m, m, fy + 1, fy + 1);
        for (int y = fy + 8; y < fy + h - 1; y += 6) o.ring(Pal.GILDED, 0, -m, -m, m, m, y, y);
        for (int y = fy + 5; y + 1 < fy + h - 1; y += 6) {
            o.box(Pal.GLASS, 0, 0, y, -m, 0, y + 1, -m);
            o.box(Pal.GLASS, 0, 0, y, m, 0, y + 1, m);
            o.box(Pal.GLASS, 0, -m, y, 0, -m, y + 1, 0);
            o.box(Pal.GLASS, 0, m, y, 0, m, y + 1, 0);
        }
        o.put(Pal.SEA_LANTERN, 0, -m, fy + 7, 2);                                                    // lights in the wall, seen from both sides
        o.put(Pal.SEA_LANTERN, 0, m, fy + 7, -2);
        if (h > 14) {
            o.put(Pal.SEA_LANTERN, 0, -m, fy + 13, -2);
            o.put(Pal.SEA_LANTERN, 0, m, fy + 13, 2);
        }
        o.air(-1, fy + 1, -m, 1, fy + 3, -m);                                                        // the door
        o.box(Pal.GOLD, 0, -1, fy + 4, -m, 1, fy + 4, -m);
        for (int s = -1; s <= 1; s += 2) {
            o.box(Pal.GILDED, 0, 2 * s, fy + 1, -m - 1, 2 * s, fy + 2, -m - 1);
            o.put(Pal.LANTERN, 0, 2 * s, fy + 3, -m - 1);
        }
        // the top platform, and a spiral stair round the inside of the wall that climbs to it
        int k = m - 1;
        o.box(Pal.SMOOTH, 0, -k, fy + h, -k, k, fy + h, k);
        List<int[]> path = ringPath(k);
        int n = path.size();
        for (int i = h - 5; i <= h - 2; i++) {                                                       // the last steps climb through a gap in the platform
            int[] c = path.get(i % n);
            o.air(c[0], fy + h, c[1], c[0], fy + h, c[1]);
        }
        for (int i = 0; i < h; i++) {
            int[] c = path.get(i % n), nx = path.get((i + 1) % n);
            int face = nx[0] > c[0] ? EAST : nx[0] < c[0] ? WEST : nx[1] > c[1] ? SOUTH : NORTH;
            o.put(Pal.STAIRS, face, c[0], fy + 1 + i, c[1]);
        }
        for (int x = -m; x <= m; x++) {                                                              // battlements
            for (int z = -m; z <= m; z++) {
                if ((Math.abs(x) == m || Math.abs(z) == m) && ((x + z) & 1) == 0) o.put(Pal.BRICKS, 0, x, fy + h + 1, z);
            }
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                o.put(Pal.GILDED, 0, sx * m, fy + h + 1, sz * m);
                o.put(Pal.LANTERN, 0, sx * m, fy + h + 2, sz * m);
            }
        }
        if (ruined) {
            int hb = fy + 3 + r.range(h / 3, h / 2 + 2);
            int cut = r.range(-1, 1);
            o.air(-m - 1, hb, -m - 1, cut, fy + h + 4, m + 1);
            o.air(cut + 1, hb + 3, -m - 1, m + 1, fy + h + 4, m + 1);
            int side = r.chance(50) ? 1 : -1, at = r.range(-1, 1);
            o.air(side * (m - 1), hb - 4, at - 1, side * (m + 1), hb + 3, at + 1);                    // a gash in the east or west wall
            rubble(o, r, fy, m, m, 9);
        }
    }

    /** The cells just inside a tower's wall, going clockwise from the one after its door, so the door end of the ring comes last. */
    private static List<int[]> ringPath(int k) {
        List<int[]> ring = new ArrayList<>();
        for (int x = -k; x <= k; x++) ring.add(new int[]{x, -k});
        for (int z = -k + 1; z <= k; z++) ring.add(new int[]{k, z});
        for (int x = k - 1; x >= -k; x--) ring.add(new int[]{x, k});
        for (int z = k - 1; z >= -k + 1; z--) ring.add(new int[]{-k, z});
        int first = 0;
        for (int i = 0; i < ring.size(); i++) if (ring.get(i)[0] == 2 && ring.get(i)[1] == -k) first = i;
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < ring.size(); i++) out.add(ring.get((first + i) % ring.size()));
        return out;
    }

    /**
     * A hall with a pitched roof, 2*hx+1 long (along x) and 2*hz+1 wide, doors at both ends, pilasters and slit windows along the sides, a
     * gold carpet and chandeliers inside.
     */
    static void hall(CityOps o, CityRng r, int fy, int hx, int hz, boolean ruined) {
        int top = fy + 6;
        o.ring(Pal.BRICKS, 0, -hx, -hz, hx, hz, fy + 1, top);
        o.ring(Pal.GILDED, 0, -hx, -hz, hx, hz, fy + 1, fy + 1);
        for (int x = -hx + 2; x <= hx - 2; x += 4) {
            o.box(Pal.GILDED, 0, x, fy + 2, -hz, x, top, -hz);
            o.box(Pal.GILDED, 0, x, fy + 2, hz, x, top, hz);
            o.box(Pal.GILDED, 0, x, fy + 2, -hz + 1, x, top, -hz + 1);
            o.box(Pal.GILDED, 0, x, fy + 2, hz - 1, x, top, hz - 1);
        }
        for (int x = -hx + 4; x <= hx - 4; x += 4) {
            o.box(Pal.GLASS, 0, x, fy + 3, -hz, x, fy + 4, -hz);
            o.box(Pal.GLASS, 0, x, fy + 3, hz, x, fy + 4, hz);
        }
        for (int s = -1; s <= 1; s += 2) {
            o.air(s * hx, fy + 1, -1, s * hx, fy + 4, 1);
            o.box(Pal.GOLD, 0, s * hx, fy + 5, -2, s * hx, fy + 5, 2);
        }
        for (int k = 0; k < hz; k++) {                                                               // the pitched roof and the gables under it
            int y = fy + 7 + k, zz = hz - k, w = hz - k - 1;
            o.box(Pal.STAIRS, NORTH, -hx, y, zz, hx, y, zz);
            o.box(Pal.STAIRS, SOUTH, -hx, y, -zz, hx, y, -zz);
            for (int s = -1; s <= 1; s += 2) o.box(Pal.BRICKS, 0, s * hx, y, -w, s * hx, y, w);
        }
        o.box(Pal.BRICKS, 0, -hx, fy + 6 + hz, 0, hx, fy + 6 + hz, 0);
        o.box(Pal.SLAB, 0, -hx, fy + 7 + hz, 0, hx, fy + 7 + hz, 0);
        o.box(Pal.CARPET_GOLD, 0, -hx + 1, fy + 1, -1, hx - 1, fy + 1, 1);
        o.box(Pal.CARPET_PURPLE, 0, -hx + 1, fy + 1, -2, hx - 1, fy + 1, -2);
        o.box(Pal.CARPET_PURPLE, 0, -hx + 1, fy + 1, 2, hx - 1, fy + 1, 2);
        for (int x = -hx + 4; x <= hx - 4; x += 4) {
            o.put(Pal.CANDLE, candles(3), x, fy + 1, -(hz - 1));
            o.put(Pal.CANDLE, candles(3), x, fy + 1, hz - 1);
        }
        for (int x = -hx / 2; x <= hx / 2; x += hx / 2) {
            o.box(Pal.CHAIN, 0, x, fy + 3 + hz, 0, x, fy + 5 + hz, 0);
            o.put(Pal.LANTERN, FLIP, x, fy + 2 + hz, 0);
        }
        if (ruined) {
            int x0 = r.range(-hx + 2, hx - 7);
            o.air(x0, fy + 6, -hz, x0 + r.range(4, 6), fy + 8 + hz, hz);                              // the roof has fallen in
            int s = r.chance(50) ? 1 : -1;
            o.air(s * (hx - 3), fy + 4, hz - 3, s * hx, fy + 8 + hz, hz);                              // and a corner with it
            for (int i = 0; i < 6; i++) o.put(Pal.CRACKED, 0, x0 + r.below(6), fy + 1, r.range(-hz + 1, hz - 1));
            rubble(o, r, fy, hx, hz, 10);
        }
    }

    /** A small open shrine: a gilded platform, four pillars under a stepped roof, and an amethyst on a pedestal lit by candles. */
    static void shrine(CityOps o, CityRng r, int fy, int m, boolean ruined) {
        int p = m - 1;
        o.box(Pal.SMOOTH, 0, -m, fy + 1, -m, m, fy + 1, m);
        o.ring(Pal.GILDED, 0, -m, -m, m, m, fy + 1, fy + 1);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                o.box(Pal.BRICKS, 0, sx * p, fy + 2, sz * p, sx * p, fy + 5, sz * p);
                o.put(Pal.GILDED, 0, sx * p, fy + 6, sz * p);
            }
        }
        for (int i = 0; i < m; i++) o.box(i == 0 ? Pal.SMOOTH : Pal.BRICKS, 0, -(m - i), fy + 7 + i, -(m - i), m - i, fy + 7 + i, m - i);
        o.put(Pal.GOLD, 0, 0, fy + 7 + m, 0);
        o.put(Pal.LANTERN, 0, 0, fy + 8 + m, 0);
        o.box(Pal.GILDED, 0, 0, fy + 2, 0, 0, fy + 3, 0);
        o.put(Pal.AMETHYST, 0, 0, fy + 4, 0);
        o.put(Pal.CLUSTER, 0, 0, fy + 5, 0);
        for (int s = -1; s <= 1; s += 2) {
            o.put(Pal.CANDLE, candles(3), s * p, fy + 2, 0);
            o.put(Pal.CANDLE, candles(3), 0, fy + 2, s * p);
            o.put(Pal.CHAIN, 0, s * p, fy + 6, 0);
            o.put(Pal.LANTERN, FLIP, s * p, fy + 5, 0);
            o.put(Pal.CHAIN, 0, 0, fy + 6, s * p);
            o.put(Pal.LANTERN, FLIP, 0, fy + 5, s * p);
        }
        if (ruined) {
            o.air(1, fy + 4, 1, m + 1, fy + 12, m + 1);
            rubble(o, r, fy, m, m, 7);
        }
    }

    /** A small monolith on a gilded plinth, crowned with amethyst and ringed with candles. */
    static void obelisk(CityOps o, CityRng r, int fy, boolean ruined) {
        o.box(Pal.GILDED, 0, -1, fy + 1, -1, 1, fy + 1, 1);
        o.box(Pal.BRICKS, 0, 0, fy + 2, 0, 0, fy + 8, 0);
        o.put(Pal.GILDED, 0, 0, fy + 4, 0);
        o.put(Pal.GILDED, 0, 0, fy + 7, 0);
        o.put(Pal.AMETHYST, 0, 0, fy + 9, 0);
        o.put(Pal.CLUSTER, 0, 0, fy + 10, 0);
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) o.put(Pal.CANDLE, candles(2), sx, fy + 2, sz);
        if (ruined) {
            o.air(-1, fy + 5 + r.below(2), -1, 1, fy + 12, 1);
            rubble(o, r, fy, 1, 1, 5);
        }
    }

    /**
     * A triumphal arch that spans an avenue: two heavy pillars, a lintel with corbels, a gold crest with an amethyst, a lantern hung under it.
     * The opening runs along x and is seven blocks wide.
     */
    static void arch(CityOps o, CityRng r, int fy, boolean ruined) {
        for (int s = -1; s <= 1; s += 2) {
            o.box(Pal.BRICKS, 0, 4 * s, fy + 1, -1, 5 * s, fy + 9, 1);
            o.box(Pal.GILDED, 0, 4 * s, fy + 1, -1, 5 * s, fy + 1, 1);
            o.box(Pal.GILDED, 0, 4 * s, fy + 2, 0, 4 * s, fy + 7, 0);
            o.box(Pal.STAIRS, (s == 1 ? WEST : EAST) | FLIP, 3 * s, fy + 7, -1, 3 * s, fy + 7, 1);
        }
        o.box(Pal.BRICKS, 0, -3, fy + 8, -1, 3, fy + 9, 1);
        o.box(Pal.GILDED, 0, -5, fy + 9, -1, 5, fy + 9, 1);
        o.box(Pal.GOLD, 0, -5, fy + 10, 0, 5, fy + 10, 0);
        o.put(Pal.AMETHYST, 0, 0, fy + 11, 0);
        o.put(Pal.CLUSTER, 0, 0, fy + 12, 0);
        o.put(Pal.CHAIN, 0, 0, fy + 7, 0);
        o.put(Pal.LANTERN, FLIP, 0, fy + 6, 0);
        if (ruined) {
            int s = r.chance(50) ? 1 : -1;
            o.air(s, fy + 6, -1, 6 * s, fy + 13, 1);
            o.air(-4 * s, fy + 9, -1, -6 * s, fy + 13, 1);
            rubble(o, r, fy, 5, 1, 9);
        }
    }

    /** Broken stone lying on the ring of floor just outside a footprint with half-widths hx and hz. */
    private static void rubble(CityOps o, CityRng r, int fy, int hx, int hz, int count) {
        for (int i = 0; i < count; i++) {
            int x, z;
            if (r.chance(50)) {
                x = r.range(-hx - 1, hx + 1);
                z = r.chance(50) ? -hz - 1 : hz + 1;
            } else {
                x = r.chance(50) ? -hx - 1 : hx + 1;
                z = r.range(-hz - 1, hz + 1);
            }
            o.put(r.chance(60) ? Pal.CRACKED : Pal.BLACKSTONE, 0, x, fy + 1, z);
        }
    }
}
