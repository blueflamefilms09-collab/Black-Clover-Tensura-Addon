package com.newuniverse.nusmp.compat.create;

/**
 * Why: the three ways the dungeon already starts a trap, reused here (a trigger already used by the existing traps). A pressure plate (the mechanics library's
 * {@code plate} prop), a tripwire between two hooks, or proximity (the mechanics library's {@code proximity} trigger). The disarm is a lever in every case.
 */
public enum TriggerKind {
    PLATE("plate"), TRIPWIRE("tripwire"), PROXIMITY("proximity");

    private final String id;

    TriggerKind(String id) { this.id = id; }

    public String id() { return id; }

    public static TriggerKind byId(String id) {
        for (TriggerKind k : values()) if (k.id.equalsIgnoreCase(id)) return k;
        return PLATE;
    }
}
