package com.newuniverse.nusmp.grimoire;

/** The 3D strap / chain / lock assembled onto the fore-edge. APPEND ONLY. */
public enum ClaspType {
    OPEN("Open"), SINGLE_BUCKLE("Single Buckle"), DUAL_CHAINS("Dual Chains"), LOCK_AND_KEY("Lock and Key");

    public final String displayName;
    ClaspType(String displayName) { this.displayName = displayName; }

    private static final ClaspType[] VALUES = values();
    public static ClaspType byOrdinal(int i) { return VALUES[Math.floorMod(i, VALUES.length)]; }
}
