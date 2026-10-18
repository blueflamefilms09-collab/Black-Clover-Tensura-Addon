package com.newuniverse.nusmp.heroweapon.weapon.harpoon;

import java.util.UUID;

/**
 * Why: the Vassal Harpoon is a thrown weapon with a line back to its wielder, and the techniques after the throw (reel in, shock, pin, strip) act on whatever the
 * line is tied to, so the wielder needs no second aim. The design forbids anchoring the line to a block (no block is ever changed), so the line is tied to a
 * creature: one at a time, for a few seconds, which a melee blow with the barbed head can keep alive a little longer up to a hard ceiling. This is that
 * bookkeeping, as plain arithmetic on the game clock and the creature's id, with tests; the game side only asks it who is tied and tells it when to cut.
 */
public final class TetherBook {
    /** How long a fresh tie lasts, how much a melee blow adds, and the longest a tie may be kept alive from the moment of the last throw. */
    public static final int LIFE = 160, EXTEND = 40, CEILING = 240;

    private UUID target;
    private long until, bornAt;
    private long lastDraw = Long.MIN_VALUE / 4;

    /** True (and remembered) when at least {@code every} ticks have passed since the line was last drawn: the hook that draws it runs on its own beat. */
    public boolean drawDue(long now, int every) {
        if (now - lastDraw < every) return false;
        lastDraw = now;
        return true;
    }

    /** Ties the line to a creature (it replaces the old tie). */
    public void tie(UUID id, long now) {
        target = id;
        bornAt = now;
        until = now + LIFE;
    }

    /** The creature the line is tied to, or null when there is none or the tie ran out. */
    public UUID target(long now) {
        return target != null && now < until ? target : null;
    }

    /** A melee blow on the tied creature adds {@link #EXTEND} ticks, but never past {@link #CEILING} ticks after the throw. Returns true when the tie was extended. */
    public boolean extend(long now, UUID struck) {
        if (target == null || !target.equals(struck) || now >= until) return false;
        long cap = bornAt + CEILING;
        if (until >= cap) return false;
        until = Math.min(cap, until + EXTEND);
        return true;
    }

    /** Ticks left on the tie (0 when none). */
    public long left(long now) {
        return target != null && now < until ? until - now : 0L;
    }

    /** Cuts the line. */
    public void cut() {
        target = null;
        until = 0L;
    }
}
