package com.newuniverse.nusmp.mana.zone;

/**
 * What the client knows of the gate: only whether it is open (the server decides everything; this is for menus: a grimoire lists the Mana Zone pages as
 * "?" until it is open). No Minecraft types, so both sides may load it.
 */
public final class ManaZoneClientGate {
    private ManaZoneClientGate() {}

    private static volatile boolean open;
    private static volatile int attune, needed;

    public static void set(boolean isOpen, int attuneSeconds, int attuneNeeded) {
        open = isOpen;
        attune = attuneSeconds;
        needed = attuneNeeded;
    }

    public static boolean open() { return open; }

    public static int attuneSeconds() { return attune; }

    public static int attuneNeeded() { return needed; }

    public static void clear() { set(false, 0, 0); }
}
