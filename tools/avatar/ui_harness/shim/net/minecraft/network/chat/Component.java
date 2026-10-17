package net.minecraft.network.chat;

/** Why: harness shim: text that is resolved when it is made, from the real en_us.json plus the fragment (system property harness.lang). */
public interface Component {
    String getString();

    MutableComponent copy();

    Style getStyle();

    static MutableComponent literal(String s) { return new MutableComponent(s == null ? "" : s); }

    static MutableComponent empty() { return new MutableComponent(""); }

    static MutableComponent translatable(String key, Object... args) { return new MutableComponent(Lang.format(Lang.get(key, key), args)); }

    static MutableComponent translatableWithFallback(String key, String fallback, Object... args) { return new MutableComponent(Lang.format(Lang.get(key, fallback), args)); }
}
