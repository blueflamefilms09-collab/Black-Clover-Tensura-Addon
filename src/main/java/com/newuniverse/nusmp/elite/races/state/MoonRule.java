package com.newuniverse.nusmp.elite.races.state;

import java.util.Locale;

/** Why: the Great Ape needs the moon. ANY = no sky condition, NIGHT = it is night, FULL_MOON = it is night and the moon is full. */
public enum MoonRule {
    ANY, NIGHT, FULL_MOON;

    public static MoonRule byId(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase(Locale.ROOT);
        for (MoonRule m : values()) if (m.name().toLowerCase(Locale.ROOT).equals(t)) return m;
        return null;
    }
}
