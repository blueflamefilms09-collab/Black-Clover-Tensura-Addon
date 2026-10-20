package com.newuniverse.nusmp.perf;

import net.minecraft.resources.ResourceLocation;

/**
 * Why: {@code ResourceLocation.fromNamespaceAndPath("nusmp", "textures/" + name + ".png")} in code that runs every frame or every tick is a string concatenation, a character validation, an
 * object and a later hash, each time, for a value that is the same every time. These two lookups build it once (a {@link BoundedCache}, so a key set that is not really finite cannot grow
 * without end) and return the same object ever after. They return exactly what the expressions they replace returned, so nothing that compares or hashes a location notices.
 */
public final class RlCache {
    private RlCache() {}

    private static final BoundedCache<String, ResourceLocation> MOD = new BoundedCache<>(4096);
    private static final BoundedCache<String, ResourceLocation> TEXTURE = new BoundedCache<>(4096);

    /** {@code nusmp:<path>}, built once per path. */
    public static ResourceLocation mod(String path) {
        return MOD.get(path, p -> ResourceLocation.fromNamespaceAndPath("nusmp", p));
    }

    /** {@code nusmp:textures/<name>.png}, built once per name. */
    public static ResourceLocation modTexture(String name) {
        return TEXTURE.get(name, n -> ResourceLocation.fromNamespaceAndPath("nusmp", "textures/" + n + ".png"));
    }

    private static final BoundedCache<String, ResourceLocation> FX_TEXTURE = new BoundedCache<>(1024);

    /** {@code nusmp:textures/fx/<name>.png} (the spec-driven effect pieces), built once per name. */
    public static ResourceLocation fxTexture(String name) {
        return FX_TEXTURE.get(name, n -> ResourceLocation.fromNamespaceAndPath("nusmp", "textures/fx/" + n + ".png"));
    }

    /** For the report and the self-test: how many locations are held and how many lookups were answered from the store. */
    public static String stats() {
        return "mod " + MOD.size() + " (" + MOD.hits() + " hits, " + MOD.misses() + " builds), texture " + TEXTURE.size() + " (" + TEXTURE.hits() + " hits, " + TEXTURE.misses() + " builds)";
    }

    public static void clear() {
        MOD.clear();
        TEXTURE.clear();
        FX_TEXTURE.clear();
    }
}
