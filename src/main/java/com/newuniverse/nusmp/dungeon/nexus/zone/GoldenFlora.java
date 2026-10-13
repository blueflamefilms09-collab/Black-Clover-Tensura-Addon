package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenGrid.Grove;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenGrid.Site;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenGrid.Vine;

import java.util.ArrayList;
import java.util.List;

/**
 * The living things of the Golden City's garden as shapes, written once as ops (pure: no Minecraft imports): the giant golden trees, the
 * smaller azalea trees, the mushrooms the size of houses and the small glowing ones, the groves that sprout and are pruned again, and what
 * grows from the ceiling. Heights are offsets above the floor of the garden, like everything in {@link GoldenGrid}.
 */
final class GoldenFlora {
    private GoldenFlora() {}

    static final String LOG = "minecraft:stripped_oak_log", GOLD_LEAVES = "nusmp:golden_leaves", AZALEA_LEAVES = "minecraft:azalea_leaves",
            FLOWERING_LEAVES = "minecraft:flowering_azalea_leaves", FROGLIGHT = "minecraft:ochre_froglight", PEARL = "minecraft:pearlescent_froglight",
            SHROOMLIGHT = "minecraft:shroomlight", STEM = "minecraft:mushroom_stem", RED_CAP = "minecraft:red_mushroom_block", BROWN_CAP = "minecraft:brown_mushroom_block",
            AZALEA = "minecraft:azalea", FLOWERING_AZALEA = "minecraft:flowering_azalea", PERSISTENT = "persistent=true";

    // ---------------------------------------------------------------- the shapes of a canopy
    /** The radius of the layer `dy` (-ry .. ry) of a canopy of half-width r and half-height ry: a flattened dome, flat at the top and the bottom. */
    static int rho(int r, int ry, int dy) {
        double k = 1.0 - (dy * (double) dy) / ((ry + 1.0) * (ry + 1.0));
        return (int) Math.round(r * Math.sqrt(Math.max(0.0, k)));
    }

    /** The y (offset above the floor) of the middle layer of a tree's canopy. */
    static int canopyMiddle(GoldenGrid g, Site s) {
        int top = g.groundAt(s.u(), s.v()) + s.ht();
        return s.kind() == GoldenGrid.GREAT_TREE ? top - 1 : top;
    }

    /** The lowest ground in the square of half-width `half` round a place: the trunk starts there. */
    static int lowestGround(GoldenGrid g, int u, int v, int half) {
        int low = Integer.MAX_VALUE;
        for (int du = -half; du <= half; du++) for (int dv = -half; dv <= half; dv++) if (g.g(u + du, v + dv)) low = Math.min(low, g.groundAt(u + du, v + dv));
        return low == Integer.MAX_VALUE ? g.groundAt(u, v) : low;
    }

