package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.dungeon.MawShapes.Kind;
import com.newuniverse.nusmp.dungeon.MawShapes.Style;

import java.util.Locale;
import java.util.Set;

/**
 * Which manifestation a Maw takes and how worn its stone is: a stable hash of the place and the biome (docs/living_dungeon.md section 2).
 * The biome is read as a family from the words of its name, so modded biomes get a sensible look too. Pure.
 */
public final class MawStyle {
    private MawStyle() {}

    /** What a biome is like, for the look of a Maw: swamps are mossy, deserts cracked, mountains favour towers, deserts and tundra gates. */
    public enum Family { PLAIN, FOREST, WET, COLD, ARID, HIGH }

    private static final Set<String> WET = Set.of("swamp", "mangrove", "jungle", "bamboo", "lush", "bog", "marsh", "mushroom"),
            COLD = Set.of("snow", "snowy", "frozen", "ice", "icy", "cold", "glacier", "tundra", "frost", "frosty"),
            ARID = Set.of("desert", "badlands", "mesa", "savanna", "eroded", "dry", "wasteland", "dunes"),
            FOREST = Set.of("forest", "taiga", "woods", "woodland", "birch", "cherry"),
            HIGH = Set.of("hills", "mountain", "mountains", "peaks", "peak", "slopes", "windswept", "stony", "jagged", "highland", "highlands", "cliffs");

    /** The family of a biome from the path of its id ("dark_forest", "snowy_taiga", "windswept_savanna"); the first match wins. */
    public static Family family(String biomePath) {
        if (biomePath == null) return Family.PLAIN;
        String lower = biomePath.toLowerCase(Locale.ROOT);
        if (lower.equals("grove")) return Family.COLD;                                  // the snowy grove, not the cherry one
        boolean wet = false, cold = false, arid = false, forest = false, high = false;
        for (String word : lower.split("[^a-z0-9]+")) {
            wet |= WET.contains(word);
            cold |= COLD.contains(word);
            arid |= ARID.contains(word);
            forest |= FOREST.contains(word);
            high |= HIGH.contains(word);
        }
        return wet ? Family.WET : cold ? Family.COLD : arid ? Family.ARID : forest ? Family.FOREST : high ? Family.HIGH : Family.PLAIN;
    }

    /** How worn the stone is in this family. */
    public static Style styleOf(Family f) {
        return switch (f) {
            case WET -> new Style(55, 10);
            case FOREST -> new Style(30, 15);
            case PLAIN -> MawShapes.PLAIN;
            case COLD -> new Style(5, 30);
            case ARID -> new Style(3, 35);
            case HIGH -> new Style(12, 28);
        };
    }

    /** The odds in percent of a tower, ruins and a gate (they add up to 100). */
    public static int[] weights(Family f) {
        return switch (f) {
            case PLAIN -> new int[]{45, 30, 25};
            case FOREST -> new int[]{30, 45, 25};
            case WET -> new int[]{20, 55, 25};
            case COLD -> new int[]{35, 25, 40};
            case ARID -> new int[]{25, 20, 55};
            case HIGH -> new int[]{50, 20, 30};
        };
    }

    /** A stable hash of the place and the biome; the same Maw spot always gives the same number. */
    public static long seed(int x, int z, String biomePath) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ (biomePath == null ? 0 : biomePath.hashCode()) * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return h;
    }

    /** The manifestation the hash picks, by the odds of the family (never the hole: that is only the fallback). */
    public static Kind choose(long seed, Family f) {
        int roll = (int) ((seed >>> 33) % 100), tower = weights(f)[0], ruins = weights(f)[1];
        return roll < tower ? Kind.TOWER : roll < tower + ruins ? Kind.RUINS : Kind.GATE;
    }

    /** Which way it faces, in quarter turns (0..3). */
    public static int rotation(long seed) { return (int) ((seed >>> 17) & 3L); }
}
