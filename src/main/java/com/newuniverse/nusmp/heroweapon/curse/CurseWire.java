package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;

import java.util.Collection;

/**
 * Why: the sync packet of the curse carries sins as small numbers (one byte each) and the unlocked series as a bit mask, and the server that writes them and the
 * client that reads them must agree on the encoding to the last bit. Both sides use these four functions, so the agreement is one piece of code with a test, and
 * the reader clamps whatever arrives (a damaged or hostile packet becomes "no sin", never an exception). The wire number of a sin is its enum position plus one,
 * 0 meaning none, so appending a sin to the enum never changes an old number. No Minecraft types.
 */
public final class CurseWire {
    private CurseWire() {}

    /** The wire number of a sin: its position in the enum plus one; 0 for none. */
    public static int toWire(Sin sin) { return sin == null ? 0 : sin.ordinal() + 1; }

    /** The sin of a wire number, or null for 0 or a number no sin has. */
    public static Sin fromWire(int wire) {
        Sin[] all = Sin.values();
        return wire >= 1 && wire <= all.length ? all[wire - 1] : null;
    }

    /** The bit mask of the sins named by these ids (bit n is the sin at enum position n). */
    public static int seriesMask(Collection<String> ids) {
        int mask = 0;
        for (String id : ids) {
            Sin s = Sin.byId(id);
            if (s != null) mask |= 1 << s.ordinal();
        }
        return mask;
    }

    /** True when the mask has the bit of this sin. */
    public static boolean hasBit(int mask, Sin sin) { return sin != null && (mask & (1 << sin.ordinal())) != 0; }

    /** A number clamped into [lo, hi] (for a value read off the wire). */
    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
