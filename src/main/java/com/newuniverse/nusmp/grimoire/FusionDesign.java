package com.newuniverse.nusmp.grimoire;

/**
 * 1.16.0: the generated look of a two-attribute (fused) grimoire. The book's colours come from {@link BookLook}; its geometry gets a
 * filigree that is generated from the pair's names, so every pair of attributes has its own cover and two mages with the same pair
 * have the same one: a ring of gems around the medallion (alternating the two parents' metals), corner brackets, and gems down the
 * spine. Pure: no game classes (the plan turns this into quads).
 */
public final class FusionDesign {
    private FusionDesign() {}

    /**
     * @param style      0 = one ring of gems, 1 = ring and brackets with long arms, 2 = a double ring (alternate gems sit further out)
     * @param gems       gems in the ring (5..8)
     * @param ring       ring radius (model units)
     * @param phase      turn of the ring (radians)
     * @param bracketLen arm length of the corner brackets (1.4..2.6)
     * @param spineGems  gems down the spine (3..5)
     */
    public record Spec(int style, int gems, float ring, float phase, float bracketLen, int spineGems) {}

    /** The two parents of a fused grimoire ("FLAME:WATER"), or null for anything else. */
    public static String[] parents(String fusion) {
        if (fusion == null || fusion.isEmpty()) return null;
        String[] p = fusion.split(":");
        return p.length == 2 && !p[0].isEmpty() && !p[1].isEmpty() ? p : null;
    }

    /** The design of a fusion, or null for none. Stable: the same string gives the same design everywhere. */
    public static Spec of(String fusion) {
        if (parents(fusion) == null) return null;
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < fusion.length(); i++) { h ^= fusion.charAt(i); h *= 0x100000001b3L; }
        int a = (int) (h >>> 8 & 0xFFFF), b = (int) (h >>> 24 & 0xFFFF), c = (int) (h >>> 40 & 0xFFFF);
        int style = a % 3;
        int gems = 5 + b % 4;
        float ring = 3.5f + (c % 5) * 0.1f;
        float phase = (float) (((a >> 3) % 360) * Math.PI / 180.0);
        float bracket = 1.4f + ((b >> 2) % 7) * 0.2f;
        int spine = 3 + c % 3;
        return new Spec(style, gems, ring, phase, bracket, spine);
    }
}
