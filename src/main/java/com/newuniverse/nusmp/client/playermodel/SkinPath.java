package com.newuniverse.nusmp.client.playermodel;

import java.util.Locale;

/**
 * Why: a look names its skin as a short id ({@code asta}), a path under the mod's assets, or (for an Avatar) a data id the client composes a texture for; and the glow map of a skin is always the
 * skin's name plus {@code _glow}. Turning those names into the one resource path, and refusing a name that could point outside the mod's texture folder (a bad save, a typo, a hand-edited
 * packet), is pure string work, so it lives here with no game class and is tested: every look of the table must resolve to a file that exists.
 */
public final class SkinPath {
    private SkinPath() {}

    /** The folder every kit skin lives in, under {@code assets/nusmp/}. */
    public static final String DIR = "textures/entity/playermodel/";
    public static final String NAMESPACE = "nusmp";
    /** The default skin every look falls back to. */
    public static final String DEFAULT_ID = "npc_default";

    /** A plain id: lower case letters, digits, underscore, dash; at most 64 characters, starting with a letter. */
    public static boolean validId(String id) {
        if (id == null || id.isEmpty() || id.length() > 64) return false;
        char c0 = id.charAt(0);
        if (!(c0 >= 'a' && c0 <= 'z')) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-';
            if (!ok) return false;
        }
        return true;
    }

    /** A path under the assets: segments of letters, digits, underscore, dash, dot; no empty segment and no {@code ..}; ends with {@code .png}. */
    public static boolean validPath(String path) {
        if (path == null || path.isEmpty() || path.length() > 160 || !path.endsWith(".png") || path.startsWith("/")) return false;
        for (String seg : path.split("/", -1)) {
            if (seg.isEmpty() || seg.equals(".") || seg.equals("..")) return false;
            for (int i = 0; i < seg.length(); i++) {
                char c = seg.charAt(i);
                boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.';
                if (!ok) return false;
            }
        }
        return true;
    }

    /**
     * The resource path (without namespace) of a skin name: a plain id becomes {@code textures/entity/playermodel/<id>.png}; a path that is already a valid {@code .png} path is kept;
     * anything else is "" (the caller falls back to the default skin).
     */
    public static String resolve(String name) {
        if (name == null) return "";
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (n.startsWith(NAMESPACE + ":")) n = n.substring(NAMESPACE.length() + 1);
        if (validId(n)) return DIR + n + ".png";
        return validPath(n) ? n : "";
    }

    /** The glow map of a skin path: {@code x.png} to {@code x_glow.png}; "" for an empty path. */
    public static String glowOf(String skinPath) {
        if (skinPath == null || !skinPath.endsWith(".png")) return "";
        return skinPath.substring(0, skinPath.length() - 4) + "_glow.png";
    }

    public static String defaultPath() { return DIR + DEFAULT_ID + ".png"; }

    /** The size of the vanilla player skin layout, and the largest square the kit accepts for a skin or glow map (sanity cap: 1024 is 4 MB of texture). */
    public static final int BASE_SIZE = 64, MAX_SIZE = 1024;

    /**
     * 3.38.0 (HD outfits): may a skin, or a glow map, have this size? The model's UVs are fractions of the 64x64 layout (the layer definition bakes u/64), so a skin may be any power-of-two
     * multiple of 64 with the SAME layout: 64 (every older character), 128 or 256 (every model pixel is then a 2x2 or 4x4 block of texels). It must be square.
     */
    public static boolean validSkinSize(int w, int h) {
        if (w != h || w < BASE_SIZE || w > MAX_SIZE || w % BASE_SIZE != 0) return false;
        int k = w / BASE_SIZE;
        return (k & (k - 1)) == 0;
    }

    /** How many texels one model pixel is wide for a skin of this width (1 for 64, 4 for 256); 1 for a size that is not valid. */
    public static int texelsPerPixel(int w) { return validSkinSize(w, w) ? w / BASE_SIZE : 1; }

    /** The path of a hair sheet: {@code textures/entity/playermodel/<texture>.png} (a style's {@code texture} field is {@code hair/<style>}). */
    public static String hairSheet(String texture) {
        return texture != null && texture.matches("[a-z0-9_/-]+") && !texture.contains("..") ? DIR + texture + ".png" : "";
    }

    /** The texture path of an extra: {@code textures/entity/<name>.png} for a plain name (letters, digits, underscore), "" otherwise. */
    public static String extraTexture(String name) {
        return name != null && name.matches("[a-z][a-z0-9_]{0,63}") ? "textures/entity/" + name + ".png" : "";
    }
}
