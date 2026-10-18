package com.newuniverse.nusmp.elite.skills;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Why: a Gaia bloom grows a small vein of ore in the stone inside the domain. What ore, how big, and in what shape are decided here without Minecraft types, from a seed, so the same
 * seed always grows the same vein (testable) and the weights can be read in one table: common ores always, rarer ores only once the Ultimate is mastered, diamond very rarely, and the
 * deepslate variant under y 0. The world part (finding stone to replace, placing the blocks) is {@code GaiaWorld}; this class never touches the world.
 */
public final class VeinBloom {
    private VeinBloom() {}

    /** An ore with its block id and the deepslate variant's id. */
    public enum Ore {
        COAL("minecraft:coal_ore", "minecraft:deepslate_coal_ore", 40, 0),
        COPPER("minecraft:copper_ore", "minecraft:deepslate_copper_ore", 25, 0),
        IRON("minecraft:iron_ore", "minecraft:deepslate_iron_ore", 25, 0),
        LAPIS("minecraft:lapis_ore", "minecraft:deepslate_lapis_ore", 5, 0),
        REDSTONE("minecraft:redstone_ore", "minecraft:deepslate_redstone_ore", 5, 0),
        GOLD("minecraft:gold_ore", "minecraft:deepslate_gold_ore", 8, 1),
        DIAMOND("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", 2, 1);

        public final String block, deepBlock;
        public final int weight, minLevel;

        Ore(String block, String deepBlock, int weight, int minLevel) {
            this.block = block;
            this.deepBlock = deepBlock;
            this.weight = weight;
            this.minLevel = minLevel;
        }

        public String blockFor(boolean deep) { return deep && deepBlock != null ? deepBlock : block; }
    }

    private static double clampRoll(double roll) { return Math.min(Math.max(roll, 0.0), 0.999999); }

    /** Level 0 = the Ultimate as it is, 1 = the Ultimate mastered. {@code roll} in [0,1). */
    public static Ore pick(double roll, int level) {
        int total = 0;
        for (Ore o : Ore.values()) if (o.minLevel <= level) total += o.weight;
        double r = clampRoll(roll) * total;
        for (Ore o : Ore.values()) {
            if (o.minLevel > level) continue;
            r -= o.weight;
            if (r < 0) return o;
        }
        return Ore.COAL;
    }

    /** Blocks in a vein: 3..6, or 5..9 mastered. {@code roll} in [0,1). */
    public static int size(double roll, int level) {
        int lo = level >= 1 ? 5 : 3, hi = level >= 1 ? 9 : 6;
        return lo + (int) Math.min(hi - lo, Math.floor(clampRoll(roll) * (hi - lo + 1)));
    }

    /** True if blocks at this height use the deepslate variant. */
    public static boolean deep(int y) { return y < 0; }

    /** A connected blob of {@code size} distinct offsets starting at (0,0,0), grown by a seeded random walk. */
    public static List<int[]> shape(long seed, int size) {
        Random rnd = new Random(seed);
        Set<Long> seen = new LinkedHashSet<>();
        List<int[]> out = new ArrayList<>();
        out.add(new int[]{0, 0, 0});
        seen.add(key(0, 0, 0));
        int guard = Math.max(1, size) * 40;
        while (out.size() < size && guard-- > 0) {
            int[] base = out.get(rnd.nextInt(out.size()));
            int axis = rnd.nextInt(3), dir = rnd.nextBoolean() ? 1 : -1;
            int[] next = base.clone();
            next[axis] += dir;
            if (seen.add(key(next[0], next[1], next[2]))) out.add(next);
        }
        return out;
    }

    private static long key(int x, int y, int z) { return ((long) (x + 64) << 40) | ((long) (y + 64) << 20) | (z + 64); }
}
