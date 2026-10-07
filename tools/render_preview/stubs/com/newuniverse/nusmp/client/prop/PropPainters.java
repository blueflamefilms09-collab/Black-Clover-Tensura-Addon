package com.newuniverse.nusmp.client.prop;

import com.newuniverse.nusmp.prop.PropKind;

import java.util.EnumMap;

/** Preview stub of PropPainters: the painter registered for each PropKind (the real attribute painters call register() unchanged). */
public final class PropPainters {
    private PropPainters() {}

    private static final EnumMap<PropKind, PropPainter> PAINTERS = new EnumMap<>(PropKind.class);

    public static void register(PropKind k, PropPainter p) { PAINTERS.put(k, p); }
    public static PropPainter get(PropKind k) { return PAINTERS.get(k); }
    public static void init() {}

    /** Preview helpers (not in the game). */
    public static boolean previewHas(PropKind k) { return PAINTERS.containsKey(k); }
    public static void previewClear() { PAINTERS.clear(); }
}
