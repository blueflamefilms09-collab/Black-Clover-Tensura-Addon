package com.newuniverse.nusmp.perf;

import net.minecraft.resources.ResourceLocation;

/** Preview stub of perf.RlCache (the real one keeps a bounded cache; the preview just builds the location). */
public final class RlCache {
    private RlCache() {}
    public static ResourceLocation mod(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }
    public static ResourceLocation modTexture(String name) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/" + name + ".png"); }
    public static ResourceLocation fxTexture(String name) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/fx/" + name + ".png"); }
}
