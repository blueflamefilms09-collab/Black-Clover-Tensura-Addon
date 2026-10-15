package com.newuniverse.nusmp.labyrinth.gen;

/**
 * Why: the wood-family mapping of the palette processor as a PURE string function, so it is unit-tested without Minecraft on the class
 * path (PaletteSwapTest) and so the rule is readable in one place: a template's {@code minecraft:dark_oak_fence} becomes the palette wood's
 * {@code <namespace>:<stem>_fence}, where the stem is the palette's planks id without {@code _planks}. Suffix matching rather than a
 * table of vanilla woods is what makes modded woods work: {@code biomesoplenty:hellbark_planks} yields {@code biomesoplenty:hellbark_fence}
 * with no code change, and a family that lacks a shape (bamboo mosaic has stairs and slabs but no fence) is caught by the processor's
 * registry check, which then keeps the template block, as the contract says. The processor, not this class, decides WHICH blocks are
 * eligible (the {@code nusmp:labyrinth/swap_wood} tag); this only answers "what would it become".
 */
public final class PaletteSwap {
    private PaletteSwap() {}

    /** The wooden shapes a template may use, longest first so {@code _fence_gate} is never read as {@code _fence}. */
    public static final String[] WOOD_SUFFIXES = {
            "_fence_gate", "_pressure_plate", "_trapdoor", "_planks", "_stairs", "_button", "_fence", "_slab", "_door"
    };
    public static final String PLANKS = "_planks";

    /** The wood suffix an id ends with ({@code "minecraft:dark_oak_fence_gate" -> "_fence_gate"}), or null if it is not a wooden shape. */
    public static String suffix(String blockId) {
        if (blockId == null) return null;
        for (String s : WOOD_SUFFIXES) if (blockId.endsWith(s)) return s;
        return null;
    }

    /** The family stem of a palette wood entry: {@code "minecraft:spruce_planks" -> "minecraft:spruce"}; an id that is not planks is its own stem. */
    public static String stem(String woodId) {
        if (woodId == null) return null;
        return woodId.endsWith(PLANKS) ? woodId.substring(0, woodId.length() - PLANKS.length()) : woodId;
    }

    /**
     * The block id a template wood block maps to in the palette's family, or null when the template id has no wood suffix (then nothing is
     * swapped). Planks map to the palette's wood block itself; every other shape to {@code stem + suffix}. The result is a candidate: the
     * caller must check it is a registered block and otherwise keep the original.
     */
    public static String wood(String templateId, String woodId) {
        if (templateId == null || woodId == null || woodId.isEmpty()) return null;
        String suffix = suffix(templateId);
        if (suffix == null) return null;
        if (PLANKS.equals(suffix)) return woodId;
        return stem(woodId) + suffix;
    }
}
