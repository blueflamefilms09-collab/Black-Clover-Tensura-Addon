package com.newuniverse.nusmp.client.prop;

import com.newuniverse.nusmp.prop.PropKind;

import java.util.EnumMap;

/** 0.53: the painter registered for each {@link PropKind} (each attribute's {@code client.prop.<Name>PropPainter.register()}). */
public final class PropPainters {
    private PropPainters() {}

    private static final EnumMap<PropKind, PropPainter> PAINTERS = new EnumMap<>(PropKind.class);

    public static void register(PropKind k, PropPainter p) { PAINTERS.put(k, p); }
    public static PropPainter get(PropKind k) { return PAINTERS.get(k); }
    public static void init() { PropPainterRegistry.registerAll(); }
}
