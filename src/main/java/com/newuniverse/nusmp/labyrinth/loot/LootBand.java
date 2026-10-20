package com.newuniverse.nusmp.labyrinth.loot;

/**
 * Why: the dungeon has 2000 floors and a drop table cannot have 2000 rows, so depth is cut into five BANDS that the tables and the rarity weights read: the first ten floors
 * (band 0), up to floor 30 (1), up to 100 (2), up to 300 (3) and everything deeper (4). The cut points follow the places where the rest of the mod changes character (a
 * sector is ten floors, the squads grow at 10 and 100 floors). Pure.
 */
public final class LootBand {
    private LootBand() {}

    public static final int BANDS = 5;

    /** The band of a floor; the sign is ignored (the abyssal floors are as deep as the holy ones) and 0 (unknown) is the first band. */
    public static int of(int depth) {
        int d = Math.abs(depth);
        if (d < 10) return 0;
        if (d < 30) return 1;
        if (d < 100) return 2;
        if (d < 300) return 3;
        return 4;
    }
}
