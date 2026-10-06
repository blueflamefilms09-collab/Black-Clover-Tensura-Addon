package net.minecraft.resources;

public record ResourceLocation(String namespace, String path) {
    public static ResourceLocation fromNamespaceAndPath(String ns, String p) { return new ResourceLocation(ns, p); }
    public String getPath() { return path; }
    public String getNamespace() { return namespace; }
    @Override public String toString() { return namespace + ":" + path; }
}
