package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import static com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Draw.*;

/**
 * Why: every hall, whatever its theme, is a SEALED room with the same skeleton: a foundation, a floor, a two-block boundary wall all the way round, a ceiling, a doorway in the north wall for the
 * Gatekeeper and one in the south wall for the party, a dais for the Guardian Altar in the middle, and enough light to fight in. That skeleton is built here once, from a palette ({@link Pal}) and a few
 * options ({@link Opts}); a theme then clears and dresses what it wants on top (its pillars, graves, pits and banners). Doing the skeleton in one place is what lets the tests promise the same
 * things of all thirty-six halls: the walls are closed except at the two doorways, the doorways are on the middle column, the dais and the altar are where the floor's template expects them, and
 * the cells the template owns are RESERVED (never written in a floor of the Mega-Dungeon).
 */
public final class Shell {
    private Shell() {}

    /** The materials of a hall. Block fields are texts ({@code minecraft:stone_bricks}); {@code slab} and {@code stair} are family names ({@code stone_brick_slab}); {@code light} is the level of the hidden light grid. */
    public record Pal(String foundation, String floorA, String floorB, String floorC, String wall, String wallB, String trim, String pillar, String cap, String slab, String stair, String glass, String lamp,
                      int light) {}

    /**
     * The options of a skeleton: {@code floor} 0 checker of A and B, 1 plain A flecked with B, 2 rings of A, B and C, 3 plain A; {@code roof} 0 flat, 1 flat with a glass skylight, 2 flat with a ring of
     * lamps; {@code pilasters} the distance between wall pilasters (0 for none); {@code dais} whether to build the three-tier dais.
     */
    public record Opts(int floor, int roof, int pilasters, boolean dais) {
        public static final Opts DEFAULT = new Opts(0, 0, 6, true);
    }

    /** Builds the skeleton of a hall and returns the plan, ready for a theme to dress. */
    public static BossArenaStructure start(String theme, int size, int height, Pal p, Opts o) {
        BossArenaStructure a = new BossArenaStructure(theme, size, height);
        ArenaRng rng = ArenaRng.of(theme + "#shell", size);
        int c = a.c, last = size - 1;

        // the cells the floor's template keeps: the two doorways and the altar column
        for (int y = 1; y <= 4; y++)
            for (int x = c - 1; x <= c + 1; x++) {
                a.reserve(x, y, 0);
                a.reserve(x, y, 1);
                a.reserve(x, y, last);
                a.reserve(x, y, last - 1);
            }
        for (int y = 1; y <= 3; y++) a.reserve(c, y, c);

        // foundation and floor
        fill(a, 0, -BossArenaStructure.FOUNDATION, 0, last, -1, last, p.foundation());
        for (int z = 0; z <= last; z++)
            for (int x = 0; x <= last; x++) a.set(x, 0, z, floorBlock(a, p, o.floor(), x, z, rng));

        // the room is emptied first: in a floor of the Mega-Dungeon the old hall is still standing in the footprint
        clear(a, 2, 1, 2, last - 2, height - 2, last - 2);

        // the boundary wall, two blocks thick, with a trim band every sixth row and flecks of the second wall block
        for (int layer = 0; layer < 2; layer++) {
            for (int i = layer; i <= last - layer; i++) {
                for (int y = 1; y <= height - 1; y++) {
                    String w = wallBlock(p, y, rng);
                    a.set(i, y, layer, w);
                    a.set(i, y, last - layer, w);
                    a.set(layer, y, i, w);
                    a.set(last - layer, y, i, w);
                }
            }
        }

        // the ceiling and its cornice
        fill(a, 0, height - 1, 0, last, height - 1, last, p.wall());
        ring(a, c, c, size / 2.0 - 4.5, size, height - 2, p.trim());
        if (o.roof() == 1) disc(a, c, c, size * 0.22, height - 1, p.glass());
        if (o.roof() == 2) {
            for (int k = 0; k < 12; k++) {
                double ang = k * Math.PI / 6.0;
                a.set((int) Math.round(c + Math.cos(ang) * size * 0.3), height - 2, (int) Math.round(c + Math.sin(ang) * size * 0.3), p.lamp());
            }
        }

        // pilasters round the inside of the wall
        if (o.pilasters() > 0) {
            for (int i = 4; i < last - 3; i += o.pilasters()) {
                if (Math.abs(i - c) <= 3) continue;                                        // the two doorways stay framed by the porch below
                pilaster(a, p, i, 2, height);
                pilaster(a, p, i, last - 2, height);
                pilaster(a, p, 2, i, height);
                pilaster(a, p, last - 2, i, height);
            }
        }

        // the porches: two pilasters and a lintel either side of each doorway
        for (int side = 0; side < 2; side++) {
            int z = side == 0 ? 2 : last - 2;
            for (int s = -1; s <= 1; s += 2) {
                int x = c + s * 3;
                a.set(x, 1, z, p.trim());
                for (int y = 2; y <= 6; y++) a.set(x, y, z, p.pillar());
                a.set(x, 7, z, p.cap());
            }
            for (int x = c - 3; x <= c + 3; x++) a.set(x, 7, z, p.trim());
            for (int x = c - 2; x <= c + 2; x++) a.set(x, 6, z, x == c ? p.cap() : p.wallB());
        }

        // the dais under the altar: three tiers like the template's, the middle column is the template's (reserved)
        if (o.dais()) dais(a, p);

        // the hidden light grid
        for (int zi = -3; zi <= 3; zi++)
            for (int xi = -3; xi <= 3; xi++) {
                int x = c + xi * 7, z = c + zi * 7;
                if (x < 3 || z < 3 || x > last - 3 || z > last - 3) continue;
                a.set(x, 2, z, light(p.light()));
            }

        // what a standalone hall adds: the Gatekeeper slab, the open entry, the altar on its pedestal, and the seal cells
        for (int x = c - 1; x <= c + 1; x++) {
            for (int z = 0; z <= 1; z++)
                for (int y = 1; y <= 4; y++) a.extra(x, y, z, "nusmp:gatekeeper_door[kind=sector,open=false,half=" + (y <= 2 ? "lower" : "upper") + "]");
            for (int z = last - 1; z <= last; z++)
                for (int y = 1; y <= 4; y++) a.extra(x, y, z, BossArenaStructure.AIR);
            for (int y = 1; y <= 4; y++) a.sealCell(x, y, last - 1, b("iron_bars"));
        }
        a.extra(c, 1, c, p.floorC());
        a.extra(c, 2, c, p.trim());
        a.extra(c, 3, c, "nusmp:guardian_altar");
        return a;
    }

