package net.minecraft.resources;

import java.util.Objects;

/** Why: harness shim. */
public final class ResourceLocation {
    private final String namespace, path;
    private ResourceLocation(String ns, String p) { namespace = ns; path = p; }
    public static ResourceLocation fromNamespaceAndPath(String ns, String path) { return new ResourceLocation(ns, path); }
    public static ResourceLocation tryParse(String s) {
        if (s == null || s.isEmpty()) return null;
        int i = s.indexOf(':');
        String ns = i < 0 ? "minecraft" : s.substring(0, i), p = i < 0 ? s : s.substring(i + 1);
        if (!ns.matches("[a-z0-9_.-]+") || !p.matches("[a-z0-9/._-]+")) return null;
        return new ResourceLocation(ns, p);
    }
    public String getNamespace() { return namespace; }
    public String getPath() { return path; }
    @Override public String toString() { return namespace + ":" + path; }
    @Override public boolean equals(Object o) { return o instanceof ResourceLocation r && r.namespace.equals(namespace) && r.path.equals(path); }
    @Override public int hashCode() { return Objects.hash(namespace, path); }
}
