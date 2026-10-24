package com.newuniverse.nusmp.mana.method;

/**
 * The rune alphabet of the Mana Method (STAGED, pure): 32 hand-authored rune characters, each a few straight strokes on a 5x5 lattice (x to the right, y UP, both 0..4), in the angular
 * style of the owner's reference frame and of the Elder Futhark: vertical bars with hooks and branches, triangles, crosses, arrows, diamonds. A glyph is WRITTEN: the pen walks the
 * strokes one after the other (a pen lift is instant), so {@link #cumulative} gives the pen's path length after each segment, and the writing animation of {@link RuneGlyphLayout}
 * draws the segments up to the pen's position.
 *
 * <p>A glyph has at most {@value #MAX_SEGMENTS} segments (a test pins it: it bounds the strip quads of a rune). The glyphs are read in the glyph's own frame (right = +x, up = +y),
 * and the layout puts that frame so that it faces the caster, so no glyph is mirrored. Each spell has its own words: {@link #phrase} is a fixed run of six glyphs per element (and
 * a True Magic variant), so a Flame rune always reads like a flame rune.
 */
public final class RuneAlphabet {
    private RuneAlphabet() {}

    public static final int COUNT = 32, MAX_SEGMENTS = 6, LATTICE = 5;

    /** name, then the strokes "x,y-x,y-x,y;..." (a stroke is a polyline of lattice points). */
    private static final String[][] SPEC = {
            {"fehu", "1,0-1,4;1,3-3,4;1,2-3,3"},
            {"uruz", "1,0-1,4-3,3-3,0"},
            {"thurs", "1,0-1,4;1,3-3,2-1,1"},
            {"ansuz", "1,0-1,4-3,3;1,3-3,2"},
            {"raido", "1,0-1,4-3,3-1,2-3,0"},
            {"kenaz", "3,4-1,2-3,0"},
            {"gebo", "0,4-4,0;0,0-4,4"},
            {"wunjo", "1,0-1,4-3,3-1,2"},
            {"hagal", "1,0-1,4;3,0-3,4;1,3-3,1"},
            {"naud", "2,0-2,4;1,3-3,1"},
            {"isa", "2,0-2,4;1,4-3,4;1,0-3,0"},
            {"jera", "1,3-2,4-2,0-3,1"},
            {"eihwaz", "2,0-2,4-3,3;2,0-3,1"},
            {"perth", "1,0-1,4-3,3-3,1-1,0"},
            {"algiz", "2,0-2,4;2,2-0,4;2,2-4,4"},
            {"sowilo", "3,4-1,3-3,1-1,0"},
            {"tiwaz", "2,0-2,4;0,2-2,4-4,2"},
            {"berkana", "1,0-1,4-3,3-1,2-3,1-1,0"},
            {"ehwaz", "1,0-1,4;3,0-3,4;1,4-2,3-3,4"},
            {"mannaz", "1,0-1,4;3,0-3,4;1,4-3,2;3,4-1,2"},
            {"laguz", "1,0-1,4-3,3"},
            {"ingwaz", "2,4-0,2-2,0-4,2-2,4"},
            {"dagaz", "0,0-0,4-4,0-4,4-0,0"},
            {"othala", "2,4-0,2;2,4-4,2;0,2-0,0;4,2-4,0"},
            {"triangle", "0,0-4,0-2,4-0,0"},
            {"cross", "2,0-2,4;0,2-4,2"},
            {"arrow", "0,2-4,2;2,4-4,2-2,0"},
            {"lantern", "2,4-0,2-2,0-4,2-2,4;0,2-4,2"},
            {"gate", "1,0-1,4-3,4-3,0;2,4-2,2"},
            {"zigzag", "0,0-1,4-3,0-4,4"},
            {"trident", "2,0-2,4;0,4-0,2-2,1-4,2-4,4"},
            {"star", "2,0-2,4;0,1-4,3;0,3-4,1"},
    };

