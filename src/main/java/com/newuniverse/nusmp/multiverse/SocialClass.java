package com.newuniverse.nusmp.multiverse;

/** Flavour only: never blocks the ceremony or joining the Magic Knights. */
public enum SocialClass {
    ROYALTY("Royalty"), NOBLE("Noble"), COMMONER("Commoner"), PEASANT("Peasant");

    public final String displayName;
    SocialClass(String displayName) { this.displayName = displayName; }

    public static SocialClass byName(String n) {
        for (SocialClass c : values()) if (c.name().equalsIgnoreCase(n)) return c;
        return null;
    }
}
