package com.newuniverse.nusmp.shadow;

/**
 * Why: Shadow Travel must decide "is this spot really in shadow" the same way everywhere (the caster's own spot, the aimed spot and every
 * candidate of the search), and that decision has to be testable without a world. This class holds only the rule; the world lookups
 * (block light, sky light, the sky darkening of the time of day and weather) live in {@link ShadowWorld}.
 *
 * <p>Formula. For one cell: {@code effectiveSky = max(0, rawSky - skyDarken)} where {@code rawSky} is the sky light of the cell (15 under the open
 * sky, falling by one a block under cover) and {@code skyDarken} is the level's own sky darkening (0 on a clear day, up to 11 at night, in between
 * at dusk, and larger when it rains or storms), exactly the value Minecraft subtracts when it asks how bright a cell is. The brightness of the cell is
 * {@code max(blockLight, effectiveSky)}. A spot is shadow when that brightness is at most {@code maxLight} at the feet cell AND at the head cell.
 * With the default {@code maxLight = 3} this is "light level below 4". Dimensions without sky light (Nether, End) have {@code rawSky = 0}, so only
 * block light counts there.
 */
public final class ShadowLight {
    private ShadowLight() {}

    /** The brightest a cell may be and still count as shadow when the config says nothing: light level 3, i.e. "below 4". */
    public static final int DEFAULT_MAX_LIGHT = 3;
    /** The config range of the threshold: 0 (pitch dark only) to 7. */
    public static final int MIN_MAX_LIGHT = 0, MAX_MAX_LIGHT = 7;

    /** Sky light that is left after the time of day and weather have darkened it. */
    public static int effectiveSky(int rawSky, int skyDarken) {
        return Math.max(0, clampLight(rawSky) - Math.max(0, skyDarken));
    }

    /** The brightness of one cell: the brighter of its block light and its darkened sky light. */
    public static int brightness(int blockLight, int rawSky, int skyDarken) {
        return Math.max(clampLight(blockLight), effectiveSky(rawSky, skyDarken));
    }

    /** Is this one cell dark enough? */
    public static boolean isShadow(int blockLight, int rawSky, int skyDarken, int maxLight) {
        return brightness(blockLight, rawSky, skyDarken) <= clampMax(maxLight);
    }

    /** Is a standing spot shadow? Both the feet cell and the head cell must be dark enough. */
    public static boolean spotInShadow(int blockFeet, int skyFeet, int blockHead, int skyHead, int skyDarken, int maxLight) {
        return isShadow(blockFeet, skyFeet, skyDarken, maxLight) && isShadow(blockHead, skyHead, skyDarken, maxLight);
    }

    /** The configured threshold, forced into its legal range (a hand-edited config can not break the rule). */
    public static int clampMax(int maxLight) {
        return Math.max(MIN_MAX_LIGHT, Math.min(MAX_MAX_LIGHT, maxLight));
    }

    private static int clampLight(int v) {
        return Math.max(0, Math.min(15, v));
    }
}