    /** Where vines can hang from the underside of a tree's canopy: one anchor per chosen column, at the lowest leaf of the column. */
    static List<Vine> canopyVines(GoldenGrid g, Site s) {
        List<Vine> out = new ArrayList<>();
        int r = s.r(), ry = s.ry(), yc = canopyMiddle(g, s);
        boolean great = s.kind() == GoldenGrid.GREAT_TREE, onRoof = g.isRoof(s.u(), s.v());
        for (int du = -r; du <= r; du++) {
            for (int dv = -r; dv <= r; dv++) {
                double d = GoldenGrid.dist(du, dv);
                if (d > r || (great && (du == 0 || dv == 0)) || du * du + dv * dv <= 4) continue;
                int u = s.u() + du, v = s.v() + dv;
                if (!g.g(u, v) || (onRoof ? !g.isRoof(u, v) || g.isRoofEdge(u, v) : g.inCity(u, v, 1)) || g.path[u + GoldenGrid.OFF][v + GoldenGrid.OFF] || g.water[u + GoldenGrid.OFF][v + GoldenGrid.OFF]) continue;
                int dyLow = Integer.MIN_VALUE;
                for (int dy = -ry; dy <= ry; dy++) {
                    if (rho(r, ry, dy) >= d) {
                        dyLow = dy;
                        break;
                    }
                }
                if (dyLow == Integer.MIN_VALUE) continue;
                int h = GoldenRng.hash(g.seed, u, v, 31);
                if (h % 100 >= (great ? 16 : 34)) continue;
                int anchor = yc + dyLow;
                int clear = anchor - 1 - (g.groundAt(u, v) + 1);
                int max = Math.min(clear, great ? 4 + (h >> 8) % 7 : 3 + (h >> 8) % 5);
                if (max >= 2) out.add(new Vine(u, v, anchor, max, true));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- trees
    /** A giant golden tree or an azalea tree: stripped oak trunk, a flattened dome of golden leaves with flowering azalea leaves and golden lanterns in it. */
    static void tree(NexusOps o, GoldenGrid g, Site s) {
        final int f = g.floorY, u = s.u(), v = s.v(), r = s.r(), ry = s.ry();
        boolean great = s.kind() == GoldenGrid.GREAT_TREE;
        int low = lowestGround(g, u, v, great ? 2 : 0), top = g.groundAt(u, v) + s.ht(), yc = canopyMiddle(g, s);
        for (int dy = -ry; dy <= ry; dy++) {                                                   // the canopy first: the trunk grows through it
            int rad = rho(r, ry, dy);
            o.frame(u, v, false, 1, 1);
            o.disc(GOLD_LEAVES, "", rad, rad, f + yc + dy, f + yc + dy);
            o.resetFrame();
        }
        GoldenRng rng = new GoldenRng(GoldenRng.hash(g.seed, u, v, 41));
        int accents = great ? 18 + r * 2 : 6 + r;
        for (int k = 0; k < accents; k++) {                                                    // flowers and lanterns on the surface of the canopy
            int dy = rng.range(-ry, ry), rad = rho(r, ry, dy);
            if (rad < 1) continue;
            int du = rng.range(-rad, rad), dv = rng.range(-rad, rad);
            if (du * du + dv * dv > rad * rad) continue;
            double pick = rng.unit();
            if (g.isVineAnchor(u + du, v + dv, yc + dy)) continue;                             // a vine hangs from this leaf
            boolean underside = dy <= -ry + 1;
            if (underside && pick < 0.35) o.put(FROGLIGHT, u + du, f + yc + dy, v + dv);
            else if (pick < 0.75) o.put(FLOWERING_LEAVES, PERSISTENT, u + du, f + yc + dy, v + dv);
            else if (pick < 0.92) o.put(AZALEA_LEAVES, PERSISTENT, u + du, f + yc + dy, v + dv);
            else o.put(SHROOMLIGHT, u + du, f + yc + dy, v + dv);
        }
        if (great) {
            o.box(LOG, "axis=y", u - 2, f + low, v - 1, u + 2, f + low + 1, v + 1);              // the flare of the roots
            o.box(LOG, "axis=y", u - 1, f + low, v - 2, u + 1, f + low + 1, v + 2);
            o.box(LOG, "axis=y", u - 1, f + low, v - 1, u + 1, f + top - 7, v + 1);              // the trunk, three blocks thick
            o.box(LOG, "axis=y", u, f + top - 6, v, u, f + top, v);
            int len = r * 2 / 3;
            o.box(LOG, "axis=x", u - len, f + top - 6, v, u + len, f + top - 6, v);              // four boughs under the canopy
            o.box(LOG, "axis=z", u, f + top - 6, v - len, u, f + top - 6, v + len);
            o.box(LOG, "axis=x", u - len, f + top - 5, v, u - len + 1, f + top - 5, v);
            o.box(LOG, "axis=x", u + len - 1, f + top - 5, v, u + len, f + top - 5, v);
            o.box(LOG, "axis=z", u, f + top - 5, v - len, u, f + top - 5, v - len + 1);
            o.box(LOG, "axis=z", u, f + top - 5, v + len - 1, u, f + top - 5, v + len);
            for (int k = 0; k < 14; k++) {                                                     // glow lichen on the bark
                int y = f + low + 3 + rng.below(Math.max(1, top - low - 11)), side = rng.below(4), l = rng.range(-1, 1);
                switch (side) {
                    case 0 -> o.put("minecraft:glow_lichen", "west=true", u + 2, y, v + l);
                    case 1 -> o.put("minecraft:glow_lichen", "east=true", u - 2, y, v + l);
                    case 2 -> o.put("minecraft:glow_lichen", "north=true", u + l, y, v + 2);
                    default -> o.put("minecraft:glow_lichen", "south=true", u + l, y, v - 2);
                }
            }
        } else {
            o.box(LOG, "axis=y", u, f + low, v, u, f + top, v);
        }
    }

    // ---------------------------------------------------------------- mushrooms
    /** A mushroom: the giants have a stem three blocks thick and a cap with glowing gills under it; the small ones are lamps. */
    static void mushroom(NexusOps o, GoldenGrid g, Site s) {
        final int f = g.floorY, u = s.u(), v = s.v();
        int low = lowestGround(g, u, v, s.kind() == GoldenGrid.GIANT_MUSHROOM ? 1 : 0), base = g.groundAt(u, v);
        if (s.kind() == GoldenGrid.MUSHROOM) {
            int topY = base + s.ht();
            o.box(STEM, u, f + low, v, u, f + topY, v);
            o.frame(u, v, false, 1, 1);
            o.disc(SHROOMLIGHT, "", 2, 2, f + topY + 1, f + topY + 1);
            o.disc(SHROOMLIGHT, "", 1, 1, f + topY + 2, f + topY + 2);
            o.resetFrame();
            return;
        }
        int r = s.r(), capH = Math.max(2, r / 2 + 1), topY = base + s.ht();
        String cap = s.variant() % 3 == 0 ? RED_CAP : BROWN_CAP;
        o.box(STEM, u - 1, f + low, v - 1, u + 1, f + topY, v + 1);
        o.frame(u, v, false, 1, 1);
        o.disc(SHROOMLIGHT, "", r - 1, r - 1, f + topY + 1, f + topY + 1);                    // the gills glow under the cap
        o.discHollow(cap, "", r, r, 1, f + topY + 1, f + topY + 1);                            // the rim of the cap
        for (int i = 1; i < capH; i++) {
            int rad = domeRadius(r, capH, i);
            o.disc(cap, "", rad, rad, f + topY + 1 + i, f + topY + 1 + i);
        }
        o.resetFrame();
        o.box(STEM, u - 1, f + topY + 1, v - 1, u + 1, f + topY + 1, v + 1);                    // the stem runs up into the cap
        GoldenRng rng = new GoldenRng(GoldenRng.hash(g.seed, u, v, 43));
        int topRad = domeRadius(r, capH, capH - 1);
        for (int k = 0; k < 4 + r; k++) {                                                      // pale spots on the cap
            int du = rng.range(-r + 1, r - 1), dv = rng.range(-r + 1, r - 1);
            if (du * du + dv * dv <= topRad * topRad) o.put(STEM, u + du, f + topY + capH, v + dv);
        }
    }

    private static int domeRadius(int r, int capH, int i) {
        return (int) Math.round(r * Math.sqrt(Math.max(0.0, 1.0 - (i * (double) i) / (capH * (double) capH))));
    }

    // ---------------------------------------------------------------- groves
    /** Where a grove's cells lie, relative to the ground block in its middle: dx and dz in -1..1, dy in 1..5. */
    static final int GROVE_LOW = 1, GROVE_HIGH = 5;

    /** The block (and properties) of a grove in `stage` at the cell (dx, dy, dz), or null for air. Stage 0 is bare, 1 a sapling, 2 a bush, 3 a small tree. */
    static String[] groveCell(int stage, int dx, int dy, int dz) {
        boolean mid = dx == 0 && dz == 0;
        switch (stage) {
            case 1:
                return mid && dy == 1 ? new String[]{AZALEA, ""} : null;
            case 2:
                if (mid && dy == 1) return new String[]{FLOWERING_AZALEA, ""};
                if (dy == 1 && Math.abs(dx) + Math.abs(dz) == 1) return new String[]{AZALEA_LEAVES, PERSISTENT};
                if (mid && dy == 2) return new String[]{AZALEA_LEAVES, PERSISTENT};
                return null;
            case 3:
                if (mid && dy >= 1 && dy <= 4) return new String[]{LOG, "axis=y"};
                if ((dy == 3 || dy == 4) && !mid) return Math.abs(dx) + Math.abs(dz) == 2 && dy == 3 ? new String[]{FLOWERING_LEAVES, PERSISTENT} : new String[]{GOLD_LEAVES, ""};
                if (mid && dy == 5) return new String[]{GOLD_LEAVES, ""};
                return null;
            default:
                return null;
        }
    }

    /** The block that tells which stage a grove is in: the one in the middle just above the ground. */
    static String groveMarker(int stage) {
        String[] c = groveCell(stage, 0, 1, 0);
        return c == null ? "minecraft:air" : c[0];
    }

    /** The grove at its first stage, put in the plan: a ring of podzol round a bare middle of rooted dirt and the stage it starts in. */
    static void grove(NexusOps o, GoldenGrid g, Grove gr) {
        final int f = g.floorY, u = gr.u(), v = gr.v(), base = g.groundAt(u, v);
        o.box("minecraft:podzol", u - 1, f + base, v - 1, u + 1, f + base, v + 1);
        o.put("minecraft:rooted_dirt", u, f + base, v);
        int stage = initialGroveStage(gr);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = GROVE_LOW; dy <= GROVE_HIGH; dy++) {
                    String[] c = groveCell(stage, dx, dy, dz);
                    if (c != null) o.put(c[0], c[1], u + dx, f + base + dy, v + dz);
                }
            }
        }
    }

