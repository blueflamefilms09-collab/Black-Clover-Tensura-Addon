package com.newuniverse.nusmp.dungeon.nexus.vault;

/**
 * What the vault planner is allowed to know about the world: one classified block at a time. The game adapts a level to it (WorldTerrain);
 * the tests give it made-up caves. A chunk that is not loaded answers {@link #UNKNOWN} and the planner then waits instead of guessing.
 */
@FunctionalInterface
public interface VaultTerrain {
    /** Open air, or a decoration that gives way (plants, carpets, mycelium veils). */
    int AIR = 0;
    /** Water, flowing or still. */
    int WATER = 1;
    /** Lava: never built next to. */
    int LAVA = 2;
    int STONE = 3;
    int DEEPSLATE = 4;
    /** Any other solid block (ores, tuff, dripstone, moss, a wall someone built). */
    int OTHER = 5;
    /** Not loaded. */
    int UNKNOWN = 6;

    int at(int x, int y, int z);

    static boolean open(int c) { return c == AIR || c == WATER; }

    static boolean solid(int c) { return c == STONE || c == DEEPSLATE || c == OTHER; }
}