    private static String floorBlock(BossArenaStructure a, Pal p, int pattern, int x, int z, ArenaRng rng) {
        switch (pattern) {
            case 1:
                return rng.chance(0.14) ? p.floorB() : p.floorA();
            case 2: {
                double r = dist(a, x, z);
                if (r > a.size / 2.0 - 3) return p.floorC();
                int band = (int) (r / 4.0);
                return band % 3 == 0 ? p.floorA() : band % 3 == 1 ? p.floorB() : p.floorC();
            }
            case 3:
                return p.floorA();
            default:
                if (x < 3 || z < 3 || x > a.size - 4 || z > a.size - 4) return p.floorC();
                return ((x + z) & 1) == 0 ? p.floorA() : p.floorB();
        }
    }

    private static String wallBlock(Pal p, int y, ArenaRng rng) {
        if (y % 6 == 0) return p.trim();
        return rng.chance(0.12) ? p.wallB() : p.wall();
    }

    private static void pilaster(BossArenaStructure a, Pal p, int x, int z, int height) {
        a.set(x, 1, z, p.trim());
        for (int y = 2; y <= height - 4; y++) a.set(x, y, z, p.pillar());
        a.set(x, height - 3, z, p.cap());
    }

    /** Three tiers like the template's dais: a ring of slabs out to 8.5, a block disc to 6.5, a ring of slabs at height 2 out to 4.6 and a block disc at height 2 to 2.8. */
    private static void dais(BossArenaStructure a, Pal p) {
        int c = a.c;
        for (int z = c - 10; z <= c + 10; z++)
            for (int x = c - 10; x <= c + 10; x++) {
                double r = Math.hypot(x - c, z - c);
                if (r > 6.5 && r <= 8.5) a.set(x, 1, z, slab(p.slab(), "bottom"));
                else if (r <= 6.5) a.set(x, 1, z, p.floorC());
                if (r > 2.8 && r <= 4.6) a.set(x, 2, z, slab(p.slab(), "bottom"));
                else if (r <= 2.8) a.set(x, 2, z, p.trim());
            }
    }
}
