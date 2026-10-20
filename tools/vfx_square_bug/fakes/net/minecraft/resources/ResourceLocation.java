package net.minecraft.resources;

public final class ResourceLocation {
    private final String ns, path;
    private ResourceLocation(String ns, String path) { this.ns = ns; this.path = path; }
    public static ResourceLocation fromNamespaceAndPath(String ns, String path) { return new ResourceLocation(ns, path); }
    public String getPath() { return path; }
    @Override public boolean equals(Object o) { return o instanceof ResourceLocation r && r.ns.equals(ns) && r.path.equals(path); }
    @Override public int hashCode() { return 31 * ns.hashCode() + path.hashCode(); }
    @Override public String toString() { return ns + ":" + path; }
}