    private static final String[] NAME = new String[COUNT];
    /** Per glyph: x0, y0, x1, y1 of each segment in drawing order, flat. */
    private static final int[][] SEG = new int[COUNT][];
    /** Per glyph: the pen's path length after each segment (the last entry is the total). */
    private static final double[][] CUM = new double[COUNT][];

    static {
        for (int g = 0; g < COUNT; g++) {
            NAME[g] = SPEC[g][0];
            int[] flat = new int[MAX_SEGMENTS * 4 * 2];
            int n = 0;
            for (String stroke : SPEC[g][1].split(";")) {
                String[] pts = stroke.split("-");
                for (int i = 0; i + 1 < pts.length; i++) {
                    String[] a = pts[i].split(","), b = pts[i + 1].split(",");
                    if (n >= flat.length / 4) throw new IllegalStateException("glyph " + NAME[g] + " has too many segments");
                    flat[n * 4] = Integer.parseInt(a[0]);
                    flat[n * 4 + 1] = Integer.parseInt(a[1]);
                    flat[n * 4 + 2] = Integer.parseInt(b[0]);
                    flat[n * 4 + 3] = Integer.parseInt(b[1]);
                    n++;
                }
            }
            SEG[g] = java.util.Arrays.copyOf(flat, n * 4);
            CUM[g] = new double[n];
            double acc = 0;
            for (int s = 0; s < n; s++) {
                double dx = SEG[g][s * 4 + 2] - SEG[g][s * 4], dy = SEG[g][s * 4 + 3] - SEG[g][s * 4 + 1];
                acc += Math.sqrt(dx * dx + dy * dy);
                CUM[g][s] = acc;
            }
        }
    }

    public static String name(int g) { return NAME[Math.floorMod(g, COUNT)]; }

    public static int segments(int g) { return SEG[Math.floorMod(g, COUNT)].length / 4; }

    /** The lattice coordinate (x0, y0, x1 or y1: k = 0..3) of segment {@code s} of glyph {@code g}. */
    public static int coord(int g, int s, int k) { return SEG[Math.floorMod(g, COUNT)][s * 4 + k]; }

    /** The pen's path length after segment {@code s}; the last one is the length of the whole glyph. The array is shared: do not write to it. */
    public static double[] cumulative(int g) { return CUM[Math.floorMod(g, COUNT)]; }

    public static double length(int g) {
        double[] c = cumulative(g);
        return c[c.length - 1];
    }

    // ---------------------------------------------------------------- the words of each element
    private static final int[][] PHRASE = {
            {5, 15, 8, 0, 24, 14},          // FIRE: kenaz, sowilo, hagal, fehu, triangle, algiz
            {20, 11, 21, 3, 12, 29},        // WATER: laguz, jera, ingwaz, ansuz, eihwaz, zigzag
            {3, 16, 26, 29, 5, 19},         // WIND: ansuz, tiwaz, arrow, zigzag, kenaz, mannaz
            {10, 22, 23, 13, 25, 28},       // EARTH: isa, dagaz, othala, perth, cross, gate
            {15, 29, 14, 9, 31, 16},        // LIGHTNING: sowilo, zigzag, algiz, naud, star, tiwaz
            {12, 2, 17, 30, 4, 18},         // PLANT: eihwaz, thurs, berkana, trident, raido, ehwaz
    };
    /** True Magic overwrites the first and the fourth word of the element's phrase with the two glyphs only it writes (gebo, the crossed gate of the real element, and othala, the inheritance). */
    private static final int TRUE_FIRST = 6, TRUE_FOURTH = 23;
    public static final int PHRASE_LENGTH = 6;

    /** The glyph of word {@code i} of the element's phrase (deterministic): a fixed run of six that repeats. */
    public static int phrase(RuneElement e, boolean trueMagic, int i) {
        int k = Math.floorMod(i, PHRASE_LENGTH);
        if (trueMagic && k == 0) return TRUE_FIRST;
        if (trueMagic && k == 3) return TRUE_FOURTH;
        return PHRASE[e.ordinal()][k];
    }
}
