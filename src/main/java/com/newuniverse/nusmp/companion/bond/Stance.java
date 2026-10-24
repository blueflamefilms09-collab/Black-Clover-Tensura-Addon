package com.newuniverse.nusmp.companion.bond;

import java.util.Locale;

/**
 * Why: how a fighting companion behaves when something hostile is near: GUARD (the default: she answers whoever hurts the player she walks with, or comes close to them), AGGRESSIVE (she goes for anything
 * hostile in sight) or PASSIVE (she never starts a fight). Saved by name in the bond data; constants are only ever appended. Not the Avatar's {@code avatar.Stance} (a movement order). Pure.
 */
public enum Stance {
    GUARD, AGGRESSIVE, PASSIVE;

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The stance of a saved name (any case); GUARD for anything unknown. */
    public static Stance byName(String name) {
        if (name != null) for (Stance s : values()) if (s.name().equalsIgnoreCase(name)) return s;
        return GUARD;
    }

    public static Stance byOrdinal(int i) { return i >= 0 && i < values().length ? values()[i] : GUARD; }
}
