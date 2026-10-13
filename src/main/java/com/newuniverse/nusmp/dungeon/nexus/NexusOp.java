package com.newuniverse.nusmp.dungeon.nexus;

/**
 * One placement of a Nexus plan: a box (inclusive corners, x1 <= x2 and so on) of one block, named by its registry id and an optional
 * property string ("facing=north,half=top"). Nothing here touches Minecraft, so a whole plan can be built and checked without the game;
 * the glue ({@link NexusStates}, {@link NexusSink}) turns the names into block states at placement time. Blocks that do not exist
 * (a mod that is not installed) are replaced by stone bricks and logged once.
 */
public record NexusOp(Kind kind, String block, String props, int x1, int y1, int z1, int x2, int y2, int z2) {
    /**
     * CARVE clears everything in its box (bedrock stays), BOX fills it with its block, BLOCK is a box of one, FILL_AIR fills only the
     * cells that are air now (for details that must not eat the structure).
     */
    public enum Kind { CARVE, BOX, BLOCK, FILL_AIR }

    public static final String AIR = "minecraft:air";

    public NexusOp {
        if (kind == null || block == null || props == null) throw new IllegalArgumentException("an op needs a kind, a block and properties (use \"\" for none)");
        if (x1 > x2 || y1 > y2 || z1 > z2) throw new IllegalArgumentException("op corners out of order");
    }

    public int sx() { return x2 - x1 + 1; }

    public int sy() { return y2 - y1 + 1; }

    public int sz() { return z2 - z1 + 1; }

    public long volume() { return (long) sx() * sy() * sz(); }
}