    static int initialGroveStage(Grove gr) { return gr.phase() % 4; }

    // ---------------------------------------------------------------- wall decor
    /** Amethyst geodes in the wall at the foot of the cavern: a block of amethyst in the shell and a cluster pointing out of it. */
    static void geodes(NexusOps o, GoldenGrid g) {
        GoldenRng rng = new GoldenRng(g.seed ^ 0x6A09E667F3BCC909L);
        int[] dx = {1, -1, 0, 0}, dz = {0, 0, 1, -1};
        int made = 0;
        for (int trial = 0; trial < 600 && made < 12; trial++) {
            int u = rng.range(-GoldenGrid.REACH, GoldenGrid.REACH), v = rng.range(-GoldenGrid.REACH, GoldenGrid.REACH);
            if (!g.g(u, v) || g.path[u + GoldenGrid.OFF][v + GoldenGrid.OFF] || g.coverAt(u, v) == GoldenGrid.POOL) continue;
            for (int k = 0; k < 4; k++) {
                int su = u + dx[k], sv = v + dz[k];
                if (g.g(su, sv) || g.shellAt(su, sv) != 1 || isSpring(g, su, sv)) continue;
                int y = g.groundAt(u, v) + 2 + rng.below(3);
                if (g.ceil(u, v) < y + 4) break;
                String facing = dx[k] > 0 ? "west" : dx[k] < 0 ? "east" : dz[k] > 0 ? "north" : "south";      // points back out of the wall into the garden
                o.box("minecraft:amethyst_block", su, g.floorY + y - 1, sv, su, g.floorY + y + 1, sv);
                o.put("minecraft:budding_amethyst", su, g.floorY + y, sv);
                o.put("minecraft:amethyst_cluster", "facing=" + facing, u, g.floorY + y, v);
                if (rng.chance(60)) o.put("minecraft:small_amethyst_bud", "facing=" + facing, u, g.floorY + y + 1, v);
                made++;
                break;
            }
        }
    }

