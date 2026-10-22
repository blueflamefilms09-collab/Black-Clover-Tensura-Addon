package com.newuniverse.nusmp.relics;

/**
 * Why: the rules of "does the other player's client know what I wear" were spread through {@link WornSync} and {@link Relics} as inline conditions, so a missing resync (after a respawn, a
 * dimension change, a login) or a stale list on a client could not be tested. They live here, pure (no Minecraft type), and both classes call them.
 * <ul>
 *   <li><b>When the server looks.</b> A player is looked at every {@link #PERIOD} ticks (staggered by the entity id) and, besides that, at the next tick after any moment in which the
 *       client's idea of the player is certainly gone ({@link Moment}): login, respawn, change of dimension. The list is sent only when it differs from the one sent last.</li>
 *   <li><b>What the client does with a list.</b> It keeps it for the wearer until the wearer's entity leaves the client's level (leaving range, dimension change, respawn, death): a list
 *       is never kept for an entity that is not there, so a wearer that took its relics off while out of range cannot come back wearing them from an old list.</li>
 *   <li><b>How fresh the client's own look is.</b> The look at a wearer is cached ({@link Relics#TTL} ticks); for the local player, while a screen is open and for a short time after it
 *       closes (the inventory preview, the Curios screen), the cache lives {@link #FAST_TTL} ticks, so a relic put on is drawn at once and not half a second later.</li>
 * </ul>
 */
public final class WornSyncRules {
    private WornSyncRules() {}

    /** A player is looked at every this many ticks. */
    public static final int PERIOD = 10;

    /** The lifetime of the client's look at the local player while a screen is open (ticks). */
    public static final int FAST_TTL = 2;

    /** How many ticks the fast look goes on after the last screen closed (a relic put on with the screen closing right after is still caught). */
    public static final int FAST_HOLD = 20;

    /** The moments at which the server looks at a player. */
    public enum Moment { TICK, LOGIN, RESPAWN, DIMENSION }

    /** Is this a moment after which the client's list for the player is certainly gone (so the next tick must send it again without waiting for the period)? */
    public static boolean forcesResend(Moment m) { return m != Moment.TICK; }

    /** Does the server look at the player at this tick? {@code forced}: a login, respawn or dimension change is waiting. */
    public static boolean looksNow(int tickCount, int entityId, boolean forced) { return forced || (tickCount + entityId) % PERIOD == 0; }

    /** Is the new signature different from the one sent last (so a list goes out)? */
    public static boolean shouldSend(String before, String now) { return now != null && !now.equals(before == null ? "" : before); }

    /** The lifetime of a client's look: the fast one for the local player while the fast mode is on, else the normal one (never longer than the normal one). */
    public static int lookTtl(boolean fast, int normal) { return fast ? Math.min(normal, FAST_TTL) : normal; }

    /** The fast-mode countdown after one more client tick: a screen open keeps it full, otherwise it runs down to zero. */
    public static int nextFastHold(boolean screenOpen, int left) { return screenOpen ? FAST_HOLD : Math.max(0, left - 1); }

    /** Does the client keep a server list for a wearer whose entity just left its level? Never. */
    public static boolean keepListAfterLeave() { return false; }
}
