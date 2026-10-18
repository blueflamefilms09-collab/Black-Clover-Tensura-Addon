package com.newuniverse.nusmp.elite.nations.core;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Why: a nation's custom roles are sets of these permissions (owner's inventory: "role permissions (custom roles)"). One bit each so a role is a single int on disk and on the wire.
 * New permissions may only be appended at the end (the bit position is the saved value). Pure, no Minecraft types.
 */
public enum Permission {
    INVITE, KICK, TREASURY, WAR, CONTRACTS, BUILD, DIPLOMACY, ROLES;

    public int bit() { return 1 << ordinal(); }

    public static final int ALL_MASK = (1 << values().length) - 1;

    public static int mask(Set<Permission> set) {
        int m = 0;
        if (set != null) for (Permission p : set) m |= p.bit();
        return m;
    }

    public static Set<Permission> fromMask(int mask) {
        Set<Permission> out = EnumSet.noneOf(Permission.class);
        for (Permission p : values()) if ((mask & p.bit()) != 0) out.add(p);
        return out;
    }

    /** Lenient parse ("treasury", "TREASURY"); null when unknown. */
    public static Permission parse(String s) {
        if (s == null) return null;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
}