    private static boolean isSpring(GoldenGrid g, int u, int v) {
        for (GoldenGrid.Fall w : g.falls) if (w.srcU() == u && w.srcV() == v) return true;
        return false;
    }

    // ---------------------------------------------------------------- the ceiling
    /** The block that skins a ceiling column (the first cell of the shell above the air). */
    static String ceilingSkin(long seed, int u, int v) {
        double r = GoldenRng.unit(seed, u >> 1, v >> 1, 51);
        if (r < 0.42) return "minecraft:moss_block";
        if (r < 0.60) return "minecraft:tuff";
        if (r < 0.72) return "minecraft:deepslate";
        if (r < 0.78) return "minecraft:calcite";
        if (r < 0.83) return "minecraft:smooth_basalt";
        if (r < 0.90) return FROGLIGHT;
        if (r < 0.92) return PEARL;
        if (r < 0.96) return SHROOMLIGHT;
        return "minecraft:dripstone_block";
    }

    /** What hangs from the ceiling: cave vines and glow berries, glow lichen, hanging roots and spore blossoms. Never above a waterfall, nor over a tree or a mushroom. */
    static void ceilingGrowth(NexusOps o, GoldenGrid g) {
        final int f = g.floorY;
        for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
            for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                if (!g.g(u, v) || g.crown[u + GoldenGrid.OFF][v + GoldenGrid.OFF]) continue;
                boolean fall = false;
                for (GoldenGrid.Fall w : g.falls) if (w.u() == u && w.v() == v) fall = true;
                if (fall) continue;
                int top = f + g.ceil(u, v);
                int h = GoldenRng.hash(g.seed, u, v, 53);
                int roll = h % 1000;
                if (roll < 70) {
                    int len = Math.min(3 + (h >> 10) % 12, g.ceil(u, v) - g.groundAt(u, v) - 6);                  // a vine ends well above the ground, or the roof garden, it hangs over
                    if (len < 2) continue;
                    boolean berries = (h >> 20) % 100 < 70;
                    o.box("minecraft:cave_vines_plant", berries ? "berries=true" : "berries=false", u, top - len + 1, v, u, top, v);
                    o.put("minecraft:cave_vines", berries ? "age=25,berries=true" : "age=25,berries=false", u, top - len, v);
                } else if (roll < 130) {
                    o.put("minecraft:glow_lichen", "up=true", u, top, v);
                } else if (roll < 160) {
                    o.put("minecraft:hanging_roots", u, top, v);
                } else if (roll < 180) {
                    o.put("minecraft:spore_blossom", u, top, v);
                }
            }
        }
    }
}
