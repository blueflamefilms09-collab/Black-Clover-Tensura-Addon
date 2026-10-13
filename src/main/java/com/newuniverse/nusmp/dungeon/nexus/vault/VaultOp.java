package com.newuniverse.nusmp.dungeon.nexus.vault;

/**
 * One placement of a vault plan: a box (inclusive corners, x1 <= x2 and so on) of one palette entry. Nothing here touches Minecraft; the
 * glue (VaultSink) turns palette entries into block states at placement time.
 */
public record VaultOp(Kind kind, Pal pal, int x1, int y1, int z1, int x2, int y2, int z2) {
    /**
     * CARVE clears everything in its box to air (bedrock stays). SOFT places its palette entry only where the cell is not solid rock yet
     * (this closes the shell of a room round a cave without touching the rock that is already there). BOX always places it.
     */
    public enum Kind { CARVE, SOFT, BOX }

    /** What can be placed. ROCK is stone or deepslate to match the rock beside it; ILLUSION is the matching Illusion Stone or Illusion Deepslate. */
    public enum Pal {
        AIR, ROCK, ILLUSION, MOSS, MOSS_CARPET, SPORE_BLOSSOM, SOUL_LANTERN_HANGING, PLATE_IDLE, PLATE_LIT, PLATE_DONE, IRON_BARS,
        BLACKSTONE_BRICKS, POLISHED_BLACKSTONE, CRYING_OBSIDIAN, BLOOM_0, BLOOM_1, BLOOM_2, BLOOM_3, GATE_DOWN, GATE_UP
    }

    public VaultOp {
        if (kind == null || pal == null) throw new IllegalArgumentException("an op needs a kind and a palette entry");
        if (x1 > x2 || y1 > y2 || z1 > z2) throw new IllegalArgumentException("op corners out of order");
    }

    public int sx() { return x2 - x1 + 1; }

    public int sy() { return y2 - y1 + 1; }

    public int sz() { return z2 - z1 + 1; }

    public long volume() { return (long) sx() * sy() * sz(); }
}
