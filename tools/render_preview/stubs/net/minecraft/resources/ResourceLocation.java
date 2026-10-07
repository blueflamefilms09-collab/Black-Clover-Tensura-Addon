package net.minecraft.resources;

import java.util.function.UnaryOperator;

/**
 * Preview stub of Minecraft 1.21.1's ResourceLocation: immutable namespace + path, built only through the factories (the constructor is
 * private in 1.21, like here). The preview maps "nusmp:textures/aura/x.png" to src/main/resources/assets/nusmp/textures/aura/x.png.
 */
public final class ResourceLocation implements Comparable<ResourceLocation> {
    public static final char NAMESPACE_SEPARATOR = ':';
    public static final String DEFAULT_NAMESPACE = "minecraft";
    public static final String REALMS_NAMESPACE = "realms";

    private final String namespace;
    private final String path;

    private ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) { return new ResourceLocation(namespace, path); }

    public static ResourceLocation withDefaultNamespace(String path) { return new ResourceLocation(DEFAULT_NAMESPACE, path); }

    public static ResourceLocation parse(String location) {
        int i = location.indexOf(NAMESPACE_SEPARATOR);
        return i < 0 ? new ResourceLocation(DEFAULT_NAMESPACE, location) : new ResourceLocation(i == 0 ? DEFAULT_NAMESPACE : location.substring(0, i), location.substring(i + 1));
    }

    public static ResourceLocation tryParse(String location) {
        try { return parse(location); } catch (RuntimeException e) { return null; }
    }

    public static ResourceLocation tryBuild(String namespace, String path) { return new ResourceLocation(namespace, path); }

    public String getPath() { return path; }

    public String getNamespace() { return namespace; }

    public ResourceLocation withPath(String path) { return new ResourceLocation(namespace, path); }

    public ResourceLocation withPath(UnaryOperator<String> op) { return withPath(op.apply(path)); }

    public ResourceLocation withPrefix(String prefix) { return withPath(prefix + path); }

    public ResourceLocation withSuffix(String suffix) { return withPath(path + suffix); }

    public String toDebugFileName() { return toString().replace('/', '_').replace(':', '_'); }

    public String toLanguageKey() { return namespace + "." + path; }

    public String toLanguageKey(String type) { return type + "." + namespace + "." + path; }

    @Override public String toString() { return namespace + ":" + path; }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof ResourceLocation r && namespace.equals(r.namespace) && path.equals(r.path));
    }

    @Override public int hashCode() { return 31 * namespace.hashCode() + path.hashCode(); }

    @Override public int compareTo(ResourceLocation o) {
        int c = path.compareTo(o.path);
        return c != 0 ? c : namespace.compareTo(o.namespace);
    }
}
