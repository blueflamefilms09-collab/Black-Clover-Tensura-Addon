package com.newuniverse.nusmp.relics.order;

import java.util.UUID;

/**
 * The decision table of the Order's ledger (pure: no Minecraft types, unit tested). Every relic that is not a consumable may exist once in
 * the world. A stack carries a serial; the ledger remembers, per relic id, the serial of the one valid copy. Given what the ledger knows
 * and what a stack carries, {@link #judge} says whether the stack is the canonical copy, a first copy that is to be adopted, or a duplicate;
 * {@link #action} turns that into what the Order does about it (during the census a duplicate is only reported).
 *
 * <pre>
 *   ledger entry     stack serial          verdict
 *   none             none or any           ADOPT       (grandfathered or creative first copy; keeps its serial if it has one)
 *   LIVE  S          S (first time seen)   CANONICAL
 *   LIVE  S          S (second time seen   DUPLICATE   (a copied stack carries the same serial: only the first one met in a pass is kept)
 *                      in the same pass)
 *   LIVE  S          none or another       DUPLICATE
 *   LOST  S          S                     REVIVE      (the copy thought destroyed was found: it is the valid one again)
 *   LOST  S          none or another       ADOPT       (nothing lives: this is the copy)
 * </pre>
 */
public final class LedgerRules {
    private LedgerRules() {}

    public enum Verdict { IGNORE, CANONICAL, ADOPT, REVIVE, DUPLICATE }

    public enum Action { NONE, TOUCH, ADOPT, REVIVE, WARN, ORDER }

    public enum Mode { CENSUS, ENFORCE }

    /** Length of the census of an existing world: ten minutes of game time. */
    public static final int CENSUS_TICKS = 12_000;
    /** Least game time between two visits of the Order for the same relic id: five minutes. */
    public static final long VISIT_COOLDOWN_TICKS = 6_000L;
    /** A canonical copy of a player that was online and was not seen for this much real time is declared lost: 14 days. */
    public static final long STALE_REAL_MILLIS = 14L * 24L * 3_600_000L;
    /** A copy issued by loot or a command that was never seen anywhere for this much game time is declared lost: one hour. */
    public static final long UNSEEN_GRACE_TICKS = 72_000L;
    /** A world older than this at the moment the ledger is made counts as an existing world and gets the census (two minutes). */
    public static final long EXISTING_WORLD_TICKS = 2_400L;
    /** The ledger rewrites the 'last seen' of a copy that stays where it is at most this often (game ticks). */
    public static final long TOUCH_EVERY_TICKS = 1_200L;

    /** What the ledger knows about one relic id. */
    public record Known(boolean exists, boolean live, UUID serial) {
        public static final Known NONE = new Known(false, false, null);

        public static Known live(UUID serial) { return new Known(true, true, serial); }

        public static Known lost(UUID serial) { return new Known(true, false, serial); }
    }

    /**
     * The verdict on one stack.
     * @param unique            false for consumables and for items that are no relic at all
     * @param known             what the ledger knows about this relic id
     * @param stackSerial       the serial on the stack, or null
     * @param serialSeenInPass  the same serial was already met (and kept) in the pass that is examining this stack
     */
    public static Verdict judge(boolean unique, Known known, UUID stackSerial, boolean serialSeenInPass) {
        if (!unique) return Verdict.IGNORE;
        if (known == null || !known.exists()) return Verdict.ADOPT;
        if (known.live()) {
            boolean matches = stackSerial != null && stackSerial.equals(known.serial());
            if (!matches) return Verdict.DUPLICATE;
            return serialSeenInPass ? Verdict.DUPLICATE : Verdict.CANONICAL;
        }
        if (stackSerial != null && stackSerial.equals(known.serial())) return serialSeenInPass ? Verdict.DUPLICATE : Verdict.REVIVE;
        return Verdict.ADOPT;
    }

    /** What the Order does about a verdict: during the census a duplicate is only reported. */
    public static Action action(Verdict v, Mode mode) {
        return switch (v) {
            case IGNORE -> Action.NONE;
            case CANONICAL -> Action.TOUCH;
            case ADOPT -> Action.ADOPT;
            case REVIVE -> Action.REVIVE;
            case DUPLICATE -> mode == Mode.CENSUS ? Action.WARN : Action.ORDER;
        };
    }

    /** The mode at a moment: an admin's choice wins; otherwise the census runs from its start for {@link #CENSUS_TICKS}. censusEnd &lt; 0: it has not started. */
    public static Mode modeAt(Mode forced, long censusEnd, long now) {
        if (forced != null) return forced;
        if (censusEnd < 0) return Mode.ENFORCE;
        return now < censusEnd ? Mode.CENSUS : Mode.ENFORCE;
    }

    /** Does a ledger made in a world of this age need a census? (A brand new world has no relic that could be twice.) */
    public static boolean needsCensus(long worldGameTime) { return worldGameTime > EXISTING_WORLD_TICKS; }

    /** Whole minutes (rounded up) from now until the census ends; 0 when it is over. */
    public static int censusMinutesLeft(long censusEnd, long now) {
        if (censusEnd < 0 || now >= censusEnd) return 0;
        return (int) ((censusEnd - now + 1199L) / 1200L);
    }

    /**
     * Is a live copy to be declared lost for not being seen? Only a copy that belongs to a player who is online and so should have been seen, after
     * {@link #STALE_REAL_MILLIS} of real time. A copy in a container, on the ground, on a mob or with an offline player is never touched.
     */
    public static boolean staleLost(boolean live, boolean heldByPlayer, boolean holderOnline, long realMillisSinceSeen) {
        return live && heldByPlayer && holderOnline && realMillisSinceSeen > STALE_REAL_MILLIS;
    }

    /** Was a copy that was issued but never seen anywhere given up on? */
    public static boolean unseenExpired(boolean live, boolean everSeen, long ticksSinceIssued) {
        return live && !everSeen && ticksSinceIssued > UNSEEN_GRACE_TICKS;
    }

    /** May the Order visit this relic id now? */
    public static boolean visitAllowed(long lastVisit, long now) { return lastVisit < 0 || now - lastVisit >= VISIT_COOLDOWN_TICKS || now < lastVisit; }

    /** Should a copy that stays put have its 'last seen' rewritten (so the saved data is not dirtied every scan)? */
    public static boolean rewriteSeen(boolean movedOrChanged, long lastWrite, long now) { return movedOrChanged || now - lastWrite >= TOUCH_EVERY_TICKS || now < lastWrite; }
}
