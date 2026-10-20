package com.newuniverse.nusmp.vfx;

/**
 * Why (docs/vfx_square_bug.md): the decisions of the square-bug fix, kept out of the renderer so they can be unit-tested. Two questions:
 * <ol>
 *   <li>{@link #choose}: which additive render type draws a sprite or a glow map of an aura. Vanilla's eyes type ignores the texture's alpha; the alpha-weighted one ({@code SRC_ALPHA, ONE}) does not.
 *       A texture that carries a colour under its transparent texels (or one that cannot be read) gets the alpha-weighted one; a texture that is black under its transparent texels is drawn exactly as
 *       before, unless the player forced the safe path ({@code vfx.safeBlend}).</li>
 *   <li>{@link #replaceWithSoftCircle}: whether a texture of the VFX vertex buffer is swapped for the generated soft circle: only when it does not exist, or (additive batches only) when it is a flat
 *       sprite that lost its alpha. Never a Time Magic texture.</li>
 * </ol>
 */
public final class AdditivePolicy {
    private AdditivePolicy() {}

    public enum Blend {
        /** {@code RenderType.eyes}: the look the aura painters always had. */
        VANILLA_EYES,
        /** Eyes shader, blend {@code SRC_ALPHA, ONE}: the transparent texels add nothing and the vertex alpha fades the sprite. */
        ALPHA_ADD
    }

    /** @param kind the audit of the texture, or null when it could not be read (missing, broken) */
    public static Blend choose(TextureAlpha.Kind kind, boolean forceSafe) {
        if (forceSafe || kind == null) return Blend.ALPHA_ADD;
        return kind == TextureAlpha.Kind.COLOUR_UNDER_ALPHA ? Blend.ALPHA_ADD : Blend.VANILLA_EYES;
    }

    /** The names of flat sprites (a ring, a circle, a glow...): the only opaque textures that may be swapped without {@code vfx.safeBlend}. */
    public static boolean looksLikeFlatSprite(String path) {
        if (path == null) return false;
        String n = path.toLowerCase(java.util.Locale.ROOT);
        for (String k : new String[]{"ring", "circle", "halo", "glow", "ripple", "sigil", "puff", "bloom", "flare", "mote", "spark"}) if (n.contains(k)) return true;
        return false;
    }

    /** True for the Time Magic textures, which the fix never touches. */
    public static boolean isTimeMagic(String path) {
        if (path == null) return false;
        int slash = path.lastIndexOf('/');
        return path.substring(slash + 1).startsWith("time_");
    }

    /**
     * @param kind     the audit of the texture, null when it does not exist or cannot be read
     * @param additive whether the batch is drawn with an additive blend (ADD, FIRE, WIND)
     * @param path     the texture's path (for the name test and the Time Magic exemption)
     */
    public static boolean replaceWithSoftCircle(TextureAlpha.Kind kind, boolean additive, String path, boolean forceSafe) {
        if (isTimeMagic(path)) return false;
        if (kind == null) return true;
        if (!additive || kind != TextureAlpha.Kind.OPAQUE_BRIGHT_EDGE) return false;
        return forceSafe || looksLikeFlatSprite(path);
    }
}
