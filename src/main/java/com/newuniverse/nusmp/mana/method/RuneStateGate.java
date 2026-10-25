package com.newuniverse.nusmp.mana.method;

/**
 * 3.8.0: what the CLIENT knows of the TIMED rune state: only whether it is on (the server decides everything; this is for the grimoire's menu, which lists the Mana Method pages only
 * while the state is ON). The same pattern as {@code ManaZoneClientGate}: no Minecraft types, one volatile number, no per-frame work (it is read when a menu asks whether a page is
 * usable). The expiry uses the machine's own clock so a lost "off" packet cannot leave the pages listed for ever.
 *
 * <p>On a server nothing ever writes it (a server only answers for a {@code ServerPlayer} through {@link RuneState}); on the integrated server it holds the local player's own state.
 */
public final class RuneStateGate {
    private RuneStateGate() {}

    private static volatile long untilNanos;

    /** From the server: {@code secondsLeft} of the ON state remaining, or 0 / false for off. */
    public static void set(boolean on, int secondsLeft) {
        untilNanos = on && secondsLeft > 0 ? System.nanoTime() + 1_000_000_000L * Math.min(secondsLeft, RuneAccess.MAX_SECONDS) : 0L;
    }

    public static boolean on() { return untilNanos != 0L && System.nanoTime() < untilNanos; }

    public static void clear() { untilNanos = 0L; }
}
