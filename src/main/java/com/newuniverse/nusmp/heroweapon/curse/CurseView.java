package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;

/**
 * Why: the core's HUD, the weapon's look and the curse overlay all ask the CLIENT whether the local player's curse form is on, how corrupted they are and whether the
 * weapon holds them, and the client does not have the server's bond record (the bond travels as the core's own packet, which does not carry the curse). The curse
 * therefore sends its own small packet and the client keeps the last one here: a handful of static fields, written only by the packet receiver on the client thread
 * and read by {@link HeroCurse} (for a client-side player), the overlay and the key handler. It lives in the common package, with no Minecraft types, so
 * {@code HeroCurse} can read it without ever loading a client class; a dedicated server never writes it. Times arrive as "ticks left" and are turned into a client
 * game time when the packet is applied.
 */
public final class CurseView {
    private CurseView() {}

    private static volatile int despair, corruption, offeredWire, activeWire, series;
    private static volatile long overrunUntil, offerUntil;

    /** Applies one sync packet; {@code clientNow} is the client's game time now. */
    public static void apply(int despair, int corruption, int offeredWire, int activeWire, int overrunLeft, int offerLeft, int series, long clientNow) {
        CurseView.despair = CurseWire.clamp(despair, 0, CurseTuning.MAX);
        CurseView.corruption = CurseWire.clamp(corruption, 0, CurseTuning.MAX);
        CurseView.offeredWire = CurseWire.fromWire(offeredWire) == null ? 0 : offeredWire;
        CurseView.activeWire = CurseWire.fromWire(activeWire) == null ? 0 : activeWire;
        CurseView.overrunUntil = overrunLeft > 0 ? clientNow + overrunLeft : 0L;
        CurseView.offerUntil = offerLeft > 0 && CurseView.offeredWire != 0 ? clientNow + offerLeft : 0L;
        CurseView.series = series & ((1 << Sin.values().length) - 1);
    }

    /** Forgets everything (the player left the world or the packet said the weapon is gone). */
    public static void reset() { apply(0, 0, 0, 0, 0, 0, 0, 0L); }

    public static int despair() { return despair; }

    public static int corruption() { return corruption; }

    /** The sin of the curse form the local player wears, or null. */
    public static Sin active() { return CurseWire.fromWire(activeWire); }

    /** The sin of the open offer, or null. */
    public static Sin offered() { return CurseWire.fromWire(offeredWire); }

    public static boolean hasSeries(Sin sin) { return CurseWire.hasBit(series, sin); }

    public static int seriesMask() { return series; }

    /** True while the weapon holds the local player. */
    public static boolean locked(long clientNow) { return clientNow < overrunUntil; }

    public static int overrunTicksLeft(long clientNow) { return (int) Math.max(0L, overrunUntil - clientNow); }

    /** True while an offer is open. */
    public static boolean offerOpen(long clientNow) { return offeredWire != 0 && clientNow < offerUntil; }

    public static int offerTicksLeft(long clientNow) { return offerOpen(clientNow) ? (int) (offerUntil - clientNow) : 0; }

    /** True when there is anything for the overlay to show. */
    public static boolean anything(long clientNow) { return corruption > 0 || despair > 0 || offerOpen(clientNow) || locked(clientNow) || activeWire != 0; }
}
