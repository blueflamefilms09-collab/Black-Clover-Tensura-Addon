package com.newuniverse.nusmp.relics.socket;

/**
 * The pure half of {@link RelicLook}: from a tier and the first bound socket id, the element key and the colour a renderer should use. The colour is
 * the bound socket's when there is one, else the grade's own (passed in as {@code gradeRgb}). Pure.
 */
public final class RelicLookRules {
    private RelicLookRules() {}

    /** What a renderer needs: the effective tier (1 to 7), the element key of the first socket ("" if none) and a 0xRRGGBB colour. */
    public record Evolved(int tier, String socketElement, int rgb) {}

    public static final Evolved PLAIN = new Evolved(1, "", 0xFFFFFF);

    public static Evolved evolved(int tier, String firstSocketId, int gradeRgb) {
        int t = Math.max(1, Math.min(7, tier));
        SocketDef d = SocketCatalog.byId(firstSocketId);
        if (d == null) return new Evolved(t, "", gradeRgb & 0xFFFFFF);
        return new Evolved(t, d.element().key(), d.rgb() & 0xFFFFFF);
    }

    /** Does a stack of this tier get the more elaborate look (the "_genesis" mesh or the stronger glow)? Legend and above. */
    public static boolean elaborate(int tier) { return tier >= SocketRules.LEGEND_TIER; }
}
